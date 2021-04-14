package ucfs.claimant.consumer.target.impl

import arrow.core.extensions.list.foldable.isNotEmpty
import arrow.core.identity
import io.prometheus.client.Counter
import io.prometheus.client.spring.web.PrometheusTimeMethod
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.*
import ucfs.claimant.consumer.target.SuccessTarget
import ucfs.claimant.consumer.utility.GsonExtensions.jsonObject
import ucfs.claimant.consumer.utility.GsonExtensions.nullableInteger
import ucfs.claimant.consumer.utility.GsonExtensions.nullableList
import ucfs.claimant.consumer.utility.GsonExtensions.nullableString
import uk.gov.dwp.dataworks.logging.DataworksLogger
import javax.sql.DataSource

@Component
class RdsTarget(private val dataSource: DataSource,
                private val insertedRecords: Counter,
                private val updatedRecords: Counter,
                private val deletedRecords: Counter,
                @Qualifier("targetTables") private val targetTables: Map<String, String>,
                @Qualifier("naturalIdFields") private val naturalIds: Map<String, String>,
                private val claimantTopic: String,
                private val contractTopic: String,
                private val statementTopic: String): SuccessTarget {

    @PrometheusTimeMethod(name = "uckc_upsert", help = "Duration and count of database upserts")
    override suspend fun upsert(topic: String, records: List<FilterProcessingResult>) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(upsertSql(topic)).use { statement ->

                records.map(FilterProcessingResult::second)
                    .filter(FilterResult::passThrough)
                    .map(FilterResult::transformationResult)
                    .map(TransformationResult::transformedDbObject)
                    .forEach { transformed ->
                        statement.setString(1, transformed)
                        statement.setString(2, transformed)
                        statement.addBatch()
                    }

                val results = statement.executeBatch()

                val (inserts, updates) = results.asList().partition { it == 1 }
                insertedRecords.labels(topic).inc(inserts.size.toDouble())
                updatedRecords.labels(topic).inc(updates.size.toDouble())

                records.map(FilterProcessingResult::second)
                    .map(FilterResult::transformationResult)
                    .zip(results.asList()).forEach { (transformationResult: TransformationResult, count) ->
                        val (extract, transformed: String) = transformationResult
                        log.info("${if (count == 1) "Inserted" else "Updated"} record",
                            "topic" to topic,
                            "table" to "${targetTables[topic]}",
                            "id" to extract.id,
                            "action" to "${extract.action}",
                            "timestamp" to extract.timestampAndSource.first,
                            "timestampSource" to extract.timestampAndSource.second,
                            "rows_updated" to "$count",
                            *topicSpecificTuples(topic, transformed))
                    }
            }
        }
    }


    private fun topicSpecificTuples(topic: String, transformedOutput: String): Array<Pair<String, String>> {
        return transformedOutput.jsonObject().map { transformedObject ->
            try {
                when (topic) {
                    claimantTopic -> {
                        arrayOf("nino" to "${transformedObject.nullableString("nino")}")
                    }
                    contractTopic -> {
                        val people = transformedObject.nullableList<String>("people")
                        val person1 = people?.firstOrNull()
                        val person2 = people?.takeIf(List<String>::isNotEmpty)?.takeIf { it.size > 1 }?.get(1)
                        val startDate = transformedObject.nullableInteger("startDate")
                        val closedDate = transformedObject.nullableInteger("closedDate")
                        arrayOf("start_date" to "$startDate", "closed_date" to "$closedDate",
                            "person_one" to "$person1", "person_two" to "$person2")
                    }
                    statementTopic -> {
                        val startDate = transformedObject.nullableInteger("assessmentPeriod", "startDate")
                        val endDate = transformedObject.nullableInteger("assessmentPeriod", "endDate")
                        val people = transformedObject.nullableList<Map<String, String>>("people")
                        val person1 = people?.firstOrNull()
                        val person1CitizenId = person1?.get("citizenId")
                        val person1ContractId = person1?.get("contractId")

                        val person2 = people?.takeIf(List<Map<String, String>>::isNotEmpty)?.takeIf { it.size > 1 }?.get(1)
                        val person2CitizenId = person2?.get("citizenId")
                        val person2ContractId = person2?.get("contractId")
                        arrayOf("start_date" to "$startDate", "end_date" to "$endDate",
                            "person_one_citizen_id" to "$person1CitizenId", "person_one_contract_id" to "$person1ContractId",
                            "person_two_citizen_id" to "$person2CitizenId", "person_two_contract_id" to "$person2ContractId")
                    }
                    else -> arrayOf()
                }
            } catch (e: Exception) {
                log.error("Failed to extract topic specific tuples", e, "error_message" to "${e.message}")
                arrayOf()
            }
        }.fold(ifRight = ::identity, ifLeft = { arrayOf() })
    }


    @PrometheusTimeMethod(name = "uckc_delete", help = "Duration and count of database deletes")
    override suspend fun delete(topic: String, records: List<JsonProcessingResult>) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(deleteSql(topic)).use { statement ->
                records.map(JsonProcessingResult::second).map(JsonProcessingExtract::id).let { ids ->
                    ids.forEach { naturalId ->
                        statement.setString(1, naturalId)
                        statement.addBatch()
                    }
                    deletedRecords.labels(topic).inc(ids.size.toDouble())
                    val results = statement.executeBatch()
                    logDeletes(records, results, topic)
                }
            }
        }
    }

    private fun logDeletes(ids: List<JsonProcessingResult>, results: IntArray, topic: String) {
        val (found, notFound) = ids.zip(results.asList()).partition { (_, rowCount) -> rowCount > 0 }

        found.forEach { (result, count) ->
            val (_, extract) = result

            log.info("Deleted record", "topic" to topic,
                "table" to "${targetTables[topic]}",
                "id" to extract.id,
                "action" to "${extract.action}",
                "timestamp" to extract.timestampAndSource.first,
                "timestampSource" to extract.timestampAndSource.second,
                "rows_updated" to "$count")
        }

        notFound.forEach { (result, count) ->
            val (_, extract) = result
            log.warn("Failed to delete record, no rows updated", "topic" to topic,
                "table" to "${targetTables[topic]}",
                "id" to extract.id,
                "action" to "${extract.action}",
                "timestamp" to extract.timestampAndSource.first,
                "timestampSource" to extract.timestampAndSource.second,
                "rows_updated" to "$count")
        }
    }

    private fun upsertSql(topic: String): String =
        """INSERT INTO ${targetTables[topic]} (data) VALUES (?) ON DUPLICATE KEY UPDATE data = ?"""

    private fun deleteSql(topic: String): String =
        """DELETE FROM ${targetTables[topic]} WHERE ${naturalIds[topic]} = ?"""

    companion object {
        private val log = DataworksLogger.getLogger(RdsTarget::class)
    }
}
