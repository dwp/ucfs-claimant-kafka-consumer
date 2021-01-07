package ucfs.claimant.consumer.processor.impl

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.assertions.json.shouldMatchJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import ucfs.claimant.consumer.domain.DatabaseAction
import ucfs.claimant.consumer.domain.SourceRecord

class JsonProcessorImplTest : StringSpec() {
    init {
        "Returns right if well-formed json and known database action" {
            forAll (*databaseActions) { databaseAction ->
                val json = """
                {
                    "message": {
                        "dbObject": "ENCRYPTED_OBJECT",
                        "@type": "$databaseAction"
                    }
                }""".trimIndent()

                val queueRecord = mock<SourceRecord>()
                val result = JsonProcessorImpl().process(Pair(queueRecord, json))
                result shouldBeRight { (record, result) ->
                    val (jsonObject, action) = result
                    Gson().toJson(jsonObject) shouldMatchJson json
                    record shouldBeSameInstanceAs queueRecord
                    action shouldBe databaseAction
                }
            }
        }

        "Returns left if unknown database action" {
            val json = """
            {
                "message": {
                    "dbObject": "ENCRYPTED_OBJECT",
                    "@type": "MONGO_HOMOLOGATE"
                }
            }""".trimIndent()

            val sourceRecord = mock<SourceRecord> {
                on { key() } doReturn "key".toByteArray()
            }
            val result = JsonProcessorImpl().process(Pair(sourceRecord, json))
            result shouldBeLeft {
                it shouldBeSameInstanceAs sourceRecord
            }
        }

        "Returns left if no database action" {
            val json = """
            {
                "message": {
                    "dbObject": "ENCRYPTED_OBJECT",
                }
            }""".trimIndent()

            val sourceRecord = mock<SourceRecord> {
                on { key() } doReturn "key".toByteArray()
            }
            val result = JsonProcessorImpl().process(Pair(sourceRecord, json))
            result shouldBeLeft {
                it shouldBeSameInstanceAs sourceRecord
            }
        }

        "Returns left if malformed json" {
            val json = """
                    "message": {
                        "dbObject": "ENCRYPTED_OBJECT"
                    }
            """.trimIndent()

            val record = mock<SourceRecord> {
                on { key() } doReturn "key".toByteArray()
            }
            val result = JsonProcessorImpl().process(Pair(record, json))
            result shouldBeLeft {
                it shouldBeSameInstanceAs record
            }
        }
    }

    companion object {
        private val databaseActions = DatabaseAction.values().map(::row).toTypedArray()
    }
}
