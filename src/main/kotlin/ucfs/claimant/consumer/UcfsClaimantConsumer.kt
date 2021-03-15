package ucfs.claimant.consumer

import io.prometheus.client.spring.web.EnablePrometheusTiming
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling
import ucfs.claimant.consumer.orchestrate.Orchestrator
import ucfs.claimant.consumer.service.MetricsService
import kotlin.time.ExperimentalTime

@SpringBootApplication
@EnableCaching
@EnableRetry
@EnableScheduling
@EnablePrometheusTiming
class UcfsClaimantConsumer(private val orchestrator: Orchestrator,
                           private val metricsService: MetricsService): CommandLineRunner {

    @ExperimentalTime
    override fun run(vararg args: String?) {
        metricsService.startMetricsEndpoint()
        orchestrator.orchestrate()
    }
}

fun main(args: Array<String>) {
    runApplication<UcfsClaimantConsumer>(*args)
}
