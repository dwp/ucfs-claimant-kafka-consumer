package ucfs.claimant.consumer.service.impl

import com.nhaarman.mockitokotlin2.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.prometheus.client.Counter
import ucfs.claimant.consumer.domain.EncryptedDataKeyServiceData
import ucfs.claimant.consumer.repository.EncryptingDataKeyRepository

class EncryptionServiceImplTest: StringSpec() {
    init {
        "Re-uses key" {
            val counter = mock<Counter>()
            val dataKeyRepository = mock<EncryptingDataKeyRepository> {
                on { encryptedDataKey() } doReturn encryptedDataKeyServiceData()
            }
            val encryptionService = encryptionService(dataKeyRepository, counter)
            (1..105).forEach { i -> encryptionService.encrypt("$i") }
            verify(dataKeyRepository, times(10)).encryptedDataKey()
            verifyNoMoreInteractions(dataKeyRepository)
            verifyZeroInteractions(counter)
        }

        "Metricates failures" {
            val counter = mock<Counter>()
            val dataKeyRepository = mock<EncryptingDataKeyRepository> {
                on { encryptedDataKey() } doReturn encryptedDataKeyServiceData() doThrow RuntimeException("Error")
            }

            val encryptionService = encryptionService(dataKeyRepository, counter)

            shouldThrow<java.lang.RuntimeException> {
                while (true) {
                    encryptionService.encrypt("plaintext")
                }
            }

            verify(dataKeyRepository, times(2)).encryptedDataKey()
            verifyNoMoreInteractions(dataKeyRepository)
            verify(counter, times(1)).inc()
        }
    }

    private fun encryptedDataKeyServiceData(): EncryptedDataKeyServiceData =
            EncryptedDataKeyServiceData("encryptingKeyId", "dataKey".toByteArray(), "encryptedDataKey")

    private fun encryptionService(dataKeyRepository: EncryptingDataKeyRepository, counter: Counter): EncryptionServiceImpl =
            EncryptionServiceImpl(dataKeyRepository,
                "transformation",
                "algorithm",
                "provider", mock(), 10, 10, counter)

}
