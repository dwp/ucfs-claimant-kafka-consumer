package ucfs.claimant.consumer.configuration

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*
import kotlin.time.ExperimentalTime

@Configuration
@ExperimentalTime
class KafkaConfiguration(private val bootstrapServers: String,
                         private val consumerGroup: String,
                         private val useSsl: Boolean,
                         private val truststore: String,
                         private val truststorePassword: String,
                         private val keystore: String,
                         private val keystorePassword: String,
                         private val keyPassword: String,
                         private val maxPollRecords: Int,
                         private val maxPollIntervalMs: Int,
                         private val fetchMaxBytes: Int,
                         private val maxPartitionFetchBytes: Int) {

    @Bean
    fun consumer(): () -> KafkaConsumer<ByteArray, ByteArray> = {
        KafkaConsumer<ByteArray, ByteArray>(consumerProperties())
    }

    @Bean
    fun producer(): () -> KafkaProducer<ByteArray, ByteArray> = {
        KafkaProducer<ByteArray, ByteArray>(producerProperties())
    }

    fun producerProperties() =
            Properties().apply {
                put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer::class.java)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer::class.java)
                if (useSsl) {
                    addSslConfig()
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
                    addSslConfig()
                }
            }

    private fun Properties.addSslConfig() {
        put("security.protocol", "SSL")
        put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststore)
        put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword)
        put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystore)
        put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keystorePassword)
        put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, keyPassword)
    }
}
