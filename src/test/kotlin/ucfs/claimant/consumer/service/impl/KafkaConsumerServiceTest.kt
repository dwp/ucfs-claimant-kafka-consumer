package ucfs.claimant.consumer.service.impl

import com.nhaarman.mockitokotlin2.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import ucfs.claimant.consumer.target.ClaimantTarget
import java.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds
import kotlin.time.toJavaDuration

@ExperimentalTime
class KafkaConsumerServiceTest: StringSpec() {
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
            }

            val provider: suspend () -> KafkaConsumer<ByteArray, ByteArray> = { consumer }
            val target = mock<ClaimantTarget> {
                on { send(any(), any()) } doThrow RuntimeException("Failed batch") doAnswer {}
            }
            val consumerService = KafkaConsumerService(provider, 10.seconds.toJavaDuration(), target)
            shouldThrow<RuntimeException> { consumerService.consume() }
            verify(consumer, times(3)).poll(10.seconds.toJavaDuration())
            val failedTopicPartition = TopicPartition("db.database.collection0", 0)
            verify(consumer, times(1)).committed(failedTopicPartition)
            verify(consumer, times(1)).seek(failedTopicPartition, lastCommittedOffset)
            verify(consumer, times(19)).commitSync(any<Map<TopicPartition, OffsetAndMetadata>>())
            verify(consumer, times(1)).close()
            verifyNoMoreInteractions(consumer)
        }
    }

    private fun consumerRecords(first: Int, last: Int): ConsumerRecords<ByteArray, ByteArray> =
            ConsumerRecords((first .. last).map(::consumerRecord).groupBy {
                TopicPartition(it.topic(), it.partition())
            })

    private fun consumerRecord(recordNumber: Int): ConsumerRecord<ByteArray, ByteArray> =
            mock {
                on { topic() } doReturn "db.database.collection${recordNumber % 2}"
                on { partition() } doReturn recordNumber % 5
                on { offset() } doReturn recordNumber.toLong()
            }
}
