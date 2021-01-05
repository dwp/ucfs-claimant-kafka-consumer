package ucfs.claimant.consumer.processor.impl

import arrow.core.rightIfNotNull
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.processor.SourceRecordProcessor
import ucfs.claimant.consumer.utility.LoggingExtensions.logFailedProcessingStep
import uk.gov.dwp.dataworks.logging.DataworksLogger

@Component
class SourceRecordProcessorImpl : SourceRecordProcessor {

    override fun process(record: SourceRecord) =
            record.value().rightIfNotNull {
                logger.logFailedProcessingStep("Failed to get message value", record, "Value returned null")
                record
            }.map { body ->
                Pair(record, String(body))
            }

    companion object {
        private val logger = DataworksLogger.getLogger(SourceRecordProcessorImpl::class)
    }
}
