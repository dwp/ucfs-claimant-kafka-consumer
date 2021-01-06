package ucfs.claimant.consumer.processor

import ucfs.claimant.consumer.domain.JsonProcessingOutput
import ucfs.claimant.consumer.domain.SourceRecord

interface PreProcessor {
    fun process(input: SourceRecord): JsonProcessingOutput
}
