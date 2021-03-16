package ucfs.claimant.consumer.target.impl

import com.google.gson.JsonPrimitive
import com.nhaarman.mockitokotlin2.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.doubles.ToleranceMatcher
import io.kotest.matchers.shouldBe
import io.prometheus.client.Counter
import ucfs.claimant.consumer.domain.*
import ucfs.claimant.consumer.processor.impl.SourceData.jsonProcessingExtract
import ucfs.claimant.consumer.transformer.impl.GsonTestUtility.jsonObject
import java.sql.Connection
import java.sql.PreparedStatement
import javax.sql.DataSource

class RdsTargetTest: StringSpec() {
    init {
        "Updates" {
            forAll (*topics) { validateUpdates(it) }
        }

        "Deletes" {
            forAll (*topics) { validateDeletes(it) }
        }
    }

    private suspend fun validateUpdates(topic: String) {
        val statement = preparedStatement()
        val conn = connection(statement)
        val dataSource = dataSource(conn)
        val sourceRecord = mock<SourceRecord>()
        val results = List(100) {
            FilterProcessingResult(sourceRecord, filterResult(it))
        }

        val insertChild = mock<Counter.Child>()
        val updateChild = mock<Counter.Child>()
        val deleteChild = mock<Counter.Child>()
        val inserts = counter(insertChild)
        val updates = counter(updateChild)
        val deletes = counter(deleteChild)

        rdsTarget(dataSource, inserts, updates, deletes).upsert(topic, results)

        verify(inserts, times(1)).labels(topic)
        verifyNoMoreInteractions(inserts)
        verify(updates, times(1)).labels(topic)
        verifyNoMoreInteractions(updates)

        argumentCaptor<Double> {
            verify(insertChild, times(1)).inc(capture())
            firstValue shouldBe ToleranceMatcher(34.toDouble(), 0.5)
        }

        argumentCaptor<Double> {
            verify(updateChild, times(1)).inc(capture())
            firstValue shouldBe ToleranceMatcher(66.toDouble(), 0.5)
        }

        verifyNoMoreInteractions(updates)
        verifyZeroInteractions(deleteChild)

        verifyUpdateStatementInteractions(statement)
        verifyStatementInteractions(statement, 50)
        verifyConnectionInteractions(conn,
            """INSERT INTO ${targetTables[topic]} (data) VALUES (?) ON DUPLICATE KEY UPDATE data = ?""")
        verifyDataSourceInteractions(dataSource)
    }

    private suspend fun validateDeletes(topic: String) {
        val statement = preparedStatement()
        val conn = connection(statement)
        val dataSource = dataSource(conn)
        val sourceRecord = mock<SourceRecord>()

        val results = List(100) {
            JsonProcessingResult(sourceRecord, jsonProcessingExtract(id = "$it"))
        }

        val deleteChild = mock<Counter.Child>()
        val deletes = counter(deleteChild)
        rdsTarget(dataSource, mock(), mock(), deletes).delete(topic, results)
        verifyDeleteStatementInteractions(statement)
        verifyStatementInteractions(statement, 100)
        verifyConnectionInteractions(conn, """DELETE FROM ${targetTables[topic]} WHERE ${naturalIds[topic]} = ?""")
        verifyDataSourceInteractions(dataSource)

        argumentCaptor<Double> {
            verify(deleteChild, times(1)).inc(capture())
            firstValue shouldBe ToleranceMatcher(100.0, 0.5)
        }
    }

    private fun verifyStatementInteractions(statement: PreparedStatement, addBatchCount: Int) {
        verify(statement, times(addBatchCount)).addBatch()
        verify(statement, times(1)).executeBatch()
        verify(statement, times(1)).close()
        verifyNoMoreInteractions(statement)
    }

    private fun verifyUpdateStatementInteractions(statement: PreparedStatement) {
        val positionCaptor = argumentCaptor<Int>()
        val jsonCaptor = argumentCaptor<String>()
        verify(statement, times(100)).setString(positionCaptor.capture(), jsonCaptor.capture())

        positionCaptor.allValues.forEachIndexed { index, value ->
            value shouldBe index % 2 + 1
        }

        jsonCaptor.allValues.asSequence()
            .filterIndexed { index, _ -> index % 2 == 0 }
            .map(::jsonObject)
            .map { it.getAsJsonObject("_id") }
            .map { it.getAsJsonPrimitive("id") }
            .map(JsonPrimitive::getAsInt)
            .toList()
            .forEachIndexed { index, x ->
                x shouldBe index * 2
            }
    }


    private fun verifyDeleteStatementInteractions(statement: PreparedStatement) {
        val parameterIndexCaptor = argumentCaptor<Int>()
        val valueCaptor = argumentCaptor<String>()
        verify(statement, times(100)).setString(parameterIndexCaptor.capture(), valueCaptor.capture())
        parameterIndexCaptor.allValues.forEach { it shouldBe 1 }
        valueCaptor.allValues.forEachIndexed { index, value ->
            value shouldBe "$index"
        }
    }

    private fun verifyConnectionInteractions(conn: Connection, sql: String) {
        verify(conn, times(1)).prepareStatement(sql)
        verify(conn, times(1)).close()
        verifyNoMoreInteractions(conn)
    }

    private fun verifyDataSourceInteractions(dataSource: DataSource) {
        verify(dataSource, times(1)).connection
        verifyNoMoreInteractions(dataSource)
    }

    private fun filterResult(index: Int): FilterResult =
            FilterResult(TransformationResult(jsonProcessingExtract(), """{"_id": { "id": $index }}"""), index % 2 == 0)

    private fun rdsTarget(dataSource: DataSource, insertedCounter: Counter, updatedCounter: Counter, deletedCounter: Counter): RdsTarget =
        RdsTarget(dataSource, insertedCounter, updatedCounter, deletedCounter, targetTables, naturalIds,
            "db.core.claimant", "db.core.contract", "db.core.statement")

    private fun dataSource(conn: Connection): DataSource =
            mock {
                on { connection } doReturn conn
            }

    private fun counter(child: Counter.Child): Counter =
            mock {
                on { labels(any()) } doReturn child
            }

    private fun connection(statement: PreparedStatement): Connection =
            mock {
                on { prepareStatement(any()) } doReturn statement
            }

    private fun preparedStatement(): PreparedStatement =
            mock {
                on { setString(any(), any()) } doAnswer {}
                on { executeBatch() } doReturn IntArray(100) { if (it % 3 == 0) 1 else 0 }
            }

    companion object {

        private const val claimantTopic = "db.core.claimant"
        private const val contractTopic = "db.core.contract"
        private const val statementTopic = "db.core.statement"

        private val topics = listOf(claimantTopic, contractTopic, statementTopic).map(::row).toTypedArray()

        private const val claimantTable = "claimant"
        private const val contractTable = "contract"
        private const val statementTable = "statement"

        private const val claimantNaturalId = "citizenId"
        private const val contractNaturalId = "contractId"
        private const val statementNaturalId = "statementId"

        val targetTables: Map<String, String> =
            mapOf(claimantTopic to claimantTable, contractTopic to contractTable, statementTopic to statementTable)

        val naturalIds: Map<String, String> =
            mapOf(claimantTopic to claimantNaturalId, contractTopic to contractNaturalId, statementTopic to statementNaturalId)
    }
}
