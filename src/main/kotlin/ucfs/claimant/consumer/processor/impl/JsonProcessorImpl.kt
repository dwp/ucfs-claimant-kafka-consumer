package ucfs.claimant.consumer.processor.impl

import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.JsonProcessingOutput
import ucfs.claimant.consumer.domain.ValidationProcessingResult
import ucfs.claimant.consumer.processor.JsonProcessor
import ucfs.claimant.consumer.utility.FunctionalUtility
import ucfs.claimant.consumer.utility.GsonExtensions.jsonObject

@Component
class JsonProcessorImpl : JsonProcessor {
    override fun process(record: ValidationProcessingResult): JsonProcessingOutput =
            record.second.jsonObject().map {
                Pair(record.first, it)
            }.mapLeft { FunctionalUtility.processingFailure(record.first, it, "Failed to parse json") }
}
