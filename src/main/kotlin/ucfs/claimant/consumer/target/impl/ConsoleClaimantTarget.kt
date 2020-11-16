package ucfs.claimant.consumer.target.impl

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.target.ClaimantTarget
import uk.gov.dwp.dataworks.logging.DataworksLogger

@Component
class ConsoleClaimantTarget: ClaimantTarget {

    override fun send(topic: String, records: List<ConsumerRecord<ByteArray, ByteArray>>) {
        records.forEach(this::printMetadata)
    }

    private fun printMetadata(record: ConsumerRecord<ByteArray, ByteArray>) {
        logger.info("Consumed record",
                "topic" to record.topic(),
                "key" to String(record.key()),
                "timestamp" to "${record.timestamp()}",
                "partition" to "${record.partition()}",
                "offset" to "${record.offset()}")
    }

    companion object {
        private val logger = DataworksLogger.getLogger("${ConsoleClaimantTarget::class.java}")
    }

}
