package ucfs.claimant.consumer.configuration

import arrow.core.*
import arrow.core.extensions.either.applicative.applicative
import org.apache.commons.dbcp2.BasicDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ucfs.claimant.consumer.repository.SecretRepository
import ucfs.claimant.consumer.utility.CipherExtensions.decoded
import ucfs.claimant.consumer.utility.GsonExtensions.integer
import ucfs.claimant.consumer.utility.GsonExtensions.jsonObject
import ucfs.claimant.consumer.utility.GsonExtensions.string
import java.io.File
import java.lang.RuntimeException
import javax.sql.DataSource
import kotlin.time.ExperimentalTime

@Configuration
class RdsConfiguration(private val databaseCaCertPath: String,
                       private val claimantTable: String,
                       private val contractTable: String,
                       private val statementTable: String) {

    @ExperimentalTime
    @Bean
    fun dataSource(secretRepository: SecretRepository, rdsSecretName: String): DataSource =
        secretRepository.secret(rdsSecretName).jsonObject().flatMap {
            Either.applicative<Any>().tupledN(it.string("dbInstanceIdentifier"),
                                              it.string("host"),
                                              it.integer("port"),
                                              it.string("username"),
                                              it.string("password")).fix()
        }.map { (instance, host, port, username, password) ->
            BasicDataSource().apply {
                addConnectionProperty("user", username)
                addConnectionProperty("password", password)
                url = "jdbc:mysql://$host:$port/$instance"
                if (databaseCaCertPath.isNotBlank()) {
                    addConnectionProperty("ssl_ca_path", databaseCaCertPath)
                    addConnectionProperty("ssl_ca", File(databaseCaCertPath).readText())
                    addConnectionProperty("ssl_verify_cert", "true")
                }
                else {
                    addConnectionProperty("useSSL", "false")
                }
            }
        }.fold(
            ifRight = { it },
            ifLeft = {
                throw RuntimeException("Failed to parse required connection parameters from secret '$rdsSecretName' value")
            })

    @Bean
    @Qualifier("targetTables")
    fun targetTables(claimantTopic: String, contractTopic: String, statementTopic: String,): Map<String, String> =
        mapOf(claimantTopic to claimantTable, contractTopic to contractTable, statementTopic to statementTable)

}
