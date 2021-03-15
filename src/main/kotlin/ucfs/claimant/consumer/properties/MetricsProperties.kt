package ucfs.claimant.consumer.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.time.ExperimentalTime

@Configuration
@ConfigurationProperties(prefix = "metrics")
@ExperimentalTime
class MetricsProperties(var pushgatewayHost: String = "pushgateway",
                        var pushgatewayPort: Int = 9091,
                        var instanceName: String = "uckc",
                        var scrapeInterval: Int = 70_000,
                        var deleteMetrics: Boolean = true) {

    @Bean
    fun pushgatewayHost() = pushgatewayHost

    @Bean
    fun pushgatewayPort() = pushgatewayPort

    @Bean
    fun instanceName() = instanceName

    @Bean
    fun scrapeInterval() = scrapeInterval

    @Bean
    fun deleteMetrics() = deleteMetrics
}
