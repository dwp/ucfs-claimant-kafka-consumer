package ucfs.claimant.consumer.processor.impl

import arrow.core.left
import arrow.core.right
import com.nhaarman.mockitokotlin2.*
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.prometheus.client.Counter
import ucfs.claimant.consumer.domain.DataKeyResult
import ucfs.claimant.consumer.domain.EncryptionExtractionResult
import ucfs.claimant.consumer.domain.EncryptionMetadata
import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.exception.DataKeyServiceUnavailableException
import ucfs.claimant.consumer.processor.impl.SourceData.jsonProcessingExtract
import ucfs.claimant.consumer.repository.DecryptingDataKeyRepository

class DatakeyProcessorImplTest : StringSpec() {

    init {
        "Returns right if datakey call successful" {
            val datakeyRepository = mock<DecryptingDataKeyRepository> {
                on { decryptDataKey(encryptingKeyId, encryptedKey) } doReturn decryptedKey.right()
            }
            val counter = mock<Counter>()
            val processor = DataKeyProcessorImpl(datakeyRepository, counter)
            verifyZeroInteractions(counter)
            val input = encryptionExtractionResult()
            val queueRecord = mock<SourceRecord>()
            val result = processor.process(Pair(queueRecord, input))
            result shouldBeRight { (record, result) ->
                record shouldBeSameInstanceAs queueRecord
                result shouldBe DataKeyResult(jsonProcessingExtract(), initialisationVector, decryptedKey)
            }
        }

        "Returns left if can't decrypt datakey" {
            val datakeyRepository = mock<DecryptingDataKeyRepository> {
                on {
                    decryptDataKey(encryptingKeyId, encryptedKey)
                } doReturn Pair(returnCode, Pair(encryptingKeyId, encryptedKey)).left()
            }
            val counter = mock<Counter>()
            val processor = DataKeyProcessorImpl(datakeyRepository, counter)
            val input = encryptionExtractionResult()
            val queueRecord = mock<SourceRecord> {
                on { key() } doReturn "key".toByteArray()
            }
            val result = processor.process(Pair(queueRecord, input))
            result shouldBeLeft queueRecord
            verify(counter, times(1)).inc()
            verifyNoMoreInteractions(counter)
        }

        "Throws exception if service unavailable" {
            val thrown = DataKeyServiceUnavailableException("Service Unavailable")
            val datakeyRepository = mock<DecryptingDataKeyRepository> {
                on { decryptDataKey(encryptingKeyId, encryptedKey) } doThrow thrown
            }
            val counter = mock<Counter>()
            val processor = DataKeyProcessorImpl(datakeyRepository, counter)
            val input = encryptionExtractionResult()
            val error = shouldThrow<DataKeyServiceUnavailableException> {
                processor.process(Pair(mock(), input))
            }
            error shouldBeSameInstanceAs thrown
            verify(counter, times(1)).inc()
            verifyNoMoreInteractions(counter)
        }

    }

    private fun encryptionExtractionResult(): EncryptionExtractionResult =
            EncryptionExtractionResult(jsonProcessingExtract(), EncryptionMetadata(encryptingKeyId, encryptedKey, initialisationVector))

    private val encryptingKeyId = "encryptingKeyId"
    private val encryptedKey = "encryptedKey"
    private val initialisationVector = "initialisationVector"
    private val decryptedKey = "decryptedKey"
    private val returnCode = 400
}
