package ucfs.claimant.consumer.processor

import ucfs.claimant.consumer.domain.DecryptionProcessingOutput
import ucfs.claimant.consumer.domain.SourceRecord

interface CompoundProcessor {
    fun process(input: SourceRecord): DecryptionProcessingOutput
}
