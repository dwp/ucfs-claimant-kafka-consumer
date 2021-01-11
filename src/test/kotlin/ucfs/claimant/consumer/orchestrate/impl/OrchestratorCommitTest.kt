package ucfs.claimant.consumer.orchestrate.impl

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
import ucfs.claimant.consumer.processor.PreProcessor
import ucfs.claimant.consumer.target.FailureTarget
import ucfs.claimant.consumer.target.SuccessTarget
import java.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds
import kotlin.time.toJavaDuration

@ExperimentalTime
class OrchestratorCommitTest : StringSpec() {

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
                } doReturn Pair(queueRecord, transformationResult()).right()
            }

            val orchestrator = orchestrator(provider, preProcessor, processor, successTarget, failureTarget)

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

    }

    private fun transformationResult() = TransformationResult(
        JsonProcessingExtract(JsonObject(), "id", DatabaseAction.MONGO_UPDATE, Pair("2020-01-01", "_lastModifiedDateTime")),
        "TRANSFORMED_DB_OBJECT")

    private fun orchestrator(
        provider: () -> KafkaConsumer<ByteArray, ByteArray>,
        preProcessor: PreProcessor,
        processor: CompoundProcessor,
        successTarget: SuccessTarget,
        failureTarget: FailureTarget): OrchestratorImpl =
            OrchestratorImpl(provider, Regex(topic), preProcessor, processor,
                            10.seconds.toJavaDuration(), successTarget, failureTarget)


    private fun preProcessor(): PreProcessor {
        val sourceRecord = mock<SourceRecord>()
        return mock {
            on {
                process(any())
            } doReturn JsonProcessingResult(sourceRecord,
                JsonProcessingExtract(JsonObject(), "id",
                                        DatabaseAction.MONGO_INSERT,
                                        Pair("date", "datesource"))).right()
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
