package ucfs.claimant.consumer.orchestrate.impl

import arrow.core.Either
import arrow.core.flatMap
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.springframework.stereotype.Service
import sun.misc.Signal
import ucfs.claimant.consumer.domain.JsonProcessingOutput
import ucfs.claimant.consumer.domain.SourceRecordProcessingOutput
import ucfs.claimant.consumer.domain.TransformationProcessingOutput
import ucfs.claimant.consumer.orchestrate.Orchestrator
import ucfs.claimant.consumer.processor.CompoundProcessor
import ucfs.claimant.consumer.processor.PreProcessor
import ucfs.claimant.consumer.processor.SourceRecordProcessor
import ucfs.claimant.consumer.processor.ValidationProcessor
import ucfs.claimant.consumer.target.FailureTarget
import ucfs.claimant.consumer.target.SuccessTarget
import ucfs.claimant.consumer.utility.GsonExtensions.string
import ucfs.claimant.consumer.utility.KafkaConsumerUtility.subscribe
import uk.gov.dwp.dataworks.logging.DataworksLogger
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Service
class OrchestratorImpl(private val consumerProvider: () -> KafkaConsumer<ByteArray, ByteArray>,
                       private val topicRegex: Regex,
                       private val preProcessor: PreProcessor,
                       private val compoundProcessor: CompoundProcessor,
                       private val pollDuration: Duration,
                       private val successTarget: SuccessTarget,
                       private val failureTarget: FailureTarget) : Orchestrator {

    @ExperimentalTime
    override fun orchestrate() = runBlocking {
        consumerProvider().use { consumer ->
            consumer handleSignal "INT"
            consumer handleSignal "TERM"
            consumer.processLoop()
        }
    }

    private suspend fun KafkaConsumer<ByteArray, ByteArray>.processLoop() {
        while (!closed.get()) {
            coroutineScope {
                subscribe(this@processLoop, topicRegex)
                poll(pollDuration).let { records ->
                    logger.info("Fetched records", "size" to "${records.count()}")
                    records.partitions().forEach { topicPartition ->
                        launch { processPartitionRecords(topicPartition, records.records(topicPartition)) }
                    }
                }
            }
        }
    }

    private suspend fun KafkaConsumer<ByteArray, ByteArray>.processPartitionRecords(topicPartition: TopicPartition, records: List<ConsumerRecord<ByteArray, ByteArray>>) =
            sendToTargets(records, topicPartition).fold(
                ifRight = {
                    lastPosition(records).let { lastPosition ->
                        logger.info("Processed batch, committing offset",
                            "topic" to topicPartition.topic(), "partition" to "${topicPartition.partition()}",
                            "offset" to "$lastPosition")
                        commitSync(mapOf(topicPartition to OffsetAndMetadata(lastPosition + 1)))
                    }
                },
                ifLeft = {
                    logger.error("Batch failed, not committing offset, resetting position to last commit", it,
                        "topic" to topicPartition.topic(),
                        "partition" to "${topicPartition.partition()}")
                    rollback(topicPartition)
                })

   private suspend fun sendToTargets(records: List<ConsumerRecord<ByteArray, ByteArray>>, topicPartition: TopicPartition) =
            Either.catch {
                val (sourced, notSourced) =
                    records.map(preProcessor::process).partition(JsonProcessingOutput::isRight)

                //val (insertsAndUpdates, deletes) = sourced.partition(JsonProcessingOutput::isDelete)

                val (processed, notProcessed) =
                    sourced.map { it.flatMap(compoundProcessor::process) }
                        .partition(TransformationProcessingOutput::isRight)

                failureTarget.send((notSourced + notProcessed).mapNotNull { it.swap().orNull() })
                successTarget.send(topicPartition.topic(), processed.mapNotNull(TransformationProcessingOutput::orNull))
            }



    private fun <K, V> KafkaConsumer<K, V>.rollback(topicPartition: TopicPartition) =
            lastCommittedOffset(topicPartition)?.let { seek(topicPartition, it) }

    private fun <K, V> KafkaConsumer<K, V>.lastCommittedOffset(partition: TopicPartition): Long? =
            committed(partition)?.offset()

    private fun lastPosition(partitionRecords: List<ConsumerRecord<ByteArray, ByteArray>>) =
            partitionRecords[partitionRecords.size - 1].offset()

    private infix fun <K, V> KafkaConsumer<K, V>.handleSignal(signalName: String) =
            Signal.handle(Signal(signalName)) {
                logger.info("Signal received, cancelling job.", "signal" to "$it")
                closed.set(true)
                wakeup()
            }

    companion object {
        private val logger = DataworksLogger.getLogger(OrchestratorImpl::class)
        private val closed = AtomicBoolean(false)
    }
}
