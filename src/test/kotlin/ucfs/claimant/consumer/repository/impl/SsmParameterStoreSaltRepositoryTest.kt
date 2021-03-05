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
import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.services.ssm.model.GetParameterRequest
import software.amazon.awssdk.services.ssm.model.GetParameterResponse
import software.amazon.awssdk.services.ssm.model.Parameter
import ucfs.claimant.consumer.repository.SaltRepository
import java.lang.RuntimeException


@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [SsmParameterStoreSaltRepository::class, TestContext::class])
@EnableRetry
@TestPropertySource(properties = [
    "ssm.retry.maxAttempts=5",
    "ssm.retry.delay=1",
    "ssm.retry.multiplier=1"])
class SsmParameterStoreSaltRepositoryTest: StringSpec() {

    init {
        "Does not retry if successful" {
            given(ssmClient.getParameter(any<GetParameterRequest>())).willReturn(getParameterResponse())
            ssmParameterStoreSaltRepository.salt() shouldBe SALT_VALUE
            verify(ssmClient, times(1)).getParameter(any<GetParameterRequest>())
            verifyNoMoreInteractions(ssmClient)
            verifyZeroInteractions(counter)
        }

        "Retries until retries exhausted" {
            given(ssmClient.getParameter(any<GetParameterRequest>())).willThrow(RuntimeException("Error"))
            shouldThrow<RuntimeException>(ssmParameterStoreSaltRepository::salt)
            verify(ssmClient, times(5)).getParameter(any<GetParameterRequest>())
            verifyNoMoreInteractions(ssmClient)
            verify(counter, times(5)).inc()
            verifyNoMoreInteractions(counter)
        }

        "Retries until success" {
            given(ssmClient.getParameter(any<GetParameterRequest>())).willThrow(RuntimeException("Error"))
                .willThrow(RuntimeException("Error"))
                .willReturn(getParameterResponse())
            ssmParameterStoreSaltRepository.salt() shouldBe SALT_VALUE
            verify(ssmClient, times(3)).getParameter(any<GetParameterRequest>())
            verifyNoMoreInteractions(ssmClient)
            verify(counter, times(2)).inc()
            verifyNoMoreInteractions(counter)
        }
    }

    private fun getParameterResponse(): GetParameterResponse =
        with(GetParameterResponse.builder()) {
            parameter(parameter())
            build()
        }

    private fun parameter(): Parameter =
        with(Parameter.builder()) {
            value(SALT_VALUE)
            build()
        }

    override fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)
        reset(counter)
        reset(ssmClient)
    }

    override fun listeners(): List<TestListener> = listOf(SpringListener)

    @Autowired
    private lateinit var ssmParameterStoreSaltRepository: SaltRepository

    @MockBean
    private lateinit var ssmClient: SsmClient

    @MockBean
    private lateinit var counter: Counter

    companion object {
        private const val SALT_VALUE = "SALT_VALUE"
    }
}
