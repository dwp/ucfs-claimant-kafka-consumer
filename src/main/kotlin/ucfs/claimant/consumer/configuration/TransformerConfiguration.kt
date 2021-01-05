package ucfs.claimant.consumer.configuration

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ucfs.claimant.consumer.transformer.Transformer

@Configuration
class TransformerConfiguration() {

    @Bean
    @Qualifier("transformers")
    fun transformers(claimantTopic: String,
                     contractTopic: String,
                     statementTopic: String,
                     claimantTransformer: Transformer,
                     contractTransformer: Transformer,
                     statementTransformer: Transformer) =
        mapOf(claimantTopic to claimantTransformer,
            contractTopic to contractTransformer,
            statementTopic to statementTransformer)
}
