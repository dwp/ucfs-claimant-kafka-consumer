package ucfs.claimant.consumer.target.impl

import io.prometheus.client.Counter
import io.prometheus.client.spring.web.PrometheusTimeMethod
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.target.FailureTarget
import uk.gov.dwp.dataworks.logging.DataworksLogger

@Component
class DeadLetterQueueTarget(private val producerProvider: () -> KafkaProducer<ByteArray, ByteArray>,
                            private val dlqTopic: String,
                            private val failedRecords: Counter) : FailureTarget {

    @PrometheusTimeMethod(name = "uckc_dlq", help = "Duration and count of DLQ posts")
    override fun send(records: List<SourceRecord>) {
        if (records.isNotEmpty()) {
            records.forEach {
                logger.warn("Sending record to the dlq", "topic" to it.topic(),
                    "key" to String(it.key()), "offset" to "${it.offset()}", "partition" to "${it.partition()}",
                    "message_timestamp" to "${it.timestamp()}")
                failedRecords.labels(it.topic()).inc()
            }
            producerProvider().use { producer -> records.map(::producerRecord).forEach(producer::send) }
        }
    }

    private fun producerRecord(consumerRecord: SourceRecord): ProducerRecord<ByteArray, ByteArray> =
            ProducerRecord(dlqTopic, null,
                consumerRecord.timestamp(),
                "ucfs-claimant-kafka-consumer-reject-".toByteArray() + consumerRecord.key(),
                consumerRecord.value())

    companion object {
        private val logger = DataworksLogger.getLogger(DeadLetterQueueTarget::class)
    }
}
