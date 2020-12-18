package ucfs.claimant.consumer.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "cipher")
data class CipherProperties(var dataKeySpec: String = "AES_256",
                            var decryptingTransformation: String = "AES/CTR/NoPadding",
                            var encryptingTransformation: String = "AES/GCM/NoPadding",
                            var decryptingAlgorithm: String = "AES",
                            var encryptingAlgorithm: String = "AES",
                            var decryptingProvider: String = "BC",
                            var encryptingProvider: String = "BC",
                            var maxKeyUsage: Int = 10_000,
                            var initialisationVectorSize: Int = 12) {

    @Bean
    fun dataKeySpec() = dataKeySpec

    @Bean
    fun encryptingTransformation() = encryptingTransformation

    @Bean
    fun decryptingTransformation() = decryptingTransformation

    @Bean
    fun encryptingAlgorithm() = encryptingAlgorithm

    @Bean
    fun decryptingAlgorithm() = decryptingAlgorithm

    @Bean
    fun encryptingProvider() = encryptingProvider

    @Bean
    fun decryptingProvider() = decryptingProvider

    @Bean
    fun maxKeyUsage() = maxKeyUsage

    @Bean
    fun initialisationVectorSize() = initialisationVectorSize
}
