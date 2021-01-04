package ucfs.claimant.consumer.processor.impl

import arrow.core.Either
import arrow.core.extensions.either.applicative.applicative
import arrow.core.fix
import arrow.core.flatMap
import arrow.core.rightIfNotNull
import com.google.gson.JsonObject
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.DecryptionProcessingResult
import ucfs.claimant.consumer.domain.TransformationProcessingOutput
import ucfs.claimant.consumer.domain.TransformationResult
import ucfs.claimant.consumer.processor.TransformationProcessor
import ucfs.claimant.consumer.transformer.Transformer
import ucfs.claimant.consumer.utility.FunctionalUtility
import ucfs.claimant.consumer.utility.GsonExtensions.jsonObject
import ucfs.claimant.consumer.utility.GsonExtensions.string

@Component
class TransformationProcessorImpl(@Qualifier("transformers") private val transformers: Map<String, Transformer>,
                                  @Qualifier("idFields") private val idFields: Map<String, String>): TransformationProcessor {

    override fun process(record: DecryptionProcessingResult): TransformationProcessingOutput {
        return record.second.plainText.jsonObject().flatMap {
            Either.applicative<Any>().tupledN(transformed(record.first.topic(), it), naturalIdValue(record.first.topic(), it)).fix()
        }.map { (transformed, naturalId) ->
            Pair(record.first, TransformationResult(record.second.json, transformed, naturalId))
        }.mapLeft {
            FunctionalUtility.processingFailure(record.first, it,"Failed to transform dbObject from '${record.first.topic()}'.")
        }
    }

    private fun naturalIdValue(topic: String, dbObject: JsonObject) =
            idFields[topic].rightIfNotNull {
                "No topic id field for: '$topic'."
            }.flatMap {
                dbObject.string("_id", it)
            }

    private fun transformed(topic: String, dbObject: JsonObject): Either<Any, String> =
            transformers[topic].rightIfNotNull {
                "No transformer configured for '$topic'."
            }.flatMap {
                it.transform(dbObject)
            }
}


