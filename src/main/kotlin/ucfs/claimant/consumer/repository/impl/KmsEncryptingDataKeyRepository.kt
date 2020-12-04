package ucfs.claimant.consumer.repository.impl

import org.springframework.stereotype.Service
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest
import ucfs.claimant.consumer.domain.EncryptedDataKeyServiceData
import ucfs.claimant.consumer.repository.EncryptingDataKeyRepository
import java.util.*

@Service
class KmsEncryptingDataKeyRepository(private val kmsClient: KmsClient,
                                     private val cmkAlias: String,
                                     private val dataKeySpec: String): EncryptingDataKeyRepository {

    override fun encryptedDataKey(): EncryptedDataKeyServiceData =
            with (kmsClient.generateDataKey(generateDataKeyRequest())) {
                Base64.getEncoder().encodeToString(ciphertextBlob().asByteArray()).let {
                    EncryptedDataKeyServiceData(keyId(), plaintext().asByteArray(), it)
                }
            }

    private fun generateDataKeyRequest(): GenerateDataKeyRequest =
            with(GenerateDataKeyRequest.builder()) {
                keyId(cmkAlias)
                keySpec(dataKeySpec)
                build()
            }
}
