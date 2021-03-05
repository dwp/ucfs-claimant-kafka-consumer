package ucfs.claimant.consumer.repository.impl

import com.nhaarman.mockitokotlin2.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.shouldBe
import io.kotest.spring.SpringListener
import io.prometheus.client.Counter
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.retry.annotation.EnableRetry
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse
import ucfs.claimant.consumer.repository.SecretRepository

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [SecretsManagerSecretRepository::class])
@EnableRetry
@TestPropertySource(properties = [
        "secrets.retry.maxAttempts=5",
        "secrets.retry.delay=1",
        "secrets.retry.multiplier=1"])
class SecretsManagerSecretRepositoryTest: StringSpec() {
    init {

        "Does not retry if successful" {
            given(secretsManagerClient.getSecretValue(any<GetSecretValueRequest>())).willReturn(getSecretValueResponse())
            val secret = secretRepository.secret(SECRET_NAME)
            secret shouldBe SECRET_VALUE
            verify(secretsManagerClient, times(1)).getSecretValue(any<GetSecretValueRequest>())
            verifyNoMoreInteractions(secretsManagerClient)
            verifyZeroInteractions(counter)
        }

        "Retries until successful" {
            given(secretsManagerClient.getSecretValue(any<GetSecretValueRequest>()))
                .willThrow(RuntimeException("Error"))
                .willThrow(RuntimeException("Error"))
                .willReturn(getSecretValueResponse())

            val secret = secretRepository.secret(SECRET_NAME)
            secret shouldBe SECRET_VALUE
            verify(secretsManagerClient, times(3)).getSecretValue(any<GetSecretValueRequest>())
            verifyNoMoreInteractions(secretsManagerClient)
            verify(counter, times(2)).inc()
            verifyNoMoreInteractions(counter)
        }

        "Gives up after max retries" {
            given(secretsManagerClient.getSecretValue(any<GetSecretValueRequest>()))
                .willThrow(RuntimeException("Error"))

            shouldThrow<RuntimeException> {
                secretRepository.secret(SECRET_NAME)
            }

            verify(secretsManagerClient, times(5)).getSecretValue(any<GetSecretValueRequest>())
            verifyNoMoreInteractions(secretsManagerClient)
            verify(counter, times(5)).inc()
            verifyNoMoreInteractions(counter)
        }
    }

    private fun getSecretValueResponse(): GetSecretValueResponse =
            with(GetSecretValueResponse.builder()) {
                secretString(SECRET_VALUE)
                build()
            }

    @Autowired
    private lateinit var secretRepository: SecretRepository

    @MockBean
    private lateinit var secretsManagerClient: SecretsManagerClient

    @MockBean
    private lateinit var counter: Counter

    override fun listeners(): List<TestListener> = listOf(SpringListener)

    override fun beforeEach(testCase: TestCase) {
        super.beforeEach(testCase)
        reset(counter)
        reset(secretsManagerClient)
    }

    companion object {
        private const val SECRET_NAME = "SECRET_NAME"
        private const val SECRET_VALUE = "SECRET_VALUE"
    }
}
