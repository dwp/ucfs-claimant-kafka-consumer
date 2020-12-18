package ucfs.claimant.consumer.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "topic")
data class SourceTopicProperties(var claimant: String = "db.core.claimant",
                                 var contract: String = "db.core.contract",
                                 var statement: String = "db.core.statement")  {

    @Bean
    fun claimantSourceTopic() = claimant

    @Bean
    fun contractSourceTopic() = contract

    @Bean
    fun statementSourceTopic() = statement
}
