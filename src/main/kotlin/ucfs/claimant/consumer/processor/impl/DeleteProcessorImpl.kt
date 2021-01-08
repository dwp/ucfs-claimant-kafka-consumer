package ucfs.claimant.consumer.processor.impl

import arrow.core.flatMap
import arrow.core.rightIfNotNull
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.DeleteProcessingOutput
import ucfs.claimant.consumer.domain.JsonProcessingResult
import ucfs.claimant.consumer.processor.DeleteProcessor
import ucfs.claimant.consumer.utility.ExtractionUtility.id
import ucfs.claimant.consumer.utility.FunctionalUtility

@Component
class DeleteProcessorImpl(@Qualifier("idSourceFields") private val idSourceFields: Map<String, String>): DeleteProcessor {
    override fun process(record: JsonProcessingResult): DeleteProcessingOutput =
        idSourceFields[record.first.topic()].rightIfNotNull {
            "No source id configured for topic '${record.first.topic()}'."
        }.flatMap { sourceId ->
            val (json) = record.second
            json.id(sourceId)
        }.map { sourceId ->
            Pair(record.first, sourceId)
        }.mapLeft {
            FunctionalUtility.processingFailure(record.first, it, "Failed to extract source id")
        }
}

