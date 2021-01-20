package ucfs.claimant.consumer.processor.impl

import arrow.core.flatMap
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.FilterProcessingOutput
import ucfs.claimant.consumer.domain.JsonProcessingResult
import ucfs.claimant.consumer.processor.*

@Component
class CompoundProcessorImpl(private val extractionProcessor: ExtractionProcessor,
                            private val datakeyProcessor: DatakeyProcessor,
                            private val decryptionProcessor: DecryptionProcessor,
                            private val transformationProcessor: TransformationProcessor,
                            private val filterProcessor: FilterProcessor) : CompoundProcessor {
    override fun process(input: JsonProcessingResult): FilterProcessingOutput =
        extractionProcessor.process(input)
                    .flatMap(datakeyProcessor::process)
                    .flatMap(decryptionProcessor::process)
                    .flatMap(transformationProcessor::process)
                    .flatMap(filterProcessor::process)
}
