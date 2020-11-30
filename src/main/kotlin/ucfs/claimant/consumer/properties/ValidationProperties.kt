package ucfs.claimant.consumer.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "validation")
data class ValidationProperties(var schemaLocation: String = "/message.schema.json") {

    @Bean
    fun schemaLocation() = schemaLocation
}
