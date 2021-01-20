package ucfs.claimant.consumer.processor.impl

import arrow.core.Either
import arrow.core.right
import org.springframework.stereotype.Component
import ucfs.claimant.consumer.domain.FilterProcessingOutput
import ucfs.claimant.consumer.domain.FilterResult
import ucfs.claimant.consumer.domain.TransformationProcessingResult
import ucfs.claimant.consumer.processor.FilterProcessor
import ucfs.claimant.consumer.utility.FunctionalUtility
import ucfs.claimant.consumer.utility.GsonExtensions.jsonObject
import ucfs.claimant.consumer.utility.GsonExtensions.nullableString

@Component
class FilterProcessorImpl(private val claimantTopic: String): FilterProcessor {
    override fun process(record: TransformationProcessingResult): FilterProcessingOutput =
        filter(record.first.topic(), record.second.transformedDbObject).map {
            Pair(record.first, FilterResult(record.second, it))
        }.mapLeft {
            FunctionalUtility.processingFailure(record.first, it,"Failed to perform filtering on transformed object from '${record.first.topic()}'.")
        }

    private fun filter(topic: String, transformed: String): Either<Any, Boolean> =
        if (topic == claimantTopic) {
            transformed.jsonObject().map {
                it.nullableString("nino")?.run(String::isNotBlank) ?: false
            }
        }
        else {
            true.right()
        }
}
