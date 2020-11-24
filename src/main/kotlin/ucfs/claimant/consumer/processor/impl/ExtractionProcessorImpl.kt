package ucfs.claimant.consumer.processor.impl

import arrow.Kind
import arrow.core.Either
import arrow.core.ForListK
import arrow.core.extensions.either.applicative.applicative
import arrow.core.extensions.list.traverse.sequence
import arrow.core.fix
import arrow.core.flatMap
import com.google.gson.JsonObject
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.EncryptionExtractionResult
import ucfs.claimant.consumer.domain.EncryptionMetadata
import ucfs.claimant.consumer.domain.ExtractionProcessingOutput
import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.processor.ExtractionProcessor
import ucfs.claimant.consumer.utility.JsonExtensions.getObject
import ucfs.claimant.consumer.utility.JsonExtensions.getString
import ucfs.claimant.consumer.utility.LoggingExtensions.logFailedProcessingStep
import uk.gov.dwp.dataworks.logging.DataworksLogger

@Component
class ExtractionProcessorImpl : ExtractionProcessor {

    override fun process(record: Pair<SourceRecord, JsonObject>): ExtractionProcessingOutput =
            record.second.getObject("message", "encryption").flatMap { encryption ->
                listOf(encryption.getString("encryptedEncryptionKey"),
                        encryption.getString("keyEncryptionKeyId"),
                        encryption.getString("initialisationVector"))
                        .sequence(Either.applicative()).fix().map(Kind<ForListK, String>::fix)
            }.map { (encryptedKey, encryptingKeyId, initialisationVector) ->
                Pair(record.first,
                        EncryptionExtractionResult(record.second,
                                EncryptionMetadata(encryptingKeyId, encryptedKey, initialisationVector)))
            }.mapLeft {
                logger.logFailedProcessingStep("Failed to extract encryption metadata", record.first, it)
                record.first
            }

    companion object {
        private val logger = DataworksLogger.getLogger(ExtractionProcessorImpl::class)
    }
}
