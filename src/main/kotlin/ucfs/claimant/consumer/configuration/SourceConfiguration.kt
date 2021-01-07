package ucfs.claimant.consumer.configuration

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SourceConfiguration(private val claimantTopic: String,
                          private val contractTopic: String,
                          private val statementTopic: String,
                          private val claimantIdSourceField: String,
                          private val contractIdSourceField: String,
                          private val statementIdSourceField: String) {
    @Bean
    @Qualifier("idSourceFields")
    fun idSourceFields() =
        mapOf(claimantTopic to claimantIdSourceField, contractTopic to contractIdSourceField, statementTopic to statementIdSourceField)
}
