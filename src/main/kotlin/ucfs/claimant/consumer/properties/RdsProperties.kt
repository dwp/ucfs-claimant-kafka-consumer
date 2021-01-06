package ucfs.claimant.consumer.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "rds")
data class RdsProperties(var caCertPath: String = "./rds-ca-2019-2015-root.pem",
                         var claimantTable: String = "claimant",
                         var contractTable: String = "contract",
                         var statementTable: String = "statement") {

    @Bean
    fun databaseCaCertPath() = caCertPath

    @Bean
    fun claimantTable() = claimantTable

    @Bean
    fun contractTable() = contractTable

    @Bean
    fun statementTable() = statementTable
}

