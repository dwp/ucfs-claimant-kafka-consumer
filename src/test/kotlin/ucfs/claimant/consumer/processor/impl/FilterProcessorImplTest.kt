package ucfs.claimant.consumer.processor.impl

import com.google.gson.JsonObject
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import ucfs.claimant.consumer.domain.*

class FilterProcessorImplTest: StringSpec() {

    init {
        "Return right true if not claimant and well formed json" {
            forAll(*nonClaimant) { topic ->
                allowedThrough("{}", topic) shouldBeRight {
                    it.second.passThrough.shouldBeTrue()
                }
            }
        }

        "Return right true if not claimant and malformed json" {
            forAll(*nonClaimant) { topic ->
                allowedThrough("{", topic) shouldBeRight {
                    it.second.passThrough.shouldBeTrue()
                }
            }
        }

        "Return right true if claimant and nino not blank" {
            allowedThrough("""{ "nino": "123" }""") shouldBeRight {
                it.second.passThrough.shouldBeTrue()
            }
        }

        "Return right false if claimant and nino empty" {
            allowedThrough("""{ "nino": "" }""") shouldBeRight {
                it.second.passThrough.shouldBeFalse()
            }
        }

        "Return left if claimant and malformed" {
            allowedThrough("""{ "nino": }""").shouldBeLeft()
        }

        "Return right false if claimant and nino blank" {
            allowedThrough("""{ "nino": "   " }""") shouldBeRight {
                it.second.passThrough.shouldBeFalse()
            }
        }

        "Return right false if claimant and nino absent" {
            allowedThrough("{}") shouldBeRight {
                it.second.passThrough.shouldBeFalse()
            }
        }
    }

    companion object {
        private fun allowedThrough(json: String, topic: String = claimantTopic) =
            FilterProcessorImpl(claimantTopic).process(processingResult(json, topic))

        private fun processingResult(json: String, topic: String): TransformationProcessingResult {
            val record = mock<SourceRecord> {
                on { topic() } doReturn topic
                on { key() } doReturn ByteArray(0)
            }
            return TransformationProcessingResult(record, transformationResult(json))
        }

        private fun transformationResult(json: String): TransformationResult = TransformationResult(extract, json)
        private val extract = JsonProcessingExtract(JsonObject(), "", DatabaseAction.MONGO_UPDATE, Pair("", ""))
        private const val claimantTopic = "db.core.claimant"
        private const val contractTopic = "db.core.contract"
        private const val statementTopic = "db.core.statement"
        private val nonClaimant = listOf(contractTopic, statementTopic).map(::row).toTypedArray()
    }
}
