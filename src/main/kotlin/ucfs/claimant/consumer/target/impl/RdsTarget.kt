package ucfs.claimant.consumer.target.impl

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.*
import ucfs.claimant.consumer.target.SuccessTarget
import uk.gov.dwp.dataworks.logging.DataworksLogger
import javax.sql.DataSource

@Component
class RdsTarget(private val dataSource: DataSource,
                @Qualifier("targetTables") private val targetTables: Map<String, String>,
                @Qualifier("naturalIdFields") private val naturalIds: Map<String, String>): SuccessTarget {

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
                records.map(FilterProcessingResult::second)
                    .map(FilterResult::transformationResult)
                    .map(TransformationResult::extract)
                    .zip(results.asList()).forEach { (extract, count) ->
                    log.info("${if (count == 1) "Inserted" else "Updated"} record","topic" to topic,
                        "table" to "${targetTables[topic]}",
                        "id" to extract.id,
                        "action" to "${extract.action}",
                        "timestamp" to extract.timestampAndSource.first,
                        "timestampSource" to extract.timestampAndSource.second,
                        "rows_updated" to "$count")
                }
            }
        }
    }

    override suspend fun delete(topic: String, records: List<JsonProcessingResult>) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(deleteSql(topic)).use { statement ->
                records.map(JsonProcessingResult::second).map(JsonProcessingExtract::id).let { ids ->
                    ids.forEach { naturalId ->
                        statement.setString(1, naturalId)
                        statement.addBatch()
                    }
                    val results = statement.executeBatch()
                    logDeletes(records, results, topic)
                }
            }
        }
    }

    private fun logDeletes(ids: List<JsonProcessingResult>, results: IntArray, topic: String) {
        val (found, notFound) = ids.zip(results.asList()).partition{ (_, rowCount) ->
            rowCount > 0
        }


        found.forEach { (result, count) ->
            val (_, extract) = result

            log.info("Deleted record","topic" to topic,
                "table" to "${targetTables[topic]}",
                "id" to extract.id,
                "action" to "${extract.action}",
                "timestamp" to extract.timestampAndSource.first,
                "timestampSource" to extract.timestampAndSource.second,
                "rows_updated" to "$count")
        }

        notFound.forEach { (result, count) ->
            val (_, extract) = result
            log.warn("Failed to delete record, no rows updated","topic" to topic,
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
