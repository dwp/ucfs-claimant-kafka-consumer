package ucfs.claimant.consumer.processor.impl

import arrow.core.flatMap
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.DecryptionProcessingOutput
import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.processor.*

@Component
class CompoundProcessorImpl(private val sourceRecordProcessor: SourceRecordProcessor,
                            private val jsonProcessor: JsonProcessor,
                            private val extractionProcessor: ExtractionProcessor,
                            private val datakeyProcessor: DatakeyProcessor,
                            private val decryptionProcessor: DecryptionProcessor) : CompoundProcessor {

    override fun process(input: SourceRecord): DecryptionProcessingOutput =
            sourceRecordProcessor.process(input)
                    .flatMap(jsonProcessor::process)
                    .flatMap(extractionProcessor::process)
                    .flatMap(datakeyProcessor::process)
                    .flatMap(decryptionProcessor::process)
}
