package ucfs.claimant.consumer.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "source")
data class SourceProperties(var claimantTopic: String = "db.core.claimant",
                            var contractTopic: String = "db.core.contract",
                            var statementTopic: String = "db.core.statement",
                            var claimantIdSourceField: String = "citizenId",
                            var contractIdSourceField: String = "contractId",
                            var statementIdSourceField: String = "statementId")  {

    @Bean
    fun claimantTopic() = claimantTopic

    @Bean
    fun contractTopic() = contractTopic

    @Bean
    fun statementTopic() = statementTopic

    @Bean
    fun claimantIdSourceField() = claimantIdSourceField

    @Bean
    fun contractIdSourceField() = contractIdSourceField

    @Bean
    fun statementIdSourceField() = statementIdSourceField
}
