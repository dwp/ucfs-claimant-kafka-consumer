package ucfs.claimant.consumer.utility

import kotlinx.coroutines.delay
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.PartitionInfo
import uk.gov.dwp.dataworks.logging.DataworksLogger
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ExperimentalTime
object KafkaConsumerUtility {

    tailrec suspend fun <K, V> subscribe(consumer: KafkaConsumer<K, V>, includesRegex: Regex) {

        val currentSubscription = consumer.subscription()
        val topics = includedTopics(consumer, includesRegex)

        if (topics.toSet() != currentSubscription && topics.isNotEmpty()) {
            topics.minus(currentSubscription).forEach {
                logger.info("New topic found", "topic" to it)
            }
            consumer.subscribe(topics)
            return
        }

        if (currentSubscription.isNotEmpty()) {
            return
        }

        logger.info("No topics and no current subscription, trying again")
        delay(5.seconds)
        subscribe(consumer, includesRegex)
    }

    private fun <K, V> includedTopics(consumer: KafkaConsumer<K, V>, inclusionRegex: Regex): List<String> =
            consumer.listTopics()
                    .map(Map.Entry<String, List<PartitionInfo>>::key)
                    .filter(inclusionRegex::matches)

    private val logger = DataworksLogger.getLogger(KafkaConsumerUtility::class)
}
