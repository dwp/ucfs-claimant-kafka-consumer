package ucfs.claimant.consumer.service.impl

import arrow.core.Either
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import org.springframework.cache.annotation.Cacheable
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import ucfs.claimant.consumer.domain.DataKeyServiceResult
import ucfs.claimant.consumer.domain.DatakeyServiceResult
import ucfs.claimant.consumer.exception.DataKeyServiceUnavailableException
import ucfs.claimant.consumer.provider.HttpClientProvider
import ucfs.claimant.consumer.service.DatakeyService
import ucfs.claimant.consumer.utility.GsonExtensions.jsonObject
import uk.gov.dwp.dataworks.logging.DataworksLogger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URLEncoder
import java.util.*

@Service
class DatakeyServiceImpl(private val httpClientProvider: HttpClientProvider,
                         private val url: String) : DatakeyService {

    @Retryable(value = [Exception::class],
            maxAttemptsExpression = "\${dks.retry.maxAttempts:5}",
            backoff = Backoff(delayExpression = "\${dks.retry.delay:1000}",
                                multiplierExpression = "\${dks.retry.multiplier:2}"))
    @Cacheable("DECRYPTED_KEY_CACHE")
    override fun decryptKey(encryptingKeyId: String, encryptedKey: String): DatakeyServiceResult {
        val correlationId = UUID.randomUUID().toString()
        httpClientProvider.client().use { client ->
            val dksUrl = resourceUrl(encryptingKeyId, correlationId)
            val httpPost = HttpPost(dksUrl)
            httpPost.entity = StringEntity(encryptedKey, ContentType.TEXT_PLAIN)
            client.execute(httpPost).use { response ->
                return when (response.statusLine.statusCode) {
                    200 -> {
                        val entity = response.entity
                        val text = BufferedReader(InputStreamReader(entity.content)).use(BufferedReader::readText)
                        EntityUtils.consume(entity)
                        text.jsonObject(DataKeyServiceResult::class.java).map(DataKeyServiceResult::plaintextDataKey)
                    }
                    400 -> {
                        logger.error("DKS key decryption error", "status_code" to "${response.statusLine.statusCode}",
                                "encrypted_key" to encryptedKey, "encrypting_key_id" to encryptingKeyId,
                                "dks_url" to dksUrl, "correlation_id" to correlationId)
                        Either.Left(Pair(response.statusLine.statusCode, Pair(encryptingKeyId, encryptedKey)))
                    }
                    else -> {
                        throw DataKeyServiceUnavailableException("Request to data key service returned ${response.statusLine.statusCode}")
                    }
                }
            }
        }
    }

    private fun resourceUrl(encryptionKeyId: String, dksCorrelationId: String): String =
            "$url/datakey/actions/decrypt?keyId=${URLEncoder.encode(encryptionKeyId, "US-ASCII")}&correlationId=$dksCorrelationId"

    companion object {
        val logger = DataworksLogger.getLogger(DatakeyServiceImpl::class)
    }
}
