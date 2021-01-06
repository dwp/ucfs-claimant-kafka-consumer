package ucfs.claimant.consumer.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.ssm.SsmClient

@Configuration
@Profile("!LOCALSTACK")
class AwsConfiguration {
    @Bean
    fun ssmClient(): SsmClient = SsmClient.create()

    @Bean
    fun kmsClient(): KmsClient = KmsClient.create()

    @Bean
    fun secretsManagerClient(): SecretsManagerClient = SecretsManagerClient.create()
}
