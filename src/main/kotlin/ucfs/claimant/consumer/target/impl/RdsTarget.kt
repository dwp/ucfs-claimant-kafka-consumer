package ucfs.claimant.consumer.target.impl

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.DeleteProcessingResult
import ucfs.claimant.consumer.domain.TransformationProcessingResult
import ucfs.claimant.consumer.target.SuccessTarget
import uk.gov.dwp.dataworks.logging.DataworksLogger
import javax.sql.DataSource

@Component
class RdsTarget(private val dataSource: DataSource,
                @Qualifier("targetTables") private val targetTables: Map<String, String>,
                @Qualifier("naturalIdFields") private val naturalIds: Map<String, String>): SuccessTarget {

    override suspend fun upsert(topic: String, records: List<TransformationProcessingResult>) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(upsertSql(topic)).use { statement ->
                records.map(TransformationProcessingResult::second).forEach { transformationResult ->
                    statement.setString(1, transformationResult.transformedDbObject)
                    statement.setString(2, transformationResult.transformedDbObject)
                    statement.addBatch()
                }
                val results = statement.executeBatch()

            }
        }
    }

    override suspend fun delete(topic: String, records: List<DeleteProcessingResult>) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(deleteSql(topic)).use { statement ->
                records.map(DeleteProcessingResult::second).let { ids ->
                    ids.forEach { naturalId ->
                        statement.setString(1, naturalId)
                        statement.addBatch()
                    }
                    val results = statement.executeBatch()
                    logDeletes(ids, results, topic)
                }
            }
        }
    }

    private fun logDeletes(ids: List<String>, results: IntArray, topic: String) {
        ids.zip(results.asList()).forEach {
            log.info("Deleted record", "table" to "${targetTables[topic]}",
                "id" to it.first, "rows_updated" to "${it.second}")
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
