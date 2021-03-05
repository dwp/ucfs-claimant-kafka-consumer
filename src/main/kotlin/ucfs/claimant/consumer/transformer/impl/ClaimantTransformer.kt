package ucfs.claimant.consumer.transformer.impl

import arrow.core.Either
import com.google.gson.JsonObject
import io.prometheus.client.Counter
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.repository.SaltRepository
import ucfs.claimant.consumer.transformer.Transformer
import ucfs.claimant.consumer.utility.GsonExtensions.getObject
import ucfs.claimant.consumer.utility.GsonExtensions.nullableString
import java.security.MessageDigest
import java.util.*

@Component
class ClaimantTransformer(private val saltRepository: SaltRepository,
                            private val saltFailures: Counter): Transformer {

    override fun transform(dbObject: JsonObject): Either<Any, String> =
        dbObject.getObject("_id")
            .map { id ->
                val nino = dbObject.nullableString("nino")
                """{
                        "_id": $id,
                        "nino": "${nino?.takeIf(String::isNotBlank)?.let(this::hash) ?: "" }"
                    }"""
            }

    private fun hash(x: String): String =
        with(Base64.getEncoder()) {
            encodeToString(digest(x)).replace('+', '-').replace('/', '_')
        }

    private fun digest(x: String) =
        try {
            MessageDigest.getInstance("SHA-512").digest("$x${saltRepository.salt()}".toByteArray())
        } catch (e: Exception) {
            saltFailures.inc()
            throw e
        }
}
