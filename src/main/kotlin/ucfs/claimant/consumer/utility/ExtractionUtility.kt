package ucfs.claimant.consumer.utility

import arrow.core.*
import arrow.core.extensions.either.applicative.applicative
import arrow.core.extensions.either.monad.flatMap
import com.google.gson.JsonObject
import ucfs.claimant.consumer.domain.DatabaseAction
import ucfs.claimant.consumer.utility.GsonExtensions.string

object ExtractionUtility {

    fun JsonObject.id(sourceId: String) = string("message", "_id", sourceId)

    fun JsonObject.timestamp() =
        databaseAction().flatMap { action ->
            when (action) {
                DatabaseAction.MONGO_UPDATE, DatabaseAction.MONGO_INSERT ->
                    lastModifiedDateTime().handleErrorWith {
                        createdDateTime().handleErrorWith { epoch() }
                    }
                DatabaseAction.MONGO_DELETE -> {
                    enqueuedDateTime().handleErrorWith { epoch() }
                }
            }
        }


    private fun JsonObject.databaseAction() = string("message", "@type").flatMap(this@ExtractionUtility::enumeratedAction)

    private fun enumeratedAction(action: String) =
            runCatching { DatabaseAction.valueOf(action) }.fold(DatabaseAction::right, Throwable::left)

    private fun JsonObject.lastModifiedDateTime(): Either<Any, Tuple2<String, String>> =
            messageWrapperSubField(MODIFIED_TIMESTAMP_FIELD)

    private fun JsonObject.createdDateTime(): Either<Any, Tuple2<String, String>> =
            messageWrapperSubField(CREATED_TIMESTAMP_FIELD)

    private fun JsonObject.messageWrapperSubField(field: String): Either<Any, Tuple2<String, String>> =
            Either.applicative<Any>().tupledN(string("message", field), field.right()).fix()

    private fun JsonObject.enqueuedDateTime(): Either<Any, Tuple2<String, String>> =
            Either.applicative<Any>().tupledN(string(ENQUEUED_TIMESTAMP_FIELD), ENQUEUED_TIMESTAMP_FIELD.right()).fix()

    private fun epoch() = Either.applicative<Any>().tupledN(EPOCH.right(), EPOCH_INDICATOR.right()).fix()

    private const val EPOCH = "1980-01-01T00:00:00.000+0000"

    private const val EPOCH_INDICATOR = "epoch"
    private const val MODIFIED_TIMESTAMP_FIELD = "_lastModifiedDateTime"
    private const val CREATED_TIMESTAMP_FIELD = "createdDateTime"
    private const val ENQUEUED_TIMESTAMP_FIELD = "timestamp"
}
