package ucfs.claimant.consumer.orchestrate.impl

import arrow.core.left
import arrow.core.right
import com.google.gson.JsonObject
import com.nhaarman.mockitokotlin2.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import ucfs.claimant.consumer.domain.*
import ucfs.claimant.consumer.processor.CompoundProcessor
import ucfs.claimant.consumer.processor.DeleteProcessor
import ucfs.claimant.consumer.processor.PreProcessor
import ucfs.claimant.consumer.target.FailureTarget
import ucfs.claimant.consumer.target.SuccessTarget
import java.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds
import kotlin.time.toJavaDuration

@ExperimentalTime
class OrchestratorImplTest : StringSpec() {

    init {
        "Commit on success, rollback on failure" {
            val batch1 = consumerRecords(0, 99)
            val batch2 = consumerRecords(100, 199)
            val lastCommittedOffset: Long = 50
            val lastCommitted = mock<OffsetAndMetadata> {
                on { offset() } doReturn lastCommittedOffset
            }

            val consumer = mock<KafkaConsumer<ByteArray, ByteArray>> {
                on { poll(any<Duration>()) } doReturn batch2 doReturn batch1 doThrow RuntimeException("End the loop")
                on { committed(any()) } doReturn lastCommitted
                on { listTopics() } doReturn mapOf(topic to listOf(mock()))
                on { subscription() } doReturn setOf(topic)
            }

            val provider: () -> KafkaConsumer<ByteArray, ByteArray> = { consumer }

            val successTarget = mock<SuccessTarget> {
                onBlocking {
                    upsert(any(), any())
                } doThrow RuntimeException("Failed batch") doAnswer {}
            }

            val failureTarget = mock<FailureTarget>()
            val queueRecord = mock<SourceRecord>()
            val preProcessor = preProcessor()

            val processor = mock<CompoundProcessor> {
                on {
                    process(any())
                } doReturn Pair(queueRecord, TransformationResult(JsonObject(), "TRANSFORMED_DB_OBJECT")).right()
            }

            val deleteProcessor = mock<DeleteProcessor>()
            val orchestrator =
                orchestrator(provider, preProcessor, processor, deleteProcessor, successTarget, failureTarget)

            shouldThrow<RuntimeException> { orchestrator.orchestrate() }
            verify(consumer, times(3)).poll(10.seconds.toJavaDuration())
            val failedTopicPartition = TopicPartition(topic, 0)
            verify(consumer, times(1)).committed(failedTopicPartition)
            verify(consumer, times(3)).subscription()
            verify(consumer, times(3)).listTopics()
            verify(consumer, times(1)).seek(failedTopicPartition, lastCommittedOffset)
            verify(consumer, times(19)).commitSync(any<Map<TopicPartition, OffsetAndMetadata>>())
            verify(consumer, times(1)).close()
            verifyNoMoreInteractions(consumer)
        }

        "Sends updates, deletes to success target, writes failures to the dlq" {
            val provider = consumerProvider()
            val successTarget = successTarget()
            val failureTarget = mock<FailureTarget>()
            val processor = compoundProcessor()
            val preProcessor = preProcessor()
            val deleteProcessor = deleteProcessor()

            with (orchestrator(provider, preProcessor, processor, deleteProcessor, successTarget, failureTarget)) {
                shouldThrow<RuntimeException> { orchestrate() }
            }
            verify(failureTarget, times(10)).send(any())
            verify(successTarget, times(10)).upsert(any(), any())
            verify(successTarget, times(10)).delete(any(), any())
        }

    }

    private fun deleteProcessor(): DeleteProcessor {
        val sourceRecord = mock<SourceRecord>()
        val deleteOutputs = (1..100).map {
            DeleteProcessingResult(sourceRecord, "$it").right()
        }
        return mock {
            on { process(any()) } doReturnConsecutively deleteOutputs
        }
    }

    private fun compoundProcessor(): CompoundProcessor {
        val sourceRecord = mock<SourceRecord>()
        val records = processingOutputs(sourceRecord)
        return mock {
            on { process(any()) } doReturnConsecutively records
        }
    }

    private fun processingOutputs(queueRecord: SourceRecord) =
        (1..100).map { recordNumber ->
            when {
                recordNumber % 2 == 0 -> {
                    Pair(queueRecord, TransformationResult(JsonObject(), "TRANSFORMED_DB_OBJECT")).right()
                }
                else -> {
                    consumerRecord(recordNumber).left()
                }
            }
        }

    private fun preProcessingOutputs(queueRecord: SourceRecord) =
        (1..100).map { recordNumber ->
            when (recordNumber % 3) {
                0 -> {
                    Pair(queueRecord, Pair(JsonObject(), DatabaseAction.MONGO_DELETE)).right()
                }
                1 -> {
                    Pair(queueRecord, Pair(JsonObject(), DatabaseAction.MONGO_INSERT)).right()
                }
                else -> {
                    Pair(queueRecord, Pair(JsonObject(), DatabaseAction.MONGO_UPDATE)).right()
                }
            }
        }

    private fun orchestrator(
        provider: () -> KafkaConsumer<ByteArray, ByteArray>,
        preProcessor: PreProcessor,
        processor: CompoundProcessor,
        deleteProcessor: DeleteProcessor,
        successTarget: SuccessTarget,
        failureTarget: FailureTarget): OrchestratorImpl =
            OrchestratorImpl(provider, Regex(topic), preProcessor, processor, deleteProcessor,
                            10.seconds.toJavaDuration(), successTarget, failureTarget)

    private fun successTarget(): SuccessTarget = mock {
        onBlocking {
            upsert(any(), any())
        } doAnswer {}
    }

    private fun consumerProvider(): () -> KafkaConsumer<ByteArray, ByteArray> {
        val batch = consumerRecords(0, 100)
        val consumer = mock<KafkaConsumer<ByteArray, ByteArray>> {
            on { poll(any<Duration>()) } doReturn batch doThrow RuntimeException("End the loop")
            on { listTopics() } doReturn mapOf(topic to listOf(mock()))
            on { subscription() } doReturn setOf(topic)
        }
        return { consumer }
    }

    private fun preProcessor(): PreProcessor {
        val sourceRecord = mock<SourceRecord>()
        return mock {
            on {
                process(any())
            } doReturn JsonProcessingResult(sourceRecord, Pair(JsonObject(), DatabaseAction.MONGO_INSERT)).right()
        }
    }

    private fun consumerRecords(first: Int, last: Int): ConsumerRecords<ByteArray, ByteArray> =
            ConsumerRecords((first..last).map(::consumerRecord).groupBy {
                TopicPartition(it.topic(), it.partition())
            })

    private fun consumerRecord(recordNumber: Int): ConsumerRecord<ByteArray, ByteArray> =
            mock {
                on { topic() } doReturn "db.database.collection${recordNumber % 2}"
                on { partition() } doReturn recordNumber % 5
                on { offset() } doReturn recordNumber.toLong()
            }

    private val topic = "db.database.collection0"
}
