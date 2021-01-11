package ucfs.claimant.consumer.processor.impl

import arrow.core.*
import arrow.core.extensions.either.applicative.applicative
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.DatabaseAction
import ucfs.claimant.consumer.domain.JsonProcessingExtract
import ucfs.claimant.consumer.domain.JsonProcessingOutput
import ucfs.claimant.consumer.domain.ValidationProcessingResult
import ucfs.claimant.consumer.processor.JsonProcessor
import ucfs.claimant.consumer.utility.ExtractionUtility.id
import ucfs.claimant.consumer.utility.ExtractionUtility.timestamp
import ucfs.claimant.consumer.utility.FunctionalUtility
import ucfs.claimant.consumer.utility.GsonExtensions.jsonObject
import ucfs.claimant.consumer.utility.GsonExtensions.string

@Component
class JsonProcessorImpl(@Qualifier("idSourceFields") private val idSourceFields: Map<String, String>) : JsonProcessor {

    override fun process(record: ValidationProcessingResult): JsonProcessingOutput =
        record.second.jsonObject().flatMap {
                Either.applicative<Any>().tupledN(it.right(),
                    it.string("message", "@type"),
                    idSourceField(record.first.topic())).fix()
            }
            .flatMap { (json, action, idSourceField) ->
                Either.applicative<Any>().tupledN(
                    json.right(),
                    json.id(idSourceField),
                    databaseAction(action),
                    json.timestamp()).fix()
            }
            .map { (json, id, action, timestampAndSource) ->
                val (timestamp, source) = timestampAndSource
                Pair(record.first, JsonProcessingExtract(json, id, action, Pair(timestamp, source)))
            }.mapLeft { FunctionalUtility.processingFailure(record.first, it, "Failed to parse json") }

    private fun databaseAction(action: String): Either<Throwable, DatabaseAction> =
        runCatching { DatabaseAction.valueOf(action) }.fold(DatabaseAction::right, Throwable::left)

    private fun idSourceField(topic: String) =
            idSourceFields[topic].rightIfNotNull {
                "No source id configured for topic '$topic'."
            }
}
