package ucfs.claimant.consumer.target

import ucfs.claimant.consumer.domain.DeleteProcessingResult
import ucfs.claimant.consumer.domain.TransformationProcessingResult

interface SuccessTarget {
    suspend fun upsert(topic: String, records: List<TransformationProcessingResult>)
    suspend fun delete(topic: String, records: List<DeleteProcessingResult>)
}
