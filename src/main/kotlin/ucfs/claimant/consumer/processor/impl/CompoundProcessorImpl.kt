package ucfs.claimant.consumer.processor.impl

import arrow.core.flatMap
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.domain.TransformationProcessingOutput
import ucfs.claimant.consumer.processor.*

@Component
class CompoundProcessorImpl(private val sourceRecordProcessor: SourceRecordProcessor,
                            private val jsonProcessor: JsonProcessor,
                            private val validationProcessor: ValidationProcessor,
                            private val extractionProcessor: ExtractionProcessor,
                            private val datakeyProcessor: DatakeyProcessor,
                            private val decryptionProcessor: DecryptionProcessor,
                            private val transformationProcessor: TransformationProcessor) : CompoundProcessor {
    override fun process(input: SourceRecord): TransformationProcessingOutput =
            sourceRecordProcessor.process(input)
                    .flatMap(validationProcessor::process)
                    .flatMap(jsonProcessor::process)
                    .flatMap(extractionProcessor::process)
                    .flatMap(datakeyProcessor::process)
                    .flatMap(decryptionProcessor::process)
                    .flatMap(transformationProcessor::process)

}
