package ucfs.claimant.consumer.service.impl

import arrow.core.flatMap
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
                            private val initialisationVectorSize: Int) : EncryptionService {

    override fun encrypt(plaintext: String): CipherServiceEncryptionData {
        val (encryptingKeyId, dataKey, encryptedDataKey) = encryptedDataKey()
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

    @Synchronized
    private fun encryptedDataKey(): EncryptedDataKeyServiceData {
        if (keyUseCount.incrementAndGet() > maxKeyUsage || currentKeyData == null) {
            keyUseCount.set(0)
            currentKeyData = encryptingDataKeyRepository.encryptedDataKey()
        }
        return currentKeyData!!
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
        var currentKeyData: EncryptedDataKeyServiceData? = null
    }
}
