package ucfs.claimant.consumer.processor.impl

import arrow.core.flatMap
import arrow.core.rightIfNotNull
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.DeleteProcessingOutput
import ucfs.claimant.consumer.domain.JsonProcessingResult
import ucfs.claimant.consumer.processor.DeleteProcessor
import ucfs.claimant.consumer.utility.FunctionalUtility
import ucfs.claimant.consumer.utility.GsonExtensions.string

@Component
class DeleteProcessorImpl(@Qualifier("naturalIdFields") private val naturalIdFields: Map<String, String>): DeleteProcessor {
    override fun process(record: JsonProcessingResult): DeleteProcessingOutput =
        naturalIdFields[record.first.topic()].rightIfNotNull {
            "No natural id configured for topic '${record.first.topic()}'."
        }.flatMap { naturalIdField ->
            val (json) = record.second
            json.string("message", "_id", naturalIdField)
        }.map { naturalId ->
            Pair(record.first, naturalId)
        }.mapLeft {
            FunctionalUtility.processingFailure(record.first, it, "Failed to extract natural id")
        }
}

