package ucfs.claimant.consumer.repository

import arrow.core.Either

interface SecretRepository {
    fun secret(name: String): Either<Throwable, String>
}
