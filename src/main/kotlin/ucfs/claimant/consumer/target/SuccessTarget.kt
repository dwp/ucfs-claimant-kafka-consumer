package ucfs.claimant.consumer.target

import ucfs.claimant.consumer.domain.FilterProcessingResult
import ucfs.claimant.consumer.domain.JsonProcessingResult

interface SuccessTarget {
    suspend fun upsert(topic: String, records: List<FilterProcessingResult>)
    suspend fun delete(topic: String, records: List<JsonProcessingResult>)
}
