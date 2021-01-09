package ucfs.claimant.consumer.processor.impl

import arrow.core.Either
import arrow.core.extensions.either.applicative.applicative
import arrow.core.fix
import arrow.core.flatMap
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.EncryptionExtractionResult
import ucfs.claimant.consumer.domain.EncryptionMetadata
import ucfs.claimant.consumer.domain.ExtractionProcessingOutput
import ucfs.claimant.consumer.domain.JsonProcessingResult
import ucfs.claimant.consumer.processor.ExtractionProcessor
import ucfs.claimant.consumer.utility.FunctionalUtility.processingFailure
import ucfs.claimant.consumer.utility.GsonExtensions.getObject
import ucfs.claimant.consumer.utility.GsonExtensions.string

@Component
class ExtractionProcessorImpl : ExtractionProcessor {

    override fun process(record: JsonProcessingResult): ExtractionProcessingOutput {
        val (json, _) = record.second
        return json.getObject("message", "encryption").flatMap { encryption ->
            Either.applicative<Any>().tupledN(encryption.string("encryptedEncryptionKey"),
                encryption.string("keyEncryptionKeyId"),
                encryption.string("initialisationVector")).fix()
        }.map { (encryptedKey, encryptingKeyId, initialisationVector) ->
            Pair(record.first,
                EncryptionExtractionResult(record.second, EncryptionMetadata(encryptingKeyId, encryptedKey, initialisationVector)))
        }.mapLeft { processingFailure(record.first, it, "Failed to extract encryption metadata") }
    }
}
