package ucfs.claimant.consumer.target

import ucfs.claimant.consumer.domain.TransformationProcessingResult

interface SuccessTarget {
    suspend fun send(topic: String, records: List<TransformationProcessingResult>)
}
