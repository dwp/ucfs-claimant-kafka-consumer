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
                         var claimantSourceId: String = "citizen_id",
                         var contractSourceId: String = "contract_id",
                         var statementSourceId: String = "statement_id") {

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
    @Qualifier("sourceIds")
    fun sourceIds(claimantSourceTopic: String, contractSourceTopic: String, statementSourceTopic: String): Map<String, String> =
        mapOf(claimantSourceTopic to claimantSourceId, contractSourceTopic to contractSourceId, statementSourceTopic to statementSourceId)

    @Bean
    @Qualifier("targetTables")
    fun targetTables(claimantSourceTopic: String, contractSourceTopic: String, statementSourceTopic: String): Map<String, String> =
        mapOf(claimantSourceTopic to claimantTable, contractSourceTopic to contractTable, statementSourceTopic to statementTable)
}

