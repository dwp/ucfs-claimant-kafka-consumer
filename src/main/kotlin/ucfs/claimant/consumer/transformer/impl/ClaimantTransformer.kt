package ucfs.claimant.consumer.transformer.impl

import arrow.core.Either
import arrow.core.extensions.either.applicative.applicative
import arrow.core.fix
import arrow.core.flatMap
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.repository.SaltRepository
import ucfs.claimant.consumer.transformer.Transformer
import ucfs.claimant.consumer.utility.GsonExtensions.getObject
import ucfs.claimant.consumer.utility.GsonExtensions.json
import ucfs.claimant.consumer.utility.GsonExtensions.jsonObject
import ucfs.claimant.consumer.utility.GsonExtensions.string
import java.security.MessageDigest
import java.util.*

@Component
class ClaimantTransformer(private val saltRepository: SaltRepository): Transformer {

    override fun transform(dbObject: String): Either<Any, String> =
        dbObject.jsonObject().flatMap {
            Either.applicative<Any>().tupledN(it.getObject("_id"), it.string("nino")).fix()
        }.map { (id, nino) -> """{
                    "_id": ${id.json()},
                    "nino": "${hash(nino)}"
               }"""
        }

    private fun hash(x: String): String =
        with(Base64.getEncoder()) {
            encodeToString(digest(x)).replace('+', '-').replace('/', '_')
        }

    private fun digest(x: String) =
        MessageDigest.getInstance("SHA-512").digest("$x${saltRepository.salt()}".toByteArray())
}
