package ucfs.claimant.consumer.processor.impl

import arrow.core.*
import arrow.core.extensions.either.applicative.applicative
import com.google.gson.JsonObject
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.*
import ucfs.claimant.consumer.processor.JsonProcessor
import ucfs.claimant.consumer.utility.FunctionalUtility
import ucfs.claimant.consumer.utility.GsonExtensions.jsonObject
import ucfs.claimant.consumer.utility.GsonExtensions.string

@Component
class JsonProcessorImpl : JsonProcessor {

//    typealias JsonProcessingResult = Pair<SourceRecord, Pair<JsonObject, DatabaseAction>>
//    typealias JsonProcessingOutput = Either<SourceRecord, JsonProcessingResult>

    override fun process(record: ValidationProcessingResult): JsonProcessingOutput =
            record.second.jsonObject()
                .flatMap {
                    Either.applicative<Any>().tupledN(it.right(), it.string("message", "@type")).fix()
                }
                .flatMap { (json, action) ->
                    Either.applicative<Any>().tupledN(json.right(), databaseAction(action)).fix()
                }
                .map { (json, action) ->
                    Pair(record.first, Pair(json, action))
                }.mapLeft { FunctionalUtility.processingFailure(record.first, it, "Failed to parse json") }

    private fun databaseAction(action: String): Either<Throwable, DatabaseAction> =
        runCatching { DatabaseAction.valueOf(action) }.fold(DatabaseAction::right, Throwable::left)
}
