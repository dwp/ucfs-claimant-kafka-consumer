package ucfs.claimant.consumer.processor.impl

import arrow.core.flatMap
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.DatakeyResult
import ucfs.claimant.consumer.domain.DecryptionProcessingOutput
import ucfs.claimant.consumer.domain.DecryptionResult
import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.processor.DecryptionProcessor
import ucfs.claimant.consumer.service.CipherService
import ucfs.claimant.consumer.utility.JsonExtensions.getString
import ucfs.claimant.consumer.utility.LoggingExtensions.logFailedProcessingStep
import uk.gov.dwp.dataworks.logging.DataworksLogger

@Component
class DecryptionProcessorImpl(private val cipherService: CipherService) : DecryptionProcessor {
    override fun process(record: Pair<SourceRecord, DatakeyResult>): DecryptionProcessingOutput =
            record.second.json.getString("message", "dbObject").flatMap { encryptedObject ->
                cipherService.decrypt(record.second.datakey, record.second.initializationVector, encryptedObject)
            }.map { decryptedDbObject ->
                Pair(record.first, DecryptionResult(record.second.json, decryptedDbObject))
            }.mapLeft {
                logger.logFailedProcessingStep("Failed to decrypt dbObject", record.first, it)
                record.first
            }

    companion object {
        private val logger = DataworksLogger.getLogger(DecryptionProcessorImpl::class)
    }
}
