package ucfs.claimant.consumer.processor.impl

import arrow.core.*
import arrow.core.extensions.either.applicative.applicative
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.DatabaseAction
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
                .flatMap { (json, action) ->
                    Either.applicative<Any>().tupledN(json.right(), databaseAction(action)).fix()
                }
                .map { (json, action) ->
                    Pair(record.first, Pair(json, action))
                }.mapLeft { FunctionalUtility.processingFailure(record.first, it, "Failed to parse json") }

    private fun databaseAction(action: String): Either<Throwable, DatabaseAction> =
        runCatching { DatabaseAction.valueOf(action) }.fold(DatabaseAction::right, Throwable::left)





//        val epoch = "1980-01-01T00:00:00.000+0000"
//        val recordType = json?.lookup<String?>("message.@type")?.get(0)
//        val lastModifiedTimestampStr = json?.lookup<String?>("message._lastModifiedDateTime")?.get(0)
//
//        if (recordType == "MONGO_DELETE") {
//            val kafkaTimestampStr = json.lookup<String?>("timestamp")[0]
//            if (!kafkaTimestampStr.isNullOrBlank()) {
//                return Pair(kafkaTimestampStr, "kafkaMessageDateTime")
//            }
//        }
//
//        if (!lastModifiedTimestampStr.isNullOrBlank()) {
//            return Pair(lastModifiedTimestampStr, "_lastModifiedDateTime")
//        }
//
//        val createdTimestampStr = json?.lookup<String?>("message.createdDateTime")?.get(0)
//        if (!createdTimestampStr.isNullOrBlank()) {
//            return Pair(createdTimestampStr, "createdDateTime")
//        }
//
//        return Pair(epoch, "epoch")

}
