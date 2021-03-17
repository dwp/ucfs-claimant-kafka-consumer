package ucfs.claimant.consumer.properties

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "rds")
data class RdsProperties(@Value("rds.truststore") var truststore: String = "",
                         @Value("rds.truststore.password") var truststorePassword: String = "",
                         var claimantTable: String = "claimant",
                         var contractTable: String = "contract",
                         var statementTable: String = "statement",
                         var claimantNaturalIdField: String = "citizen_id",
                         var contractNaturalIdField: String = "contract_id",
                         var statementNaturalIdField: String = "statement_id") {

    @Bean
    fun trustStore() = truststore

    @Bean
    fun trustStorePassword() = truststorePassword

    @Bean
    fun claimantTable() = claimantTable

    @Bean
    fun contractTable() = contractTable

    @Bean
    fun statementTable() = statementTable

    @Bean
    fun claimantNaturalIdField() = claimantNaturalIdField

    @Bean
    fun contractNaturalIdField() = contractNaturalIdField

    @Bean
    fun statementNaturalIdField() = statementNaturalIdField
}

