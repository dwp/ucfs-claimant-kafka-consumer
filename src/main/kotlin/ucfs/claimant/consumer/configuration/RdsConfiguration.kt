package ucfs.claimant.consumer.configuration

import org.apache.commons.dbcp2.BasicDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ucfs.claimant.consumer.repository.SecretRepository
import java.io.File
import javax.sql.DataSource
import kotlin.time.ExperimentalTime

@Configuration
class RdsConfiguration(private val secretRepository: SecretRepository,
                       private val databaseEndpoint: String,
                       private val databasePort: Int,
                       private val databaseName: String,
                       private val databaseUser: String,
                       private val databasePasswordSecretName: String,
                       private val databaseCaCertPath: String) {

    @ExperimentalTime
    @Bean
    fun dataSource(): DataSource =
        BasicDataSource().apply {
            url = "jdbc:mysql://$databaseEndpoint:$databasePort/$databaseName"
            addConnectionProperty("user", databaseUser)
            addConnectionProperty("password", secretRepository.secret(databasePasswordSecretName))
            if (databaseCaCertPath.isNotBlank()) {
                addConnectionProperty("ssl_ca_path", databaseCaCertPath)
                addConnectionProperty("ssl_ca", File(databaseCaCertPath).readText())
                addConnectionProperty("ssl_verify_cert", "true")
            }
        }
}
