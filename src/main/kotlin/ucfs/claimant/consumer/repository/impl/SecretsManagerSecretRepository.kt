package ucfs.claimant.consumer.repository.impl

import io.prometheus.client.Counter
import io.prometheus.client.spring.web.PrometheusTimeMethod
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import ucfs.claimant.consumer.repository.SecretRepository

@Repository
class SecretsManagerSecretRepository(private val secretsManagerClient: SecretsManagerClient,
                                     private val secretRetries: Counter): SecretRepository {

    @PrometheusTimeMethod(name = "uckc_secrets", help = "Duration of secret manager requests")
    @Retryable(value = [Exception::class],
        maxAttemptsExpression = "\${secrets.retry.maxAttempts:5}",
        backoff = Backoff(delayExpression = "\${secrets.retry.delay:1000}",
            multiplierExpression = "\${secrets.retry.multiplier:2}"))
    override fun secret(name: String): String =
        try {
            secretsManagerClient.getSecretValue(secretValueRequest(name)).secretString()
        } catch (e: Exception) {
            secretRetries.inc()
            throw e
        }

    private fun secretValueRequest(secretName: String): GetSecretValueRequest =
        with(GetSecretValueRequest.builder()) {
            secretId(secretName)
            build()
        }
}
