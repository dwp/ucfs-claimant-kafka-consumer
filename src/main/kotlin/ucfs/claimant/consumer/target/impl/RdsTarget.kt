package ucfs.claimant.consumer.target.impl

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.TransformationProcessingResult
import ucfs.claimant.consumer.target.SuccessTarget
import javax.sql.DataSource

@Component
@Profile("!QUEUE_TARGET")
class RdsTarget(private val dataSource: DataSource,
                @Qualifier("targetTables") private val targetTables: Map<String, String>,
                @Qualifier("sourceIds") private val sourceIds: Map<String, String>): SuccessTarget {

    override suspend fun send(topic: String, records: List<TransformationProcessingResult>) {
        dataSource.connection.use { connection ->
            val sql = upsertSql(topic)
            records.forEach { (_, wtf) ->
                val (_, result) = wtf
                println("===============================================================")
                println(connection)
                println(result)
                println(sql)
                println(targetTables)
                println(sourceIds)
                println("===============================================================")
            }
        }
    }


    fun upsertSql(topic: String): String =
        """INSERT INTO ${targetTables[topic]} (data) VALUES (?) ON DUPLICATE KEY UPDATE data = ? WHERE ${sourceIds[topic]} = ?"""
}
