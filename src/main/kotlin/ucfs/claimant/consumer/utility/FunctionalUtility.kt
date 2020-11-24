package ucfs.claimant.consumer.utility

import arrow.core.Either
import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.utility.LoggingExtensions.logFailedProcessingStep
import uk.gov.dwp.dataworks.logging.DataworksLogger

object FunctionalUtility {
    fun <T, O> T.encase(f: T.() -> O): Either<Throwable, O> =
            try {
                Either.right(this.f())
            } catch (e: Throwable) {
                logger.error("Failed to run method", e, "error" to "${e.message}", "method" to "$f")
                Either.left(e)
            }

    fun processingFailure(sourceRecord: SourceRecord, result: Any, message: String): SourceRecord {
        logger.logFailedProcessingStep(message, sourceRecord, result)
        return sourceRecord
    }

    private val logger = DataworksLogger.getLogger(FunctionalUtility::class)
}
