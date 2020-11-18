package ucfs.claimant.consumer.target

import ucfs.claimant.consumer.domain.SourceRecord

interface FailureTarget {
    fun send(records: List<SourceRecord>)
}
