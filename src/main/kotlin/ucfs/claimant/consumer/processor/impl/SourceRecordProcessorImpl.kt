package ucfs.claimant.consumer.processor.impl

import arrow.core.Either
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.QueueRecordProcessingOutput
import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.processor.SourceRecordProcessor
import ucfs.claimant.consumer.utility.LoggingExtensions.logFailedProcessingStep
import uk.gov.dwp.dataworks.logging.DataworksLogger

@Component
class SourceRecordProcessorImpl : SourceRecordProcessor {
    override fun process(record: SourceRecord): QueueRecordProcessingOutput =
            record.value()?.let { body ->
                Either.Right(Pair(record, body))
            } ?: {
                logger.logFailedProcessingStep("Failed to get message value", record, "Value returned null")
                Either.Left(record)
            }()


    companion object {
        private val logger = DataworksLogger.getLogger(SourceRecordProcessorImpl::class)
    }
}
