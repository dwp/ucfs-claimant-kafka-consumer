package ucfs.claimant.consumer.orchestrate.impl

import arrow.core.left
import arrow.core.right
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nhaarman.mockitokotlin2.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import ucfs.claimant.consumer.domain.*
import ucfs.claimant.consumer.processor.CompoundProcessor
import ucfs.claimant.consumer.processor.PreProcessor
import ucfs.claimant.consumer.target.FailureTarget
import ucfs.claimant.consumer.target.SuccessTarget
import java.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds
import kotlin.time.toJavaDuration

@ExperimentalTime
class OrchestratorTargetTest : StringSpec() {

    init {
        "Sends updates, deletes to success target, writes failures to the dlq" {
            val successTarget = mock<SuccessTarget>()
            val failureTarget = mock<FailureTarget>()
            with (orchestrator(consumerProvider(), preProcessor(), compoundProcessor(), successTarget, failureTarget)) {
                shouldThrow<RuntimeException> { orchestrate() }
            }
            validateFailures(failureTarget)
            validateSuccesses(successTarget)
        }
    }

    private fun validateFailures(failureTarget: FailureTarget) {
        argumentCaptor<List<SourceRecord>> {
            verify(failureTarget, times(1)).send(capture())
            firstValue.size shouldBe (100 / 4) + (50 / 4)
            firstValue.forEach {
                it.topic() shouldBe TOPIC
                it.partition() shouldBe 0
                String(it.key()).toInt() % 4 shouldBe 3
            }
        }
    }

    private suspend fun validateSuccesses(successTarget: SuccessTarget) {
        verifyAdditionsAndModifications(successTarget)
        verifyDeletes(successTarget)
    }

    private suspend fun verifyAdditionsAndModifications(successTarget: SuccessTarget) {
        val topicCaptor = argumentCaptor<String>()
        argumentCaptor<List<TransformationProcessingResult>> {
            verify(successTarget, times(1)).upsert(topicCaptor.capture(), capture())
            topicCaptor.firstValue shouldBe TOPIC
            firstValue.size shouldBe 50 * 3 / 4 + 1
            firstValue.forEachIndexed { index, result ->
                with(result.first) {
                    String(key()).toInt() % 4 shouldNotBe 3
                    offset() % 4 shouldNotBe 3
                    topic() shouldBe TOPIC
                    partition() shouldBe 0
                }

                with(result.second.extract) {
                    id.toInt() % 4 shouldNotBe 3
                    action shouldBe if (index % 3 == 0) DatabaseAction.MONGO_INSERT else DatabaseAction.MONGO_UPDATE
                    timestampAndSource shouldBe Pair("2020-01-01", "_lastModifiedDateTime")
                }
            }
        }
    }

    private suspend fun verifyDeletes(successTarget: SuccessTarget) {
        val topicCaptor = argumentCaptor<String>()
        argumentCaptor<List<JsonProcessingResult>> {
            verify(successTarget, times(1)).delete(topicCaptor.capture(), capture())
            allValues.size shouldBe 1
            firstValue.size shouldBe 100 / 4
            firstValue.forEach { result ->
                with (result.first) {
                    String(key()).toInt() % 4 shouldBe 2
                }
                with (result.second) {
                    action shouldBe DatabaseAction.MONGO_DELETE
                }
            }
        }
    }


    private fun consumerProvider(): () -> KafkaConsumer<ByteArray, ByteArray> {
        val batch = consumerRecords(0, 99)
        val consumer = mock<KafkaConsumer<ByteArray, ByteArray>> {
            on { poll(any<Duration>()) } doReturn batch doThrow RuntimeException("End the loop")
            on { listTopics() } doReturn mapOf(TOPIC to listOf(mock()))
            on { subscription() } doReturn setOf(TOPIC)
        }
        return { consumer }
    }

    private fun compoundProcessor(): CompoundProcessor {
        val records = processingOutputs()
        return mock {
            on { process(any()) } doReturnConsecutively records
        }
    }

    private fun preProcessor(): PreProcessor {
        val results = (0..99).map { recordNumber ->
            when (recordNumber % 4) {
                0 -> {
                    JsonProcessingResult(consumerRecord(recordNumber), JsonProcessingExtract(
                                                                JsonObject(), "id",
                                                                DatabaseAction.MONGO_INSERT,
                                                                Pair("date", "datesource"))).right()
                }
                1 -> {
                    JsonProcessingResult(consumerRecord(recordNumber), JsonProcessingExtract(
                                                                JsonObject(), "id",
                                                                DatabaseAction.MONGO_UPDATE,
                                                                Pair("date", "datesource"))).right()
                }
                2 -> {
                    JsonProcessingResult(consumerRecord(recordNumber), JsonProcessingExtract(
                                                                JsonObject(), "id",
                                                                DatabaseAction.MONGO_DELETE,
                                                                Pair("date", "datesource"))).right()
                }
                else -> {
                    consumerRecord(recordNumber).left()
                }
            }

        }
        return mock {
            on {
                process(any())
            } doReturnConsecutively results
        }
    }

    private fun processingOutputs() =
        (0..99).map { recordNumber ->
            when (recordNumber % 4) {
                0 -> {
                    Pair(consumerRecord(recordNumber), mongoInsert(recordNumber)).right()
                }
                1, 2 -> {
                    Pair(consumerRecord(recordNumber), mongoUpdate(recordNumber)).right()
                }
                else -> {
                    consumerRecord(recordNumber).left()
                }
            }
        }


    private fun mongoUpdate(recordNumber: Int) = transformationResult(DatabaseAction.MONGO_UPDATE, recordNumber)
    private fun mongoInsert(recordNumber: Int) = transformationResult(DatabaseAction.MONGO_INSERT, recordNumber)

    private fun transformationResult(databaseAction: DatabaseAction, recordNumber: Int) =
        TransformationResult(JsonProcessingExtract(Gson().fromJson("""{ "body": "$recordNumber" }""", JsonObject::class.java), "$recordNumber",
            databaseAction, Pair("2020-01-01", "_lastModifiedDateTime")), "TRANSFORMED_DB_OBJECT")

    private fun orchestrator(provider: () -> KafkaConsumer<ByteArray, ByteArray>,
                            preProcessor: PreProcessor,
                            processor: CompoundProcessor,
                            successTarget: SuccessTarget,
                            failureTarget: FailureTarget): OrchestratorImpl =
            OrchestratorImpl(provider, Regex(TOPIC), preProcessor, processor,
                                10.seconds.toJavaDuration(), successTarget, failureTarget)

    private fun consumerRecords(first: Int, last: Int): ConsumerRecords<ByteArray, ByteArray> =
            ConsumerRecords((first..last).map(::consumerRecord).groupBy {
                TopicPartition(it.topic(), it.partition())
            })

    private fun consumerRecord(recordNumber: Int): ConsumerRecord<ByteArray, ByteArray> =
            mock {
                on { key() } doReturn "$recordNumber".toByteArray()
                on { topic() } doReturn TOPIC
                on { partition() } doReturn 0
                on { offset() } doReturn recordNumber.toLong()
            }

    companion object {
        private const val TOPIC = "db.database.collection"
    }
}
