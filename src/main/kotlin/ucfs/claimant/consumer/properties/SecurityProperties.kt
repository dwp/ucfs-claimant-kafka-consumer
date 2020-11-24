package ucfs.claimant.consumer.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "security")
data class SecurityProperties(var keystore: String = "",
                              var keystorePassword: String = "",
                              var keystoreAlias: String = "",
                              var keyPassword: String = "",
                              var truststore: String = "",
                              var truststorePassword: String = "") {
    @Bean
    fun keystore() = keystore

    @Bean
    fun keystorePassword() = keystorePassword

    @Bean
    fun keystoreAlias() = keystoreAlias

    @Bean
    fun keyPassword() = keyPassword

    @Bean
    fun truststore() = truststore

    @Bean
    fun truststorePassword() = truststorePassword

}
