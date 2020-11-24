package ucfs.claimant.consumer.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.time.ExperimentalTime
import kotlin.time.minutes
import kotlin.time.seconds
import kotlin.time.toJavaDuration

@Configuration
@ConfigurationProperties(prefix = "kafka")
@ExperimentalTime
data class KafkaProperties(var bootstrapServers: String = "kafka:9092",
                           var useSsl: Boolean = false,
                           var consumerGroup: String = "ucfs-claimant-consumers",
                           var topicRegex: String = "",
                           var pollDurationSeconds: Int = 10,
                           var maxPollRecords: Int = 5000,
                           var maxPollIntervalMs: Int = 5.minutes.inMilliseconds.toInt(),
                           var fetchMaxBytes: Int = 1024 * 1024,
                           var maxPartitionFetchBytes: Int = 1024 * 1024,
                           var dlqTopic: String = "dead.letter.queue") {

    @Bean
    fun bootstrapServers() = bootstrapServers

    @Bean
    fun consumerGroup() = consumerGroup

    @Bean
    fun topicRegex() = Regex(topicRegex)

    @ExperimentalTime
    @Bean
    fun pollDuration() = pollDurationSeconds.seconds.toJavaDuration()

    @Bean
    fun maxPollRecords() = maxPollRecords

    @Bean
    fun maxPollIntervalMs() = maxPollIntervalMs

    @Bean
    fun fetchMaxBytes() = fetchMaxBytes

    @Bean
    fun maxPartitionFetchBytes() = maxPartitionFetchBytes

    @Bean
    fun useSsl() = useSsl

    @Bean
    fun dlqTopic() = dlqTopic
}
