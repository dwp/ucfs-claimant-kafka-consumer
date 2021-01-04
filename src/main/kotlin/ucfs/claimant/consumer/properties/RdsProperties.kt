package ucfs.claimant.consumer.properties

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "rds")
data class RdsProperties(var endpoint: String = "rds",
                         var port: Int = 3306,
                         var database: String = "",
                         var user: String = "",
                         var passwordSecretName: String = "",
                         var caCertPath: String = "",
                         var claimantTable: String = "claimant",
                         var contractTable: String = "contract",
                         var statementTable: String = "statement",
                         var claimantNaturalId: String = "citizen_id",
                         var contractNaturalId: String = "contract_id",
                         var statementNaturalId: String = "statement_id") {

    @Bean
    fun databaseEndpoint() = endpoint

    @Bean
    fun databasePort() = port

    @Bean
    fun databaseName() = database

    @Bean
    fun databaseUser() = user

    @Bean
    fun databasePasswordSecretName() = passwordSecretName

    @Bean
    fun databaseCaCertPath() = caCertPath

    @Bean
    @Qualifier("naturalIds")
    fun naturalIds(claimantTopic: String, contractTopic: String, statementTopic: String): Map<String, String> =
        mapOf(claimantTopic to claimantNaturalId, contractTopic to contractNaturalId, statementTopic to statementNaturalId)

    @Bean
    @Qualifier("targetTables")
    fun targetTables(claimantTopic: String, contractTopic: String, statementTopic: String): Map<String, String> =
        mapOf(claimantTopic to claimantTable, contractTopic to contractTable, statementTopic to statementTable)
}

