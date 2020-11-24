package ucfs.claimant.consumer.processor.impl

import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.DatakeyProcessingOutput
import ucfs.claimant.consumer.domain.DatakeyResult
import ucfs.claimant.consumer.domain.ExtractionProcessingResult
import ucfs.claimant.consumer.processor.DatakeyProcessor
import ucfs.claimant.consumer.service.DatakeyService
import ucfs.claimant.consumer.utility.FunctionalUtility.processingFailure

@Component
class DatakeyProcessorImpl(private val datakeyService: DatakeyService) : DatakeyProcessor {
    override fun process(record: ExtractionProcessingResult): DatakeyProcessingOutput =
            datakeyService.decryptKey(record.second.encryptionMetadata.encryptingKeyId,
                    record.second.encryptionMetadata.encryptedKey).map { datakey ->
                Pair(record.first, DatakeyResult(record.second.json, record.second.encryptionMetadata.initialisationVector, datakey))
            }.mapLeft { processingFailure(record.first, it, "Failed to decrypt datakey") }
}
