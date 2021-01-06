package ucfs.claimant.consumer.processor.impl

import arrow.core.flatMap
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.JsonProcessingOutput
import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.processor.JsonProcessor
import ucfs.claimant.consumer.processor.PreProcessor
import ucfs.claimant.consumer.processor.SourceRecordProcessor
import ucfs.claimant.consumer.processor.ValidationProcessor

@Component
class PreProcessorImpl(private val sourceRecordProcessor: SourceRecordProcessor,
                       private val validationProcessor: ValidationProcessor,
                       private val jsonProcessor: JsonProcessor): PreProcessor {
    override fun process(input: SourceRecord): JsonProcessingOutput =
        sourceRecordProcessor.process(input)
            .flatMap(validationProcessor::process)
            .flatMap(jsonProcessor::process)
}
