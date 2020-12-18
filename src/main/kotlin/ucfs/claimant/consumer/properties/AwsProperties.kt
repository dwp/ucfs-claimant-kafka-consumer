package ucfs.claimant.consumer.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "aws")
data class AwsProperties(var saltParameterName: String = "/ucfs/claimant-api/nino/salt",
                         var cmkAlias: String = "alias/ucfs_etl_cmk") {
    @Bean
    fun saltParameterName() = saltParameterName

    @Bean
    fun cmkAlias() = cmkAlias
}
