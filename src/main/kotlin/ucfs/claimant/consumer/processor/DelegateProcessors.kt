package ucfs.claimant.consumer.processor

import ucfs.claimant.consumer.domain.*

interface DelegateProcessor<I, O> {
    fun process(record: I): O
}

interface SourceRecordProcessor : DelegateProcessor<SourceRecord, QueueRecordProcessingOutput>
interface JsonProcessor : DelegateProcessor<SourceRecordProcessingResult, JsonProcessingOutput>
interface ExtractionProcessor : DelegateProcessor<JsonProcessingResult, ExtractionProcessingOutput>
interface DatakeyProcessor : DelegateProcessor<ExtractionProcessingResult, DatakeyProcessingOutput>
interface DecryptionProcessor : DelegateProcessor<DatakeyProcessingResult, DecryptionProcessingOutput>
