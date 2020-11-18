package ucfs.claimant.consumer.configuration

import kotlinx.coroutines.delay
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.PartitionInfo
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ucfs.claimant.consumer.service.impl.KafkaConsumerService
import uk.gov.dwp.dataworks.logging.DataworksLogger
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@Configuration
@ExperimentalTime
class KafkaConfiguration(private val bootstrapServers: String,
                         private val consumerGroup: String,
                         private val useSsl: Boolean,
                         private val trustStore: String,
                         private val trustStorePassword: String,
                         private val keyStore: String,
                         private val keyStorePassword: String,
                         private val keyPassword: String,
                         private val topicRegex: Regex,
                         private val maxPollRecords: Int,
                         private val maxPollIntervalMs: Int,
                         private val fetchMaxBytes: Int,
                         private val maxPartitionFetchBytes: Int) {

    @Bean
    fun consumer(): suspend () -> KafkaConsumer<ByteArray, ByteArray> = suspend {
        KafkaConsumer<ByteArray, ByteArray>(consumerProperties()).apply {
            subscribe(topicRegex)
        }
    }

    fun consumerProperties() =
            Properties().apply {
                put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup)
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer::class.java)
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer::class.java)
                put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords)
                put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs)
                put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, fetchMaxBytes)
                put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, maxPartitionFetchBytes)

                if (useSsl) {
                    put("security.protocol", "SSL")
                    put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, trustStore)
                    put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, trustStorePassword)
                    put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keyStore)
                    put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keyStorePassword)
                    put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, keyPassword)
                }
            }

    @ExperimentalTime
    private final tailrec suspend infix fun <K, V> KafkaConsumer<K, V>.subscribe(includesRegex: Regex) {
        val currentSubscription = subscription()
        val topics =  includedTopics(includesRegex)

        if (topics.toSet() != currentSubscription && topics.isNotEmpty()) {
            topics.minus(currentSubscription).forEach {
                logger.info("New topic found", "topic" to it)
            }
            subscribe(topics)
            return
        }

        if (currentSubscription.isNotEmpty()) {
            return
        }

        logger.info("No topics and no current subscription, trying again")
        delay(5.seconds)
        subscribe(includesRegex)
    }

    private fun <K, V> KafkaConsumer<K, V>.includedTopics(inclusionRegex: Regex): List<String> =
            listTopics()
                .map(Map.Entry<String, List<PartitionInfo>>::key)
                .filter(inclusionRegex::matches)

    companion object {
        private val logger = DataworksLogger.getLogger("${KafkaConsumerService::class.java}")
    }

}
