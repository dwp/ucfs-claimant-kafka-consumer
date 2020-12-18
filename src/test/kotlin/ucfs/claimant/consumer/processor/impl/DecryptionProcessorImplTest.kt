package ucfs.claimant.consumer.processor.impl

import arrow.core.Either
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import ucfs.claimant.consumer.domain.DataKeyResult
import ucfs.claimant.consumer.domain.DecryptionResult
import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.service.DecryptionService

class DecryptionProcessorImplTest : StringSpec() {

    init {
        "Returns right on success" {
            val cipherService = mock<DecryptionService> {
                on {
                    decrypt(datakey, initialisationVector, encryptedDbObject)
                } doReturn Either.Right(decryptedDbObject)
            }
            val decryptionProcessor = DecryptionProcessorImpl(cipherService)
            val json = json()
            val datakeyResult = DataKeyResult(json, initialisationVector, datakey)
            val queueRecord = mock<SourceRecord>()
            val result = decryptionProcessor.process(Pair(queueRecord, datakeyResult))
            result shouldBeRight { (record, result) ->
                record shouldBeSameInstanceAs queueRecord
                result shouldBe DecryptionResult(json, decryptedDbObject)
            }
        }

        "Returns left on failure" {
            val left = Exception("Failed to decrypt")
            val cipherService = mock<DecryptionService> {
                on { decrypt(datakey, initialisationVector, encryptedDbObject) } doReturn Either.Left(left)
            }
            val decryptionProcessor = DecryptionProcessorImpl(cipherService)
            val json = json()
            val datakeyResult = DataKeyResult(json, initialisationVector, datakey)
            val queueRecord = mock<SourceRecord> {
                on { key() } doReturn "key".toByteArray()
            }
            val result = decryptionProcessor.process(Pair(queueRecord, datakeyResult))
            result shouldBeLeft queueRecord
        }
    }

    private fun json(): JsonObject =
            Gson().fromJson("""
                {
                    "message": {
                        "dbObject": "$encryptedDbObject"
                    }
                }
            """.trimIndent(), JsonObject::class.java)

    private val initialisationVector = "initialisationVector"
    private val datakey = "datakey"
    private val encryptedDbObject = "encryptedDbObject"
    private val decryptedDbObject = "decryptedDbObject"
}
