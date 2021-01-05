package ucfs.claimant.consumer.target.impl

import com.google.gson.JsonObject
import com.nhaarman.mockitokotlin2.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.domain.TransformationProcessingResult
import ucfs.claimant.consumer.domain.TransformationResult
import ucfs.claimant.consumer.transformer.impl.GsonTestUtility.jsonObject
import java.sql.Connection
import java.sql.PreparedStatement
import javax.sql.DataSource

class RdsTargetTest: StringSpec() {
    init {
        "Excludes deletes" {
            val statement = mock<PreparedStatement> {
                on { setString(any(), any()) } doAnswer {}
            }
            val sqlCaptor = argumentCaptor<String>()
            val conn = mock<Connection> {
                on {prepareStatement(sqlCaptor.capture())} doReturn statement
            }

            val dataSource = mock<DataSource> {
                on {connection} doReturn conn
            }

            val target = RdsTarget(dataSource, targetTables)
            val sourceRecord = mock<SourceRecord>()

            val results = (0..99).map {
                TransformationProcessingResult(sourceRecord, transformationResult(it))
            }

            target.send(claimantTopic, results)
            verifyStatementInteractions(statement)
            verifyConnectionInteractions(conn)
            verifyDataSourceInteractions(dataSource)
        }

    }

    private fun verifyStatementInteractions(statement: PreparedStatement) {
        val positionCaptor = argumentCaptor<Int>()
        val jsonCaptor = argumentCaptor<String>()
        verify(statement, times(34 * 2 + 33 * 2)).setString(positionCaptor.capture(), jsonCaptor.capture())
        positionCaptor.allValues.forEachIndexed { index, value ->
            value shouldBe index % 2 + 1
        }
        jsonCaptor.allValues.asSequence().filterIndexed { index, _ -> index % 2 == 0 }.map(::jsonObject)
            .map { it.getAsJsonObject("_id") }
            .map { it.getAsJsonPrimitive("id") }
            .map { it.asInt }.toList()
            .forEachIndexed { index, x ->
                x % 3 shouldNotBe 2
                x % 3 shouldBe index % 2
            }

        verify(statement, times(34 + 33)).addBatch()
        verify(statement, times(1)).executeBatch()
        verify(statement, times(1)).close()
        verifyNoMoreInteractions(statement)
    }

    private fun verifyConnectionInteractions(conn: Connection) {
        verify(conn, times(1)).prepareStatement("""INSERT INTO claimant (data) VALUES (?) ON DUPLICATE KEY UPDATE data = ?""")
        verify(conn, times(1)).close()
        verifyNoMoreInteractions(conn)
    }

    private fun verifyDataSourceInteractions(dataSource: DataSource) {
        verify(dataSource, times(1)).connection
        verifyNoMoreInteractions(dataSource)
    }

    private fun transformationResult(index: Int): TransformationResult =
            TransformationResult(JsonObject(), """{"_id": { "id": $index }}""",
                when (index % 3) {
                    0 -> "MONGO_INSERT"
                    1 -> "MONGO_UPDATE"
                    else -> "MONGO_DELETE"
                })

    companion object {
        private const val claimantTopic = "db.core.claimant"
        private const val contractTopic = "db.core.contract"
        private const val statementTopic = "db.core.statement"

        private const val claimantTable = "claimant"
        private const val contractTable = "contract"
        private const val statementTable = "statement"

        val targetTables: Map<String, String> =
            mapOf(claimantTopic to claimantTable, contractTopic to contractTable, statementTopic to statementTable)
    }
}
