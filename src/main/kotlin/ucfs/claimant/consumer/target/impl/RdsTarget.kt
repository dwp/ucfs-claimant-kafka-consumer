package ucfs.claimant.consumer.target.impl

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.TransformationProcessingResult
import ucfs.claimant.consumer.target.SuccessTarget
import javax.sql.DataSource

@Component
class RdsTarget(private val dataSource: DataSource,
                @Qualifier("targetTables") private val targetTables: Map<String, String>): SuccessTarget {

    override suspend fun send(topic: String, records: List<TransformationProcessingResult>) {
        dataSource.connection.use { connection ->
            val (addsMods, _) = records.partition { it.second.action != DELETE_ACTION }
            connection.prepareStatement(upsertSql(topic)).use { statement ->
                addsMods.forEach { (_, transformationResult) ->
                    statement.setString(1, transformationResult.transformedDbObject)
                    statement.setString(2, transformationResult.transformedDbObject)
                    statement.addBatch()
                }
                statement.executeBatch()
            }
        }
    }

    private fun upsertSql(topic: String): String =
        """INSERT INTO ${targetTables[topic]} (data) VALUES (?) ON DUPLICATE KEY UPDATE data = ?"""

    companion object {
        private const val DELETE_ACTION = "MONGO_DELETE"
    }
}
