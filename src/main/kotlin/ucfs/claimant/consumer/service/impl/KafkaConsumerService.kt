package ucfs.claimant.consumer.service.impl

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.springframework.stereotype.Service
import sun.misc.Signal
import ucfs.claimant.consumer.service.ConsumerService
import ucfs.claimant.consumer.target.ClaimantTarget
import uk.gov.dwp.dataworks.logging.DataworksLogger
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.ExperimentalTime

@Service
class KafkaConsumerService(private val consumerProvider: suspend () -> KafkaConsumer<ByteArray, ByteArray>,
                           private val pollDuration: Duration,
                           private val target: ClaimantTarget) : ConsumerService {

    @ExperimentalTime
    override fun consume() = runBlocking {
        consumerProvider().use { consumer ->
            consumer handleSignal "INT"
            consumer handleSignal "TERM"
            consumer.processLoop()
        }
    }

    private suspend fun KafkaConsumer<ByteArray, ByteArray>.processLoop() {
        while (!closed.get()) {
            coroutineScope {
                poll(pollDuration).let { records ->
                    records.partitions().forEach { topicPartition ->
                        launch { processPartitionRecords(topicPartition, records) }
                    }
                }
            }
        }
    }

    private fun KafkaConsumer<ByteArray, ByteArray>.processPartitionRecords(topicPartition: TopicPartition, records: ConsumerRecords<ByteArray, ByteArray>) {
        try {
            val partitionRecords = records.records(topicPartition)
            target.send(topicPartition.topic(), partitionRecords)
            val lastPosition = lastPosition(partitionRecords)
            logger.info("Processed batch, committing offset",
                    "topic" to topicPartition.topic(), "partition" to "${topicPartition.partition()}",
                    "offset" to "$lastPosition")
            commitSync(mapOf(topicPartition to OffsetAndMetadata(lastPosition + 1)))
        }
        catch (e: Exception) {
            logger.error("Batch failed, not committing offset, resetting position to last commit", e,
                    "topic" to topicPartition.topic(),
                    "partition" to "${topicPartition.partition()}")
            rollback(topicPartition)
        }
    }

    private fun <K, V> KafkaConsumer<K, V>.rollback(topicPartition: TopicPartition) {
        lastCommittedOffset(topicPartition)?.let { seek(topicPartition, it) }
    }

    private fun <K, V> KafkaConsumer<K, V>.lastCommittedOffset(partition: TopicPartition): Long? =
            committed(partition)?.offset()

    private fun lastPosition(partitionRecords: MutableList<ConsumerRecord<ByteArray, ByteArray>>) =
            partitionRecords[partitionRecords.size - 1].offset()

    private infix fun <K, V> KafkaConsumer<K, V>.handleSignal(signalName: String) {
        logger.info("Setting up $signalName handler.")
        Signal.handle(Signal(signalName)) {
            logger.info("Signal received, cancelling job.", "signal" to "$it")
            closed.set(true)
            wakeup()
        }
    }

    companion object {
        private val logger = DataworksLogger.getLogger("${KafkaConsumerService::class.java}")
        private val closed: AtomicBoolean = AtomicBoolean(false)
    }
}
