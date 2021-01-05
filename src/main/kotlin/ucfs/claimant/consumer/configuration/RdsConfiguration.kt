package ucfs.claimant.consumer.configuration

import org.apache.commons.dbcp2.BasicDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ucfs.claimant.consumer.repository.SecretRepository
import java.io.File
import javax.sql.DataSource
import kotlin.time.ExperimentalTime

@Configuration
class RdsConfiguration(private val databaseEndpoint: String,
                       private val databasePort: Int,
                       private val databaseName: String,
                       private val databaseUser: String,
                       private val databaseCaCertPath: String,
                       private val claimantTable: String,
                       private val contractTable: String,
                       private val statementTable: String) {

    @ExperimentalTime
    @Bean
    fun dataSource(secretRepository: SecretRepository, rdsPasswordSecretName: String): DataSource =
        BasicDataSource().apply {
            url = "jdbc:mysql://$databaseEndpoint:$databasePort/$databaseName"
            addConnectionProperty("user", databaseUser)
            addConnectionProperty("password", secretRepository.secret(rdsPasswordSecretName))
            if (databaseCaCertPath.isNotBlank()) {
                addConnectionProperty("ssl_ca_path", databaseCaCertPath)
                addConnectionProperty("ssl_ca", File(databaseCaCertPath).readText())
                addConnectionProperty("ssl_verify_cert", "true")
            }
        }

    @Bean
    @Qualifier("targetTables")
    fun targetTables(claimantTopic: String, contractTopic: String, statementTopic: String,): Map<String, String> =
        mapOf(claimantTopic to claimantTable, contractTopic to contractTable, statementTopic to statementTable)

}
