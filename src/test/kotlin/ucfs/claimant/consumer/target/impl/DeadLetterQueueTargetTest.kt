package ucfs.claimant.consumer.target.impl

import com.nhaarman.mockitokotlin2.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.prometheus.client.Counter
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

class DeadLetterQueueTargetTest: StringSpec() {

    init {
        "Sends and metricates" {
            val producer = mock<KafkaProducer<ByteArray, ByteArray>>()
            val supplier: () -> KafkaProducer<ByteArray, ByteArray> = {producer}
            val child = mock<Counter.Child>()
            val counter = mock<Counter> {
                on { labels(any()) } doReturn child
            }

            val target = DeadLetterQueueTarget(supplier, DLQ_TOPIC, counter)

            val records = List(FAILED_RECORD_COUNT) {
                val key = "$it".toByteArray()
                val topic = "db.database.collection${it % 3}"
                mock<ConsumerRecord<ByteArray, ByteArray>> {
                    on { key() } doReturn key
                    on { topic() } doReturn topic
                }
            }

            target.send(records)

            argumentCaptor<ProducerRecord<ByteArray, ByteArray>> {
                verify(producer, times(FAILED_RECORD_COUNT)).send(capture())
                allValues.forEachIndexed { index, producerRecord ->
                    producerRecord.key() shouldBe "ucfs-claimant-kafka-consumer-reject-$index".toByteArray()
                    producerRecord.topic() shouldBe DLQ_TOPIC
                }
            }

            argumentCaptor<String> {
                verify(counter, times(FAILED_RECORD_COUNT)).labels(capture())
                allValues.forEachIndexed { index, label ->
                    label shouldBe "db.database.collection${index % 3}"
                }
                verifyNoMoreInteractions(counter)
            }

            verify(child, times(100)).inc()
            verifyNoMoreInteractions(child)
        }
    }

    companion object {
        private const val FAILED_RECORD_COUNT = 100
        private const val DLQ_TOPIC = "dead.letter.queue"
    }
}
