package ucfs.claimant.consumer.utility

import arrow.core.Either
import uk.gov.dwp.dataworks.logging.DataworksLogger

object FunctionalUtility {
    fun <T, O> T.encase(f: T.() -> O): Either<Throwable, O> =
            try {
                Either.right(this.f())
            } catch (e: Throwable) {
                logger.error("Failed to run method", e, "error" to "${e.message}", "method" to "$f")
                Either.left(e)
            }

    private val logger = DataworksLogger.getLogger(FunctionalUtility::class)
}
