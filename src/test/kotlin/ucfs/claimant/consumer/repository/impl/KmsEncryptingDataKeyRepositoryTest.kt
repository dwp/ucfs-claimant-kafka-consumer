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
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse
import ucfs.claimant.consumer.domain.EncryptedDataKeyServiceData
import ucfs.claimant.consumer.repository.EncryptingDataKeyRepository
import java.nio.charset.Charset
import java.util.*

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [KmsEncryptingDataKeyRepository::class, TestContext::class])
@EnableRetry
@TestPropertySource(properties = [
    "kms.retry.maxAttempts=5",
    "kms.retry.delay=1",
    "kms.retry.multiplier=1"])
class KmsEncryptingDataKeyRepositoryTest: StringSpec() {

    init {
        "Does not retry if successful" {
            given(kmsClient.generateDataKey(any<GenerateDataKeyRequest>())).willReturn(generateDataKeyResponse())
            val result = dataKeyRepository.encryptedDataKey()
            validateResult(result)
            verify(kmsClient, times(1)).generateDataKey(any<GenerateDataKeyRequest>())
            verifyNoMoreInteractions(kmsClient)
            verifyZeroInteractions(counter)
        }

        "Retries until successful" {
            given(kmsClient.generateDataKey(any<GenerateDataKeyRequest>()))
                .willThrow(RuntimeException("Error"))
                .willThrow(RuntimeException("Error"))
                .willReturn(generateDataKeyResponse())

            val result = dataKeyRepository.encryptedDataKey()
            validateResult(result)
            verify(counter, times(2)).inc()
            verifyNoMoreInteractions(counter)
            verify(kmsClient, times(3)).generateDataKey(any<GenerateDataKeyRequest>())
            verifyNoMoreInteractions(kmsClient)
        }

        "Gives up after max retries" {
            given(kmsClient.generateDataKey(any<GenerateDataKeyRequest>()))
                .willThrow(RuntimeException("Error"))
            shouldThrow<RuntimeException>(dataKeyRepository::encryptedDataKey)
            verify(counter, times(5)).inc()
            verifyNoMoreInteractions(counter)
            verify(kmsClient, times(5)).generateDataKey(any<GenerateDataKeyRequest>())
            verifyNoMoreInteractions(kmsClient)
        }
    }

    private fun generateDataKeyResponse(): GenerateDataKeyResponse =
        with(GenerateDataKeyResponse.builder()) {
            ciphertextBlob(SdkBytes.fromString(CIPHERTEXT_BLOB, Charset.defaultCharset()))
            keyId(KEY_ID)
            plaintext(SdkBytes.fromString(PLAINTEXT, Charset.defaultCharset()))
            build()
        }

    private fun validateResult(result: EncryptedDataKeyServiceData) {
        String(result.dataKey) shouldBe PLAINTEXT
        String(Base64.getDecoder().decode(result.encryptedDataKey)) shouldBe CIPHERTEXT_BLOB
        result.encryptingKeyId shouldBe KEY_ID
    }

    @Autowired
    private lateinit var dataKeyRepository: EncryptingDataKeyRepository

    @MockBean
    private lateinit var kmsClient: KmsClient

    @MockBean
    private lateinit var counter: Counter

    override fun listeners(): List<TestListener> = listOf(SpringListener)

    override fun beforeEach(testCase: TestCase) {
        super.beforeEach(testCase)
        reset(counter)
        reset(kmsClient)
    }

    companion object {
        private const val KEY_ID = "KEY_ID"
        private const val PLAINTEXT = "PLAINTEXT"
        private const val CIPHERTEXT_BLOB = "CIPHERTEXT_BLOB"
    }
}
