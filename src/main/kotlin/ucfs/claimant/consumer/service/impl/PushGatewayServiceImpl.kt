package ucfs.claimant.consumer.service.impl

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.PushGateway
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor
import org.springframework.scheduling.config.ScheduledTask
import org.springframework.stereotype.Service
import ucfs.claimant.consumer.service.PushGatewayService
import uk.gov.dwp.dataworks.logging.DataworksLogger

@Service
class PushGatewayServiceImpl(private val pushGateway: PushGateway,
                             private val instanceName: String,
                             private val scrapeInterval: Int,
                             private val deleteMetrics: Boolean,
                             private val postProcessor: ScheduledAnnotationBeanPostProcessor): PushGatewayService {

    @Scheduled(fixedRateString = "\${metrics.pushRate:20000}", initialDelayString = "\${metrics.initialDelay:10000}")
    override fun pushMetrics() {
        logger.debug("Pushing metrics", *metricsGroupingKeyPairs())
        pushGateway.push(CollectorRegistry.defaultRegistry, "ucfs-claimant-kafka-consumer", metricsGroupingKey())
        logger.debug("Pushed metrics", *metricsGroupingKeyPairs())
    }

    override fun pushFinalMetrics() {
        logger.info("Canceling scheduled task", *metricsGroupingKeyPairs())
        postProcessor.scheduledTasks.forEach(ScheduledTask::cancel)
        logger.info("Pushing final set of metrics", *metricsGroupingKeyPairs())
        pushMetrics()
        if (deleteMetrics) {
            Thread.sleep(scrapeInterval.toLong())
            deleteMetrics()
        }
        logger.info("Pushed final set of metrics", *metricsGroupingKeyPairs())
    }

    override fun deleteMetrics() {
        logger.info("Waiting for metric collection before deletion",
            "scrape_interval" to "$scrapeInterval", *metricsGroupingKeyPairs())
        pushGateway.delete("ucfs-claimant-kafka-consumer", metricsGroupingKey())
        logger.info("Deleted metrics", "scrape_interval" to "$scrapeInterval", *metricsGroupingKeyPairs())
    }

    private fun metricsGroupingKeyPairs(): Array<Pair<String, String>> =
        metricsGroupingKey().entries.map { (k, v) -> Pair(k, v) }.toTypedArray()

    private fun metricsGroupingKey(): Map<String, String> = mapOf("instance" to instanceName)

    companion object {
        private val logger = DataworksLogger.getLogger(PushGatewayServiceImpl::class)
    }
}

