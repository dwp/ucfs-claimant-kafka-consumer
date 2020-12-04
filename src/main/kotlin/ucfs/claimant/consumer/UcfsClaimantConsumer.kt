package ucfs.claimant.consumer

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.retry.annotation.EnableRetry
import ucfs.claimant.consumer.orchestrate.Orchestrator
import kotlin.time.ExperimentalTime

@SpringBootApplication
@EnableCaching
@EnableRetry
class UcfsClaimantConsumer(private val orchestrator: Orchestrator) : CommandLineRunner {

    @ExperimentalTime
    override fun run(vararg args: String?) {
        orchestrator.orchestrate()
    }
}

fun main(args: Array<String>) {
    runApplication<UcfsClaimantConsumer>(*args)
}
