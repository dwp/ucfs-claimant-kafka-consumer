package ucfs.claimant.consumer.orchestrate.impl

import arrow.core.Either
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.springframework.stereotype.Service
import sun.misc.Signal
import ucfs.claimant.consumer.domain.*
import ucfs.claimant.consumer.orchestrate.Orchestrator
import ucfs.claimant.consumer.processor.CompoundProcessor
import ucfs.claimant.consumer.processor.PreProcessor
import ucfs.claimant.consumer.target.FailureTarget
import ucfs.claimant.consumer.target.SuccessTarget
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


    private suspend fun KafkaConsumer<ByteArray, ByteArray>.processPartitionRecords(topicPartition: TopicPartition, records: List<SourceRecord>) =
            sendToTargets(topicPartition.topic(), records).fold(
                ifRight = {
                    lastPosition(records).let { lastPosition ->
                        logger.info("Processed batch, committing offset",
                            "topic" to topicPartition.topic(), "partition" to "${topicPartition.partition()}",
                            "offset" to "$lastPosition")
                        commitSync(mapOf(topicPartition to OffsetAndMetadata(lastPosition + 1)))
                    }
                },
                ifLeft = { e ->
                    logger.error("Batch failed, not committing offset, resetting position to last commit", e,
                        "topic" to topicPartition.topic(),
                        "partition" to "${topicPartition.partition()}")
                    rollback(topicPartition)
                })

   private suspend fun sendToTargets(topic: String, records: List<SourceRecord>) =
            Either.catch {
                val (successfullySourced, failedPreprocessing) = splitPreprocessed(records)
                val (additionsAndModifications, deletes) = splitActions(successfullySourced)
                val (processed, failedProcessing) = splitProcessed(additionsAndModifications)
                sendFailures(failedPreprocessing + failedProcessing)
                sendAdditionsAndModifications(topic, processed)
                sendDeletes(topic, deletes)
            }



    private suspend fun sendAdditionsAndModifications(topicPartition: String,
                                                      results: List<FilterProcessingOutput>) =
            successTarget.upsert(topicPartition, results.mapNotNull(FilterProcessingOutput::orNull))


    private suspend fun sendDeletes(topic: String, deletes: List<JsonProcessingResult>) =
            successTarget.delete(topic, deletes)


    private fun splitProcessed(additionsAndModifications: List<JsonProcessingResult>) =
            additionsAndModifications.map(compoundProcessor::process).partition(FilterProcessingOutput::isRight)

    private fun splitActions(sourced: List<JsonProcessingOutput>) =
            sourced.mapNotNull(JsonProcessingOutput::orNull).partition { (_, extract) ->
                extract.action != DatabaseAction.MONGO_DELETE
            }

    private fun splitPreprocessed(records: List<ConsumerRecord<ByteArray, ByteArray>>) =
            records.map(preProcessor::process).partition(JsonProcessingOutput::isRight)

    private fun sendFailures(failed: List<Either<ConsumerRecord<ByteArray, ByteArray>, Pair<ConsumerRecord<ByteArray, ByteArray>, Any>>>) =
            failureTarget.send(failed
                .map(Either<SourceRecord, Pair<SourceRecord, Any>>::swap)
                .mapNotNull(Either<Pair<SourceRecord, Any>, SourceRecord>::orNull))

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
