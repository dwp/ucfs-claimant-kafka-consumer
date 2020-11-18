package ucfs.claimant.consumer.service.impl

import arrow.Kind
import arrow.core.Either
import arrow.core.ForListK
import arrow.core.extensions.either.applicative.applicative
import arrow.core.extensions.list.traverse.sequence
import arrow.core.fix
import arrow.core.flatMap
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.springframework.stereotype.Service
import ucfs.claimant.consumer.domain.CipherServiceResult
import ucfs.claimant.consumer.service.CipherService
import ucfs.claimant.consumer.utility.FunctionalUtility.encase
import java.security.Key
import java.security.Security
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Service
class CipherServiceImpl : CipherService {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    override fun decrypt(key: String, initializationVector: String, encrypted: String): CipherServiceResult =
            listOf(key.decoded(), initializationVector.decoded())
                    .sequence(Either.applicative()).fix().map(Kind<ForListK, ByteArray>::fix)
                    .flatMap { (keyBytes, iv) ->
                        keyBytes.key().flatMap {
                            it.cipher(iv)
                        }
                    }.flatMap { cipher ->
                        encrypted.decoded().flatMap {
                            cipher.finally(it)
                        }
                    }.map(::String)

    private fun Cipher.finally(it: ByteArray) =
            encase { doFinal(it) }

    private fun Key.cipher(initialisationVector: ByteArray) =
            encase {
                Cipher.getInstance("AES/CTR/NoPadding", "BC").apply {
                    init(Cipher.DECRYPT_MODE, this@encase, IvParameterSpec(initialisationVector))
                }
            }

    private fun ByteArray.key() =
            encase { SecretKeySpec(this, "AES") }

    private fun String.decoded() =
            encase { Base64.getDecoder().decode(this.toByteArray()) }
}

