package ucfs.claimant.consumer

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import ucfs.claimant.consumer.service.ConsumerService
import kotlin.time.ExperimentalTime

@SpringBootApplication
class UcfsClaimantConsumer(private val consumerService: ConsumerService) : CommandLineRunner {

    @ExperimentalTime
    override fun run(vararg args: String?) {
        consumerService.consume()
    }

}

fun main(args: Array<String>) {
    runApplication<UcfsClaimantConsumer>(*args)
}
