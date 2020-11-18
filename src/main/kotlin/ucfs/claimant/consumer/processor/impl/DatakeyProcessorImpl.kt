package ucfs.claimant.consumer.processor.impl

import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.DatakeyProcessingOutput
import ucfs.claimant.consumer.domain.DatakeyResult
import ucfs.claimant.consumer.domain.EncryptionExtractionResult
import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.processor.DatakeyProcessor
import ucfs.claimant.consumer.service.DatakeyService
import ucfs.claimant.consumer.utility.LoggingExtensions.logFailedProcessingStep
import uk.gov.dwp.dataworks.logging.DataworksLogger

@Component
class DatakeyProcessorImpl(private val datakeyService: DatakeyService) : DatakeyProcessor {
    override fun process(record: Pair<SourceRecord, EncryptionExtractionResult>): DatakeyProcessingOutput =
            datakeyService.decryptKey(record.second.encryptionMetadata.encryptingKeyId,
                    record.second.encryptionMetadata.encryptedKey).map { datakey ->
                Pair(record.first, DatakeyResult(record.second.json, record.second.encryptionMetadata.initialisationVector, datakey))
            }.mapLeft {
                logger.logFailedProcessingStep("Failed to decrypt datakey", record.first, it)
                record.first
            }

    companion object {
        private val logger = DataworksLogger.getLogger(DatakeyProcessorImpl::class)
    }
}
