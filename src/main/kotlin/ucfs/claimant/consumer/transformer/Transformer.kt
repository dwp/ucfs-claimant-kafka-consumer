package ucfs.claimant.consumer.transformer

import arrow.core.Either

interface Transformer {
    fun transform(dbObject: String): Either<Any, String>
}
