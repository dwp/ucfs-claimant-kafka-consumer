package ucfs.claimant.consumer.target

import org.apache.kafka.clients.consumer.ConsumerRecord

interface ClaimantTarget {
    fun send(topic: String, records: List<ConsumerRecord<ByteArray, ByteArray>>)
}
