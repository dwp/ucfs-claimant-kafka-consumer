package ucfs.claimant.consumer.processor.impl

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import com.google.gson.JsonObject
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.DecryptionProcessingResult
import ucfs.claimant.consumer.domain.TransformationProcessingOutput
import ucfs.claimant.consumer.domain.TransformationResult
import ucfs.claimant.consumer.processor.TransformationProcessor
import ucfs.claimant.consumer.transformer.Transformer
import ucfs.claimant.consumer.utility.FunctionalUtility
import ucfs.claimant.consumer.utility.GsonExtensions.jsonObject

@Component
class TransformationProcessorImpl(private val claimantTransformer: Transformer,
                                  private val contractTransformer: Transformer,
                                  private val statementTransformer: Transformer,
                                  private val claimantTopic: String,
                                  private val contractTopic: String,
                                  private val statementTopic: String): TransformationProcessor {

    override fun process(record: DecryptionProcessingResult): TransformationProcessingOutput {
        return record.second.plainText.jsonObject().flatMap {
            transformed(record.first.topic(), it)
        }.map {
            Pair(record.first, TransformationResult(record.second.json, it))
        }.mapLeft {
            FunctionalUtility.processingFailure(
                record.first, it,
                "Failed to transform dbObject from '${record.first.topic()}'."
            )
        }
    }

    private fun transformed(topic: String, dbObject: JsonObject): Either<Any, String> =
            when (topic) {
                claimantTopic -> {
                    claimantTransformer.transform(dbObject)
                }
                contractTopic -> {
                    contractTransformer.transform(dbObject)
                }
                statementTopic -> {
                    statementTransformer.transform(dbObject)
                }
                else -> {
                    "No transformer for '$topic'.".left()
                }
            }
}


