package ucfs.claimant.consumer.service.impl

import arrow.core.flatMap
import io.prometheus.client.Counter
import org.springframework.stereotype.Service
import ucfs.claimant.consumer.domain.CipherServiceEncryptionData
import ucfs.claimant.consumer.domain.CipherServiceEncryptionResult
import ucfs.claimant.consumer.domain.EncryptedDataKeyServiceData
import ucfs.claimant.consumer.repository.EncryptingDataKeyRepository
import ucfs.claimant.consumer.service.EncryptionService
import ucfs.claimant.consumer.utility.CipherExtensions.encoded
import ucfs.claimant.consumer.utility.CipherExtensions.finally
import ucfs.claimant.consumer.utility.CipherExtensions.key
import ucfs.claimant.consumer.utility.FunctionalUtility.encase
import java.security.Key
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
@Service
class EncryptionServiceImpl(private val encryptingDataKeyRepository: EncryptingDataKeyRepository,
                            private val encryptingTransformation: String,
                            private val encryptingAlgorithm: String,
                            private val encryptingProvider: String,
                            private val secureRandom: SecureRandom,
                            private val maxKeyUsage: Int,
                            private val initialisationVectorSize: Int,
                            private val kmsFailures: Counter) : EncryptionService {

    override fun encrypt(plaintext: String): CipherServiceEncryptionData {
        val (encryptingKeyId, dataKey, encryptedDataKey) = encryptedDataKey
        val initialisationVector = initialisationVector()
        return dataKey.key(encryptingAlgorithm).flatMap {
            it.encryptingCipher(initialisationVector)
        }.flatMap {
            it.finally(plaintext.toByteArray())
        }.map { cipherText ->
            CipherServiceEncryptionResult(encryptingKeyId,
                initialisationVector.encoded(),
                encryptedDataKey,
                cipherText.encoded())
        }
    }


    private var encryptedDataKey: EncryptedDataKeyServiceData = encryptingDataKeyRepository.encryptedDataKey()
        get() {
            return try {
                if (keyUseCount.incrementAndGet() > maxKeyUsage) {
                    keyUseCount.set(0)
                    field = encryptingDataKeyRepository.encryptedDataKey()
                }
                field
            } catch (e: Exception) {
                kmsFailures.inc()
                throw e
            }
        }

    private fun Key.encryptingCipher(initialisationVector: ByteArray) =
        encase {
            Cipher.getInstance(encryptingTransformation, encryptingProvider).apply {
                init(Cipher.ENCRYPT_MODE, this@encase, GCMParameterSpec(16 * 8, initialisationVector))
            }
        }

    private fun initialisationVector() = ByteArray(initialisationVectorSize).apply(secureRandom::nextBytes)

    companion object {
        val keyUseCount: AtomicInteger = AtomicInteger()
    }
}
