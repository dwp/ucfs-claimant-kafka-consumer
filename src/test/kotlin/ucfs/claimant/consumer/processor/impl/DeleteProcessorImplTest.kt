package ucfs.claimant.consumer.processor.impl

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import ucfs.claimant.consumer.domain.DatabaseAction
import ucfs.claimant.consumer.domain.JsonProcessingExtract
import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.processor.impl.SourceData.claimantTopic
import ucfs.claimant.consumer.processor.impl.SourceData.contractTopic
import ucfs.claimant.consumer.processor.impl.SourceData.idSourceFields
import ucfs.claimant.consumer.processor.impl.SourceData.statementTopic

class DeleteProcessorImplTest: StringSpec() {
    init {
        "Returns right if id found" {
            forAll(*(topics())) { topic ->
                val sourceRecord = sourceRecord(topic)
                deleteProcessor().process(jsonProcessingResult(sourceRecord, validJsonObject(topic))).shouldBeRight { (sourceRecord, id) ->
                    sourceRecord shouldBeSameInstanceAs sourceRecord
                    id shouldBe "ID"
                }
            }
        }

        "Returns left if id not found" {
            forAll(*topics()) { topic ->
                val sourceRecord = sourceRecord(topic)
                deleteProcessor().process(jsonProcessingResult(sourceRecord, invalidJsonObject())).shouldBeLeft {
                    it shouldBeSameInstanceAs sourceRecord
                }
            }
        }
    }

    private fun topics() = listOf(claimantTopic, contractTopic, statementTopic).map(::row).toTypedArray()

    private fun jsonProcessingResult(sourceRecord: SourceRecord,input: JsonObject) =
        Pair(sourceRecord, JsonProcessingExtract(input, "id",
                                                DatabaseAction.MONGO_DELETE, Pair("date", "dateSource")))

    private fun deleteProcessor(): DeleteProcessorImpl = DeleteProcessorImpl(idSourceFields)

    private fun validJsonObject(topic: String) = jsonObject(idSourceFields[topic]!!)
    private fun invalidJsonObject() = jsonObject("unknownIdField")

    private fun jsonObject(idField: String) =
        Gson().fromJson("""{
                    "message": {
                        "_id": {
                            "$idField": "ID"
                        }
                    }
                }""".trimIndent(), JsonObject::class.java)


    private fun sourceRecord(topic: String): SourceRecord =
        mock {
            on { key() } doReturn "1".toByteArray()
            on { topic() } doReturn topic
        }

}
