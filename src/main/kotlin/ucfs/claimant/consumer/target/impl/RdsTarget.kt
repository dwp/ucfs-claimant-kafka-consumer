package ucfs.claimant.consumer.target.impl

import ucfs.claimant.consumer.domain.TransformationProcessingResult
import ucfs.claimant.consumer.target.SuccessTarget
import javax.sql.DataSource

class RdsTarget(private val dataSource: DataSource): SuccessTarget {
    override suspend fun send(topic: String, records: List<TransformationProcessingResult>) {

    }
}
