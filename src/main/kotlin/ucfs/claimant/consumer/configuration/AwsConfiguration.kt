package ucfs.claimant.consumer.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.ssm.SsmClient
import uk.gov.dwp.dataworks.logging.DataworksLogger

@Configuration
@Profile("!LOCALSTACK")
class AwsConfiguration(private val kmsRegion: String) {
    @Bean
    fun ssmClient(): SsmClient = SsmClient.create()

    @Bean
    fun kmsClient(): KmsClient =
        with (KmsClient.builder()) {
            if (kmsRegion.isNotBlank()) {
                logger.info("Setting KMS region",
                    "kms_region" to kmsRegion, "resolved_region" to "${Region.of(kmsRegion)}")
                region(Region.of(kmsRegion))
            } else {
                logger.info("Using default kms region",
                    "kms_region" to kmsRegion)
            }
            build()
        }

    @Bean
    fun secretsManagerClient(): SecretsManagerClient = SecretsManagerClient.create()

    companion object {
        private val logger = DataworksLogger.getLogger(AwsConfiguration::class)
    }
}
