package ucfs.claimant.consumer.processor

import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.domain.TransformationProcessingOutput

interface CompoundProcessor {
    fun process(input: SourceRecord): TransformationProcessingOutput
}
