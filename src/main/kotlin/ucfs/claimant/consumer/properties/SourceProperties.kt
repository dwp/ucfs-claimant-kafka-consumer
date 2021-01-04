package ucfs.claimant.consumer.properties

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ucfs.claimant.consumer.transformer.Transformer

@Configuration
@ConfigurationProperties(prefix = "source")
data class SourceProperties(var claimantTopic: String = "db.core.claimant",
                            var contractTopic: String = "db.core.contract",
                            var statementTopic: String = "db.core.statement",
                            var claimantIdField: String = "citizenId",
                            var contractIdField: String = "contractId",
                            var statementIdField: String = "statementId")  {

    @Bean
    fun claimantTopic() = claimantTopic

    @Bean
    fun contractTopic() = contractTopic

    @Bean
    fun statementTopic() = statementTopic

    @Bean
    @Qualifier("transformers")
    fun transformers(claimantTransformer: Transformer, contractTransformer: Transformer, statementTransformer: Transformer) =
        mapOf(claimantTopic to claimantTransformer, contractTopic to contractTransformer, statementTopic to statementTransformer)

    @Bean
    @Qualifier("idFields")
    fun idFields() = mapOf(claimantTopic to claimantIdField, contractTopic to contractIdField, statementTopic to statementIdField)

}
