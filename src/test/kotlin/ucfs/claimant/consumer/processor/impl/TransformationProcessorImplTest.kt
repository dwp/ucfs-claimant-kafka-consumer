package ucfs.claimant.consumer.processor.impl

import arrow.core.left
import arrow.core.right
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nhaarman.mockitokotlin2.any
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
import ucfs.claimant.consumer.domain.DecryptionProcessingResult
import ucfs.claimant.consumer.domain.DecryptionResult
import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.processor.TransformationProcessor
import ucfs.claimant.consumer.transformer.Transformer
import ucfs.claimant.consumer.utility.GsonExtensions.json

class TransformationProcessorImplTest: StringSpec() {
    init {
        "Returns right on success" {
            val topics =
                listOf(Pair(claimantSourceTopic, claimantResult),
                    Pair(contractSourceTopic, contractResult),
                    Pair(statementSourceTopic, statementResult)).map(::row).toTypedArray()

            forAll(*topics) { (topic, result) ->
                validateRight(topic, result)
            }
        }

        "Returns left on failure" {
            val topics =
                listOf(Pair(claimantSourceTopic, claimantResult),
                    Pair(contractSourceTopic, contractResult),
                    Pair(statementSourceTopic, statementResult)).map(::row).toTypedArray()

            forAll(*topics) { (topic, _) ->
                validateLeft(topic)
            }
        }
    }

    private fun validateRight(sourceTopic: String, result: String) {
        val sourceRecord = sourceRecord(sourceTopic)
        succeedingProcessor().process(decryptionResult(sourceRecord)) shouldBeRight { (consumerRecord, transformed) ->
            consumerRecord shouldBeSameInstanceAs sourceRecord
            val (outputJson, transformedDbObject) = transformed
            outputJson.json() shouldMatchJson inputJson
            transformedDbObject shouldBe result
        }
    }

    private fun validateLeft(sourceTopic: String) {
        val sourceRecord = sourceRecord(sourceTopic)
        failingProcessor().process(decryptionResult(sourceRecord)) shouldBeLeft { consumerRecord ->
            consumerRecord shouldBeSameInstanceAs sourceRecord
        }
    }

    private fun decryptionResult(sourceRecord: SourceRecord): DecryptionProcessingResult =
        DecryptionProcessingResult(sourceRecord, decryptionResult)

    private fun sourceRecord(topic: String): SourceRecord =
        mock {
            on { topic() } doReturn topic
            on { key() } doReturn topic.toByteArray()
        }

    private fun succeedingProcessor(): TransformationProcessor =
            TransformationProcessorImpl(
                succeedingTransformer(claimantResult),
                succeedingTransformer(contractResult),
                succeedingTransformer(statementResult),
                claimantSourceTopic,
                contractSourceTopic,
                statementSourceTopic)

    private fun failingProcessor(): TransformationProcessor =
        TransformationProcessorImpl(
            failingTransformer(claimantResult),
            failingTransformer(contractResult),
            failingTransformer(statementResult),
            claimantSourceTopic,
            contractSourceTopic,
            statementSourceTopic)

    companion object {
        fun succeedingTransformer(result: String): Transformer =
            mock {
                on { transform(any()) } doReturn result.right()
            }

        fun failingTransformer(result: String): Transformer =
            mock {
                on { transform(any()) } doReturn result.left()
            }

        private const val claimantSourceTopic: String = "db.core.claimant"
        private const val contractSourceTopic: String = "db.core.contract"
        private const val statementSourceTopic: String = "db.core.statement"
        private const val claimantResult: String = "claimant"
        private const val contractResult: String = "contract"
        private const val statementResult: String = "statement"
        private const val inputJson: String = """{ "key": "value" }"""
        private val jsonObject = Gson().fromJson(inputJson, JsonObject::class.java)
        private val decryptionResult = DecryptionResult(jsonObject, inputJson)
    }
}
