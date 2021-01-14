package ucfs.claimant.consumer.utility

import ucfs.claimant.consumer.domain.SourceRecord
import uk.gov.dwp.dataworks.logging.DataworksLogger

object LoggingExtensions {

    fun DataworksLogger.logFailedProcessingStep(description: String, record: SourceRecord, result: Any) {
        logFailedRecord(description, record)
        logThrowableOrAny(description, result)
    }

    private fun DataworksLogger.logFailedRecord(description: String, record: SourceRecord) {
        error(
            "Failed record", "description" to description,
            "key" to String(record.key()), "topic" to record.topic(),
            "partition" to "${record.partition()}", "offset" to "${record.offset()}",
            "timestamp" to "${record.timestamp()}"
        )
    }

    private fun DataworksLogger.logThrowableOrAny(description: String, result: Any) {
        when (result) {
            is Throwable -> {
                error("Failure result", result, "description" to description, "message" to "${result.message}")
            }
            is Pair<*, *> -> {
                error("Failure result", "description" to description, "result" to "${result.second}")
            }
            else -> {
                error("Failure result", "description" to description, "result" to "$result")
            }
        }
    }
}
