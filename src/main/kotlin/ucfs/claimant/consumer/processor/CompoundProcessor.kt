package ucfs.claimant.consumer.processor

import ucfs.claimant.consumer.domain.FilterProcessingOutput
import ucfs.claimant.consumer.domain.JsonProcessingResult
import ucfs.claimant.consumer.domain.TransformationProcessingOutput

interface CompoundProcessor {
    fun process(input: JsonProcessingResult): FilterProcessingOutput
}
