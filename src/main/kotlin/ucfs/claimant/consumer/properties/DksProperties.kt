package ucfs.claimant.consumer.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "dks")
data class DksProperties(var url: String = "https://dks:8443") {

    @Bean
    fun url() = url
}
