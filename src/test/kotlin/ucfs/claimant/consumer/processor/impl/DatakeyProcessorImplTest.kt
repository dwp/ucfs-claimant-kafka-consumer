package ucfs.claimant.consumer.processor.impl

import arrow.core.Either
import com.google.gson.JsonObject
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.mock
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import ucfs.claimant.consumer.domain.DataKeyResult
import ucfs.claimant.consumer.domain.EncryptionExtractionResult
import ucfs.claimant.consumer.domain.EncryptionMetadata
import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.exception.DataKeyServiceUnavailableException
import ucfs.claimant.consumer.repository.DecryptingDataKeyRepository

class DatakeyProcessorImplTest : StringSpec() {

    init {
        "Returns right if datakey call successful" {
            val datakeyRepository = mock<DecryptingDataKeyRepository> {
                on { decryptDataKey(encryptingKeyId, encryptedKey) } doReturn Either.Right(decryptedKey)
            }
            val processor = DataKeyProcessorImpl(datakeyRepository)
            val input = encryptionExtractionResult()
            val queueRecord = mock<SourceRecord>()
            val result = processor.process(Pair(queueRecord, input))
            result shouldBeRight { (record, result) ->
                record shouldBeSameInstanceAs queueRecord
                result shouldBe DataKeyResult(JsonObject(), initialisationVector, decryptedKey)
            }
        }

        "Returns left if can't decrypt datakey" {
            val datakeyRepository = mock<DecryptingDataKeyRepository> {
                on {
                    decryptDataKey(encryptingKeyId, encryptedKey)
                } doReturn Either.Left(Pair(returnCode, Pair(encryptingKeyId, encryptedKey)))
            }
            val processor = DataKeyProcessorImpl(datakeyRepository)
            val input = encryptionExtractionResult()
            val queueRecord = mock<SourceRecord> {
                on { key() } doReturn "key".toByteArray()
            }
            val result = processor.process(Pair(queueRecord, input))
            result shouldBeLeft queueRecord
        }

        "Throws exception if service unavailable" {
            val thrown = DataKeyServiceUnavailableException("Service Unavailable")
            val datakeyRepository = mock<DecryptingDataKeyRepository> {
                on { decryptDataKey(encryptingKeyId, encryptedKey) } doThrow thrown
            }
            val processor = DataKeyProcessorImpl(datakeyRepository)
            val input = encryptionExtractionResult()
            val error = shouldThrow<DataKeyServiceUnavailableException> {
                processor.process(Pair(mock(), input))
            }
            error shouldBeSameInstanceAs thrown
        }

    }

    private fun encryptionExtractionResult(): EncryptionExtractionResult =
            EncryptionExtractionResult(JsonObject(),
                    EncryptionMetadata(encryptingKeyId, encryptedKey, initialisationVector))

    private val encryptingKeyId = "encryptingKeyId"
    private val encryptedKey = "encryptedKey"
    private val initialisationVector = "initialisationVector"
    private val decryptedKey = "decryptedKey"
    private val returnCode = 400
}
