package ucfs.claimant.consumer.target.impl

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.TransformationProcessingResult
import ucfs.claimant.consumer.target.SuccessTarget
import uk.gov.dwp.dataworks.logging.DataworksLogger

@Component
@Profile("!QUEUE_TARGET")
class ConsoleTarget : SuccessTarget {

    override suspend fun send(topic: String, records: List<TransformationProcessingResult>) {
        records.forEach { (record, result) ->
            logger.info(
                "Got result", "result" to "$result",
                "source_topic" to record.topic(), "source_key" to "${record.key()}",
                "source_value" to "${record.value()}"
            )
        }
    }

    companion object {
        private val logger = DataworksLogger.getLogger(ConsoleTarget::class)
    }
}
