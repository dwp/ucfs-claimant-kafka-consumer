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
import ucfs.claimant.consumer.processor.impl.SourceData.claimantIdSourceField
import ucfs.claimant.consumer.processor.impl.SourceData.claimantTopic
import ucfs.claimant.consumer.processor.impl.SourceData.idSourceFields

class JsonProcessorImplTest : StringSpec() {
    init {

        // TODO: 09/01/2021 missing id

        "Returns right if well-formed json and known database action" {
            forAll (*databaseActions) { databaseAction ->
                val json = """
                {
                    "message": {
                        "_lastModifiedDateTime": "2020-12-12",
                        "dbObject": "ENCRYPTED_OBJECT",
                        "@type": "$databaseAction",
                        "_id": {
                            "$claimantIdSourceField": "123"
                        },
                        "timestamp": "2020-01-01"
                    }
                }""".trimIndent()

                val sourceRecord = sourceRecord()

                val result = JsonProcessorImpl(idSourceFields).process(Pair(sourceRecord, json))
                result shouldBeRight { (record, result) ->
                    val (jsonObject, id, action, timestampAndSource) = result
                    Gson().toJson(jsonObject) shouldMatchJson json
                    record shouldBeSameInstanceAs sourceRecord
                    id shouldBe "123"
                    action shouldBe databaseAction
                    if (action == DatabaseAction.MONGO_DELETE) {
                        timestampAndSource shouldBe Pair("1980-01-01T00:00:00.000+0000", "epoch")
                    }
                    else {
                        timestampAndSource shouldBe Pair("2020-12-12", "_lastModifiedDateTime")
                    }
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

            val sourceRecord = sourceRecord()
            val result = JsonProcessorImpl(idSourceFields).process(Pair(sourceRecord, json))
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
            val result = JsonProcessorImpl(idSourceFields).process(Pair(sourceRecord, json))
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
            val result = JsonProcessorImpl(idSourceFields).process(Pair(record, json))
            result shouldBeLeft {
                it shouldBeSameInstanceAs record
            }
        }
    }

    private fun sourceRecord(): SourceRecord =
            mock {
                on { topic() } doReturn claimantTopic
                on { key() } doReturn "key".toByteArray()
            }

    companion object {
        private val databaseActions = DatabaseAction.values().map(::row).toTypedArray()
    }
}
