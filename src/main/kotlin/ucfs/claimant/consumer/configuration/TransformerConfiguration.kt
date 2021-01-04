package ucfs.claimant.consumer.configuration

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ucfs.claimant.consumer.transformer.Transformer

@Configuration
class TransformerConfiguration(val claimantTopic: String,
                               val contractTopic: String,
                               val statementTopic: String,
                               val claimantTransformer: Transformer,
                               val contractTransformer: Transformer,
                               val statementTransformer: Transformer) {

    @Bean
    @Qualifier("transformers")
    fun transformers() =
        mapOf(claimantTopic to claimantTransformer, contractTopic to contractTransformer, statementTopic to statementTransformer)
}
