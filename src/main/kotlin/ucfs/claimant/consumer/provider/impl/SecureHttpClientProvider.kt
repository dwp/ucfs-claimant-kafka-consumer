package ucfs.claimant.consumer.provider.impl

import org.apache.http.client.config.RequestConfig
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.provider.HttpClientProvider
import java.io.File
import javax.net.ssl.SSLContext


@Component
class SecureHttpClientProvider(private val keystore: String,
                               private val keystorePassword: String,
                               private val keystoreAlias: String,
                               private val keyPassword: String,
                               private val truststore: String,
                               private val truststorePassword: String) : HttpClientProvider {

    override fun client(): CloseableHttpClient =
            HttpClients.custom().run {
                setDefaultRequestConfig(requestConfig())
                setSSLSocketFactory(connectionFactory())
                build()
            }


    private fun requestConfig(): RequestConfig =
            RequestConfig.custom().run {
                setConnectTimeout(5_000)
                setConnectionRequestTimeout(5_000)
                build()
            }


    private fun connectionFactory() = SSLConnectionSocketFactory(
            sslContext(),
            arrayOf("TLSv1.2"),
            null,
            SSLConnectionSocketFactory.getDefaultHostnameVerifier())

    private fun sslContext(): SSLContext =
            SSLContexts.custom().run {
                loadKeyMaterial(File(keystore), keystorePassword.toCharArray(), keyPassword.toCharArray()) { _, _ -> keystoreAlias }
                loadTrustMaterial(File(truststore), truststorePassword.toCharArray())
                build()
            }
}
