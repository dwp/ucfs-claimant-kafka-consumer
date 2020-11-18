package ucfs.claimant.consumer.target

import ucfs.claimant.consumer.domain.DecryptionResult
import ucfs.claimant.consumer.domain.SourceRecord

interface SuccessTarget {
    suspend fun send(topic: String, records: List<Pair<SourceRecord, DecryptionResult>>)
}
