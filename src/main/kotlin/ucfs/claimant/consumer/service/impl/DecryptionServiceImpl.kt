package ucfs.claimant.consumer.service.impl

import arrow.core.Either
import arrow.core.extensions.either.applicative.applicative
import arrow.core.flatMap
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.springframework.stereotype.Service
import ucfs.claimant.consumer.domain.DecryptionData
import ucfs.claimant.consumer.service.DecryptionService
import ucfs.claimant.consumer.utility.CipherExtensions.decoded
import ucfs.claimant.consumer.utility.CipherExtensions.finally
import ucfs.claimant.consumer.utility.CipherExtensions.key
import ucfs.claimant.consumer.utility.FunctionalUtility.encase
import java.security.Key
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

@Service
class DecryptionServiceImpl(private val decryptingTransformation: String,
                            private val decryptingAlgorithm: String,
                            private val decryptingProvider: String): DecryptionService {
    init {
        Security.addProvider(BouncyCastleProvider())
    }


    override fun decrypt(key: String, initializationVector: String, encrypted: String): DecryptionData =
        Either.applicative<Any>().tupledN(key.decoded(), initializationVector.decoded())
            .flatMap { (keyBytes, iv) ->
                keyBytes.key(decryptingAlgorithm).flatMap {
                    it.decryptingCipher(iv)
                }
            }.flatMap { cipher ->
                encrypted.decoded().flatMap {
                    cipher.finally(it)
                }
            }.map(::String)

    private fun Key.decryptingCipher(initialisationVector: ByteArray) =
        encase {
            Cipher.getInstance(decryptingTransformation, decryptingProvider).apply {
                init(Cipher.DECRYPT_MODE, this@encase, IvParameterSpec(initialisationVector))
            }
        }
}
