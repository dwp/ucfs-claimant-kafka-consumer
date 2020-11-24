package ucfs.claimant.consumer.processor.impl

import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.JsonProcessingOutput
import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.processor.JsonProcessor
import ucfs.claimant.consumer.utility.JsonExtensions.jsonObject
import ucfs.claimant.consumer.utility.LoggingExtensions.logFailedProcessingStep
import uk.gov.dwp.dataworks.logging.DataworksLogger

@Component
class JsonProcessorImpl : JsonProcessor {
    override fun process(record: Pair<SourceRecord, ByteArray>): JsonProcessingOutput =
            record.second.jsonObject().map {
                Pair(record.first, it)
            }.mapLeft {
                logger.logFailedProcessingStep("Failed to parse json", record.first, it)
                record.first
            }

    companion object {
        private val logger = DataworksLogger.getLogger(JsonProcessorImpl::class)
    }

}
