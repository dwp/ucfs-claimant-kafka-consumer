package ucfs.claimant.consumer

import io.prometheus.client.spring.web.EnablePrometheusTiming
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling
import ucfs.claimant.consumer.orchestrate.Orchestrator
import ucfs.claimant.consumer.service.MetricsService
import uk.gov.dwp.dataworks.logging.DataworksLogger
import kotlin.system.exitProcess
import kotlin.time.ExperimentalTime

@SpringBootApplication
@EnableCaching
@EnableRetry
@EnableScheduling
@EnablePrometheusTiming
class UcfsClaimantConsumer(
    private val orchestrator: Orchestrator,
    private val metricsService: MetricsService,
    private val runningApplicationsGauge: Gauge,
) : CommandLineRunner {

    @ExperimentalTime
    override fun run(vararg args: String?) {
        val log = DataworksLogger.getLogger("run")
        metricsService.startMetricsEndpoint()

        log.info("Incrementing running applications metric count")
        runningApplicationsGauge.inc()

        try {
            orchestrator.orchestrate()
        } finally {
            log.info("Decrementing running applications metric count")
            runningApplicationsGauge.dec()
        }
    }
}

fun main(args: Array<String>) {
    val log = DataworksLogger.getLogger("main")
    try {
        runApplication<UcfsClaimantConsumer>(*args)
    } catch (e: Exception) {
        log.error("Application error", e)
        exitProcess(-1)
    }

}
