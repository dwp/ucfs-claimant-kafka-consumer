package ucfs.claimant.consumer.processor.impl

import arrow.core.Either
import arrow.core.extensions.either.applicative.applicative
import arrow.core.fix
import arrow.core.flatMap
import arrow.core.right
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.JsonProcessingOutput
import ucfs.claimant.consumer.domain.ValidationProcessingResult
import ucfs.claimant.consumer.processor.JsonProcessor
import ucfs.claimant.consumer.utility.FunctionalUtility
import ucfs.claimant.consumer.utility.GsonExtensions.jsonObject
import ucfs.claimant.consumer.utility.GsonExtensions.string

@Component
class JsonProcessorImpl : JsonProcessor {
    override fun process(record: ValidationProcessingResult): JsonProcessingOutput =
            record.second.jsonObject()
                .flatMap {
                    Either.applicative<Any>().tupledN(it.right(), it.string("message", "@type")).fix()
                }
                .map { (json, action) ->
                    Pair(json, action)
                }
                .map { (json, _) ->
                    Pair(record.first, json)
                }.mapLeft { FunctionalUtility.processingFailure(record.first, it, "Failed to parse json") }
}
