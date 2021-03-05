package ucfs.claimant.consumer.configuration

import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import io.prometheus.client.exporter.PushGateway
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

@Configuration
class MetricsConfiguration(private val pushgatewayHost: String, private val pushgatewayPort: Int) {

    @Bean
    fun pushGateway(): PushGateway = PushGateway("$pushgatewayHost:$pushgatewayPort")

    @Bean
    fun insertedRecords() = counter("uckc_inserted_records", "Count of inserted records", "topic")

    @Bean
    fun updatedRecords() = counter("uckc_updated_records", "Count of updated records", "topic")

    @Bean
    fun deletedRecords() = counter("uckc_deleted_records", "Count of deleted records", "topic")

    @Bean
    fun failedRecords() = counter("uckc_failed_records", "Count of failed records", "topic")

    @Bean
    fun dksDecryptRetries() = counter("uckc_dks_decrypt_retries", "Count of retried dks calls")

    @Bean
    fun dksDecryptFailures() = counter("uckc_dks_decrypt_failures", "Count of failed dks calls")

    @Bean
    fun saltFailures() = counter("uckc_salt_failures", "Count of failed salt requests")

    @Bean
    fun saltRetries() = counter("uckc_salt_retries", "Count of retried salt requests")

    @Bean
    fun secretRetries() = counter("uckc_secret_retries", "Count of retried secret manager requests")

    @Bean
    fun secretFailures() = counter("uckc_secret_failures", "Count of failed secret manager requests")

    @Bean
    fun kmsRetries() = counter("uckc_kms_retries", "Count of retried kms requests")

    @Bean
    fun kmsFailures() = counter("uckc_kms_failures", "Count of failed kms requests")

    @Bean
    fun lagGauge() = gauge("uckc_topic_partition_lags",
        "The processing lag on each topic/partition", "topic", "partition")


    private fun gauge(name: String, help: String, vararg labels: String): Gauge =
        with(Gauge.build()) {
            name(name)
            labelNames(*labels)
            help(help)
            register()
        }

    private fun counter(name: String, help: String, vararg labels: String): Counter =
        with(Counter.build()) {
            name(name)
            labelNames(*labels)
            help(help)
            register()
        }

    @PostConstruct
    fun init() {
        Metrics.globalRegistry.add(PrometheusMeterRegistry(PrometheusConfig.DEFAULT, CollectorRegistry.defaultRegistry, Clock.SYSTEM))
    }
}
