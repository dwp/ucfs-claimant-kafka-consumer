package ucfs.claimant.consumer.transformer

import arrow.core.Either
import com.google.gson.JsonObject

interface Transformer {
    fun transform(dbObject: JsonObject): Either<Any, String>
}
