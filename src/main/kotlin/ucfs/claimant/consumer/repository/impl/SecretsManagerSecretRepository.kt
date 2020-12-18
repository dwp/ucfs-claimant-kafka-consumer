package ucfs.claimant.consumer.repository.impl

import arrow.core.Either
import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse
import ucfs.claimant.consumer.repository.SecretRepository
import ucfs.claimant.consumer.utility.FunctionalUtility.encase

@Repository
class SecretsManagerSecretRepository(private val secretsManagerClient: SecretsManagerClient): SecretRepository {

    override fun secret(name: String): Either<Throwable, String> =
            encase {
                secretsManagerClient.getSecretValue(secretValueRequest())
            }.map(GetSecretValueResponse::secretString)

    private fun secretValueRequest(): GetSecretValueRequest =
        with(GetSecretValueRequest.builder()) {
            secretId("ucfs-claimant-api-db-password")
            build()
        }
}
