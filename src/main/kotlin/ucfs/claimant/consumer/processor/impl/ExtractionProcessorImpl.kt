package ucfs.claimant.consumer.processor.impl

import arrow.Kind
import arrow.core.Either
import arrow.core.ForListK
import arrow.core.extensions.either.applicative.applicative
import arrow.core.extensions.list.traverse.sequence
import arrow.core.fix
import arrow.core.flatMap
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.*
import ucfs.claimant.consumer.processor.ExtractionProcessor
import ucfs.claimant.consumer.utility.FunctionalUtility.processingFailure
import ucfs.claimant.consumer.utility.GsonExtensions.getObject
import ucfs.claimant.consumer.utility.GsonExtensions.string

@Component
class ExtractionProcessorImpl : ExtractionProcessor {

    override fun process(record: JsonProcessingResult): ExtractionProcessingOutput =
            record.second.getObject("message", "encryption").flatMap { encryption ->
                listOf(encryption.string("encryptedEncryptionKey"),
                        encryption.string("keyEncryptionKeyId"),
                        encryption.string("initialisationVector"))
                        .sequence(Either.applicative()).fix().map(Kind<ForListK, String>::fix)
            }.map { (encryptedKey, encryptingKeyId, initialisationVector) ->
                Pair(record.first,
                        EncryptionExtractionResult(record.second,
                                EncryptionMetadata(encryptingKeyId, encryptedKey, initialisationVector)))
            }.mapLeft { processingFailure(record.first, it, "Failed to extract encryption metadata") }
}
