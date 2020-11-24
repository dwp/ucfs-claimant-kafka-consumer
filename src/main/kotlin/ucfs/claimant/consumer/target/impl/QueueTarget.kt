package ucfs.claimant.consumer.target.impl

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.DecryptionResult
import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.target.SuccessTarget
import uk.gov.dwp.dataworks.logging.DataworksLogger

@Component
@Profile("QUEUE_TARGET")
class QueueTarget(private val producerProvider: () -> KafkaProducer<ByteArray, ByteArray>) : SuccessTarget {
    override suspend fun send(topic: String, records: List<Pair<SourceRecord, DecryptionResult>>) {
        producerProvider().use { producer ->
            coroutineScope {
                records.forEach { (record, result) ->
                    val producerRecord = ProducerRecord<ByteArray, ByteArray>("$topic.success", record.key(),
                            result.decryptedDbObject.toByteArray())

                    launch {
                        val metadata = producer.send(producerRecord)
                        while (!metadata.isDone) {
                            yield()
                        }
                        val data = metadata.get()
                        logger.info("Successfully sent transformed record",
                                "topic" to data.topic(),
                                "offset" to "${data.offset()}",
                                "partition" to "${data.partition()}", "body" to result.decryptedDbObject)
                    }
                }
            }
        }
    }

    companion object {
        private val logger = DataworksLogger.getLogger(QueueTarget::class)
    }
}
