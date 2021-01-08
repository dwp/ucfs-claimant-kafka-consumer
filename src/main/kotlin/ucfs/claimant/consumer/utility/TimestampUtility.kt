package ucfs.claimant.consumer.utility

import arrow.core.*
import arrow.core.extensions.either.applicative.applicative
import com.google.gson.JsonObject
import ucfs.claimant.consumer.domain.DatabaseAction
import ucfs.claimant.consumer.utility.GsonExtensions.string

object TimestampUtility {

    fun timestamp(json: JsonObject) =
        databaseAction(json).flatMap(this::enumeratedAction).flatMap { action ->
            when (action) {
                DatabaseAction.MONGO_UPDATE, DatabaseAction.MONGO_INSERT ->
                    lastModifiedDateTime(json).handleErrorWith {
                        createdDateTime(json).handleErrorWith { epoch() }
                    }
                DatabaseAction.MONGO_DELETE -> {
                    enqueuedDateTime(json).handleErrorWith { epoch() }
                }
            }
        }


    private fun databaseAction(json: JsonObject) = json.string("message", "@type")

    private fun enumeratedAction(action: String) =
            runCatching { DatabaseAction.valueOf(action) }.fold(DatabaseAction::right, Throwable::left)

    private fun lastModifiedDateTime(json: JsonObject): Either<Any, Tuple2<String, String>> =
            messageWrapperSubField(json, MODIFIED_TIMESTAMP_FIELD)

    private fun createdDateTime(json: JsonObject): Either<Any, Tuple2<String, String>> =
            messageWrapperSubField(json, CREATED_TIMESTAMP_FIELD)

    private fun messageWrapperSubField(json: JsonObject, field: String): Either<Any, Tuple2<String, String>> =
            Either.applicative<Any>().tupledN(json.string("message", field), field.right()).fix()

    private fun enqueuedDateTime(json: JsonObject): Either<Any, Tuple2<String, String>> =
            Either.applicative<Any>().tupledN(json.string(ENQUEUED_TIMESTAMP_FIELD), ENQUEUED_TIMESTAMP_FIELD.right()).fix()

    private fun epoch() = Either.applicative<Any>().tupledN(EPOCH.right(), EPOCH_INDICATOR.right()).fix()

    private const val EPOCH = "1980-01-01T00:00:00.000+0000"

    private const val EPOCH_INDICATOR = "epoch"
    private const val MODIFIED_TIMESTAMP_FIELD = "_lastModifiedDateTime"
    private const val CREATED_TIMESTAMP_FIELD = "createdDateTime"
    private const val ENQUEUED_TIMESTAMP_FIELD = "timestamp"
}
