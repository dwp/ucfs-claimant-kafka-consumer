package ucfs.claimant.consumer.repository.impl

import arrow.core.left
import io.prometheus.client.Counter
import io.prometheus.client.spring.web.PrometheusTimeMethod
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import org.springframework.cache.annotation.Cacheable
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Repository
import ucfs.claimant.consumer.domain.DataKeyDecryptionServiceData
import ucfs.claimant.consumer.domain.DataKeyServiceResponse
import ucfs.claimant.consumer.exception.DataKeyServiceUnavailableException
import ucfs.claimant.consumer.provider.HttpClientProvider
import ucfs.claimant.consumer.repository.DecryptingDataKeyRepository
import ucfs.claimant.consumer.utility.GsonExtensions.jsonObject
import uk.gov.dwp.dataworks.logging.DataworksLogger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URLEncoder
import java.util.*

@Repository
class DecryptingDataKeyRepositoryImpl(private val httpClientProvider: HttpClientProvider,
                                      private val dksDecryptRetries: Counter,
                                      private val url: String) : DecryptingDataKeyRepository {

    @PrometheusTimeMethod(name = "uckc_decrypt_datakey", help = "Duration of DKS decrypt key requests")
    @Retryable(value = [Exception::class],
            maxAttemptsExpression = "\${dks.retry.maxAttempts:5}",
            backoff = Backoff(delayExpression = "\${dks.retry.delay:1000}",
                                multiplierExpression = "\${dks.retry.multiplier:2}"))
    @Cacheable("DECRYPTED_KEY_CACHE")
    override fun decryptDataKey(encryptingKeyId: String, encryptedKey: String): DataKeyServiceResponse {
        val correlationId = UUID.randomUUID().toString()
        httpClientProvider.client().use { client ->
            val dksUrl = resourceUrl(encryptingKeyId, correlationId)
            val httpPost = HttpPost(dksUrl).apply {
                entity = StringEntity(encryptedKey, ContentType.TEXT_PLAIN)
            }
            client.execute(httpPost).use { response ->
                return when (response.statusLine.statusCode) {
                    200 -> {
                        val entity = response.entity
                        val text = BufferedReader(InputStreamReader(entity.content)).use(BufferedReader::readText)
                        EntityUtils.consume(entity)
                        text.jsonObject(DataKeyDecryptionServiceData::class.java).map(DataKeyDecryptionServiceData::plaintextDataKey)
                    }
                    400 -> {
                        logger.error("DKS key decryption error", "status_code" to "${response.statusLine.statusCode}",
                                "encrypted_key" to encryptedKey, "encrypting_key_id" to encryptingKeyId, "correlation_id" to correlationId)
                        Pair(response.statusLine.statusCode, Pair(encryptingKeyId, encryptedKey)).left()
                    }
                    else -> {
                        logger.error("DKS service error", "status_code" to "${response.statusLine.statusCode}",
                            "dks_url" to dksUrl, "correlation_id" to correlationId,
                            "encrypted_key" to encryptedKey, "encrypting_key_id" to encryptingKeyId)
                        dksDecryptRetries.inc()
                        throw DataKeyServiceUnavailableException("Request to data key service $correlationId returned ${response.statusLine.statusCode}")
                    }
                }
            }
        }
    }

    private fun resourceUrl(encryptionKeyId: String, dksCorrelationId: String): String =
            "$url/datakey/actions/decrypt?keyId=${URLEncoder.encode(encryptionKeyId, "US-ASCII")}&correlationId=$dksCorrelationId"

    companion object {
        val logger = DataworksLogger.getLogger(DecryptingDataKeyRepositoryImpl::class)
    }
}
