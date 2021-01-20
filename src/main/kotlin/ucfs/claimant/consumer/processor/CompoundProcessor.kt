package ucfs.claimant.consumer.processor

import ucfs.claimant.consumer.domain.FilterProcessingOutput
import ucfs.claimant.consumer.domain.JsonProcessingResult

interface CompoundProcessor {
    fun process(input: JsonProcessingResult): FilterProcessingOutput
}
