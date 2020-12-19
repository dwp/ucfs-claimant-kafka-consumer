package ucfs.claimant.consumer.repository.impl

import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import ucfs.claimant.consumer.repository.SecretRepository

@Repository
class SecretsManagerSecretRepository(private val secretsManagerClient: SecretsManagerClient): SecretRepository {

    override fun secret(name: String): String =
        secretsManagerClient.getSecretValue(secretValueRequest(name)).secretString()

    private fun secretValueRequest(secretName: String): GetSecretValueRequest =
        with(GetSecretValueRequest.builder()) {
            secretId(secretName)
            build()
        }
}
