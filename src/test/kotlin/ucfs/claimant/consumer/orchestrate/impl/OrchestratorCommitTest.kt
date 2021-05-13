package ucfs.claimant.consumer.orchestrate.impl

import arrow.core.right
import com.google.gson.JsonObject
import com.nhaarman.mockitokotlin2.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.ToleranceMatcher
import io.kotest.matchers.shouldBe
import io.prometheus.client.Gauge
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.Metric
import org.apache.kafka.common.MetricName
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
class OrchestratorCommitTest: StringSpec() {

    init {
        "Commit on success, rollback on failure" {
            val batch1 = consumerRecords(0, 99)
            val batch2 = consumerRecords(100, 199)
            val lastCommittedOffset: Long = 50
            val lastCommitted = mock<OffsetAndMetadata> {
                on { offset() } doReturn lastCommittedOffset
            }
            val failedTopicPartition = TopicPartition(topic, 0)

            val name = MetricName("records-lag-max", "consumer-fetch-manager-metrics", "Description",
                                        mapOf("topic" to "TOPIC", "partition" to "PARTITION"))

            val metric = mock<Metric> {
                on { metricValue() } doReturn 100.toDouble()
                on { metricName() } doReturn name
            }

            val consumer = mock<KafkaConsumer<ByteArray, ByteArray>> {
                on { poll(any<Duration>()) } doReturn batch2 doReturn batch1 doThrow RuntimeException("End the loop")
                on { committed(any<Set<TopicPartition>>()) } doReturn mapOf(failedTopicPartition to lastCommitted)
                on { listTopics() } doReturn mapOf(topic to listOf(mock()))
                on { subscription() } doReturn setOf(topic)
                on { metrics() } doReturn mapOf(name to metric)
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

            val childRunningApps = mock<Gauge.Child>()
            val runningApplicationsGauge = mock<Gauge> {
                on { labels(any()) } doReturn runningApplicationsGauge
            }

            val childLag = mock<Gauge.Child>()
            val lagGauge = mock<Gauge> {
                on { labels(any()) } doReturn childLag
            }
            val orchestrator = orchestrator(provider, preProcessor, processor, successTarget, failureTarget, lagGauge, runningApplicationsGauge)

            shouldThrow<RuntimeException> { orchestrator.orchestrate() }
            verify(consumer, times(3)).poll(10.seconds.toJavaDuration())
            verify(consumer, times(1)).committed(setOf(failedTopicPartition))
            verify(consumer, times(3)).subscription()
            verify(consumer, times(3)).listTopics()
            verify(consumer, times(1)).seek(failedTopicPartition, lastCommittedOffset)
            verify(consumer, times(19)).commitSync(any<Map<TopicPartition, OffsetAndMetadata>>())
            verify(consumer, times(2)).metrics()
            verify(consumer, times(1)).close()
            verifyNoMoreInteractions(consumer)

            argumentCaptor<Double> {
                verify(childLag, times(2)).set(capture())
                firstValue shouldBe ToleranceMatcher(100.toDouble(), 0.5)
            }

            verify(runningApplicationsGauge, times(1)).inc()
        }
    }

    private fun transformationResult() = FilterResult(
        TransformationResult(JsonProcessingExtract(JsonObject(),
            "id",
            DatabaseAction.MONGO_UPDATE,
            Pair("2020-01-01", "_lastModifiedDateTime")),
            "TRANSFORMED_DB_OBJECT"), true)

    private fun orchestrator(provider: () -> KafkaConsumer<ByteArray, ByteArray>,
                             preProcessor: PreProcessor,
                             processor: CompoundProcessor,
                             successTarget: SuccessTarget,
                             failureTarget: FailureTarget, 
                             lagGauge: Gauge,
                             runningApplicationsGauge: Gauge): OrchestratorImpl =
        OrchestratorImpl(provider, Regex(topic), preProcessor, processor,
            10.seconds.toJavaDuration(), successTarget, failureTarget, mock(), mock(), lagGauge, runningApplicationsGauge)


    private fun preProcessor(): PreProcessor {
        val sourceRecord = mock<SourceRecord>()
        return mock {
            on {
                process(any())
            } doReturn JsonProcessingResult(sourceRecord,
                JsonProcessingExtract(JsonObject(), "id", DatabaseAction.MONGO_INSERT,
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
