package ucfs.claimant.consumer.repository.impl

import io.prometheus.client.Counter
import io.prometheus.client.spring.web.PrometheusTimeMethod
import org.springframework.cache.annotation.Cacheable
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.services.ssm.model.GetParameterRequest
import ucfs.claimant.consumer.repository.SaltRepository

@Repository
class SsmParameterStoreSaltRepository(private val ssmClient: SsmClient, private val saltParameterName: String,
                                      private val saltRetries: Counter): SaltRepository {

    @PrometheusTimeMethod(name = "uckc_salt", help = "Duration of parameter store requests for nino salt")
    @Cacheable("SALT_CACHE")
    @Retryable(value = [Exception::class],
        maxAttemptsExpression = "\${ssm.retry.maxAttempts:5}",
        backoff = Backoff(delayExpression = "\${ssm.retry.delay:1000}",
            multiplierExpression = "\${ssm.retry.multiplier:2}"))
    override fun salt(): String =
        try {
            ssmClient.getParameter(getParameterRequest()).run {
                parameter().value()
            }
        } catch (e: Exception) {
            saltRetries.inc()
            throw e
        }

    private fun getParameterRequest(): GetParameterRequest =
        with(GetParameterRequest.builder()) {
            name(saltParameterName)
            withDecryption(true)
            build()
        }
}
