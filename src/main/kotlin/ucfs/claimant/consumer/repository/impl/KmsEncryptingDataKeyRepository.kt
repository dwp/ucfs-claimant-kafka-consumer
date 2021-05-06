package ucfs.claimant.consumer.repository.impl

import io.prometheus.client.Counter
import io.prometheus.client.spring.web.PrometheusTimeMethod
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest
import ucfs.claimant.consumer.domain.EncryptedDataKeyServiceData
import ucfs.claimant.consumer.repository.EncryptingDataKeyRepository
import ucfs.claimant.consumer.utility.CipherExtensions.encoded
import java.util.*

@Repository
class KmsEncryptingDataKeyRepository(private val kmsClient: KmsClient,
                                     private val cmkAlias: String,
                                     private val dataKeySpec: String,
                                     private val kmsRetries: Counter): EncryptingDataKeyRepository {

    @PrometheusTimeMethod(name = "uckc_encrypted_datakey", help = "Duration of KMS encrypted datakey requests")
    @Retryable(value = [Exception::class],
        maxAttemptsExpression = "\${kms.retry.maxAttempts:5}",
        backoff = Backoff(delayExpression = "\${kms.retry.delay:1000}",
            multiplierExpression = "\${kms.retry.multiplier:2}"))
    override fun encryptedDataKey(): EncryptedDataKeyServiceData =
        try {
            with (kmsClient.generateDataKey(generateDataKeyRequest())) {
                ciphertextBlob().asByteArray().encoded().let {
                    EncryptedDataKeyServiceData(keyId(), plaintext().asByteArray(), it)
                }
            }
        } catch (e: Exception) {
            kmsRetries.inc()
            throw e
        }

    private fun generateDataKeyRequest(): GenerateDataKeyRequest =
            with(GenerateDataKeyRequest.builder()) {
                keyId(cmkAlias)
                keySpec(dataKeySpec)
                build()
            }
}
