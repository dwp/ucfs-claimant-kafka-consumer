package ucfs.claimant.consumer.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "rds")
data class RdsProperties(var endpoint: String = "rds",
                         var port: Int = 3306,
                         var database: String = "",
                         var user: String = "",
                         var caCertPath: String = "",
                         var claimantTable: String = "claimant",
                         var contractTable: String = "contract",
                         var statementTable: String = "statement") {

    @Bean
    fun databaseEndpoint() = endpoint

    @Bean
    fun databasePort() = port

    @Bean
    fun databaseName() = database

    @Bean
    fun databaseUser() = user

    @Bean
    fun databaseCaCertPath() = caCertPath

    @Bean
    fun claimantTable() = claimantTable

    @Bean
    fun contractTable() = contractTable

    @Bean
    fun statementTable() = statementTable
}

