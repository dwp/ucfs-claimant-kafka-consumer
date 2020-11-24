package ucfs.claimant.consumer.target.impl

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.target.FailureTarget

@Component
class DeadLetterQueueTarget(private val producerProvider: () -> KafkaProducer<ByteArray, ByteArray>,
                            private val dlqTopic: String) : FailureTarget {

    override fun send(records: List<SourceRecord>) {
        producerProvider().use { producer ->
            records.forEach { consumerRecord ->
                producer.send(producerRecord(consumerRecord))
            }
        }
    }

    private fun producerRecord(consumerRecord: SourceRecord): ProducerRecord<ByteArray, ByteArray> =
            ProducerRecord(dlqTopic, null, consumerRecord.timestamp(), consumerRecord.key(), consumerRecord.value())
}
