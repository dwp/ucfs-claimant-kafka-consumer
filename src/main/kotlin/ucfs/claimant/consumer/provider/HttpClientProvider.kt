package ucfs.claimant.consumer.provider

import org.apache.http.impl.client.CloseableHttpClient

interface HttpClientProvider {
    fun client(): CloseableHttpClient
}
