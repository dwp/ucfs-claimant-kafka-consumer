package ucfs.claimant.consumer.service.impl

import io.prometheus.client.exporter.HTTPServer
import org.springframework.stereotype.Service
import ucfs.claimant.consumer.service.MetricsService
import uk.gov.dwp.dataworks.logging.DataworksLogger

@Service
class PrometheusMetricsService: MetricsService {

    override fun startMetricsEndpoint() {
        server.run {
            logger.info("Started metrics endpoint")
        }
    }

    override fun stopMetricsEndpoint() {
        logger.info("Stopping metrics endpoint")
        server.stop()
    }


    private val server: HTTPServer by lazy {
        HTTPServer(8080)
    }

    companion object {
        private val logger = DataworksLogger.getLogger(PrometheusMetricsService::class)
    }
}
