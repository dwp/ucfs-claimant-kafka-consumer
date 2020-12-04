package ucfs.claimant.consumer.processor.impl

import arrow.core.Either
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.DecryptionProcessingResult
import ucfs.claimant.consumer.domain.TransformationProcessingOutput
import ucfs.claimant.consumer.domain.TransformationResult
import ucfs.claimant.consumer.processor.TransformationProcessor
import ucfs.claimant.consumer.transformer.Transformer
import ucfs.claimant.consumer.utility.FunctionalUtility


@Component
class TransformationProcessorImpl(private val claimantTransformer: Transformer,
                                  private val contractTransformer: Transformer,
                                  private val statementTransformer: Transformer,
                                  private val claimantSourceTopic: String,
                                  private val contractSourceTopic: String,
                                  private val statementSourceTopic: String): TransformationProcessor {

    override fun process(record: DecryptionProcessingResult): TransformationProcessingOutput =
            transformed(record.first.topic(), record.second.plainText).map {
                Pair(record.first, TransformationResult(record.second.json, it))
            }.mapLeft {
                FunctionalUtility.processingFailure(record.first, it,
                    "Failed to transform dbObject from '${record.first.topic()}'.")
            }

    private fun transformed(topic: String, decryptedDbObject: String): Either<Any, String> =
            when (topic) {
                claimantSourceTopic -> {
                    claimantTransformer.transform(decryptedDbObject)
                }
                contractSourceTopic -> {
                    contractTransformer.transform(decryptedDbObject)
                }
                statementSourceTopic -> {
                    statementTransformer.transform(decryptedDbObject)
                }
                else -> {
                    Either.Left("No transformer for '$topic'.")
                }
            }
}
