package ucfs.claimant.consumer.processor.impl

import arrow.core.flatMap
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.DatakeyProcessingResult
import ucfs.claimant.consumer.domain.DecryptionProcessingOutput
import ucfs.claimant.consumer.domain.DecryptionResult
import ucfs.claimant.consumer.processor.DecryptionProcessor
import ucfs.claimant.consumer.service.CipherService
import ucfs.claimant.consumer.utility.FunctionalUtility.processingFailure
import ucfs.claimant.consumer.utility.GsonExtensions.string

@Component
class DecryptionProcessorImpl(private val cipherService: CipherService) : DecryptionProcessor {
    override fun process(record: DatakeyProcessingResult): DecryptionProcessingOutput =
            record.second.json.string("message", "dbObject").flatMap { encryptedObject ->
                cipherService.decrypt(record.second.datakey, record.second.initializationVector, encryptedObject)
            }.map { decryptedDbObject ->
                Pair(record.first, DecryptionResult(record.second.json, decryptedDbObject))
            }.mapLeft { processingFailure(record.first, it, "Failed to decrypt dbObject") }

}
