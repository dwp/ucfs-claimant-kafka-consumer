package ucfs.claimant.consumer.processor.impl

import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.DataKeyResult
import ucfs.claimant.consumer.domain.DatakeyProcessingOutput
import ucfs.claimant.consumer.domain.ExtractionProcessingResult
import ucfs.claimant.consumer.processor.DatakeyProcessor
import ucfs.claimant.consumer.repository.DecryptingDataKeyRepository
import ucfs.claimant.consumer.utility.FunctionalUtility.processingFailure

@Component
class DataKeyProcessorImpl(private val decryptingDataKeyRepository: DecryptingDataKeyRepository) : DatakeyProcessor {
    override fun process(record: ExtractionProcessingResult): DatakeyProcessingOutput =
            decryptingDataKeyRepository.decryptDataKey(
                record.second.encryptionMetadata.encryptingKeyId,
                record.second.encryptionMetadata.encryptedKey).map { datakey ->
                Pair(record.first, DataKeyResult(record.second.extract, record.second.encryptionMetadata.initialisationVector, datakey))
            }.mapLeft { processingFailure(record.first, it, "Failed to decrypt datakey") }
}
