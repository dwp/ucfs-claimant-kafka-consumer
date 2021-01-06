package ucfs.claimant.consumer.processor.impl

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import ucfs.claimant.consumer.domain.DatabaseAction
import ucfs.claimant.consumer.domain.EncryptionExtractionResult
import ucfs.claimant.consumer.domain.EncryptionMetadata
import ucfs.claimant.consumer.domain.SourceRecord

class ExtractionProcessorImplTest : StringSpec() {
    private val encryptedKey = "encryptedEncryptionKey"
    private val encryptingKeyId = "keyEncryptionKeyId"
    private val initialisationVector = "initialisationVector"

    init {
        "Returns right if all encryption metadata present" {
            val json = Gson().fromJson("""
                {
                    "message": {  
                        "encryption": {
                            "encryptedEncryptionKey": "$encryptedKey",
                            "keyEncryptionKeyId": "$encryptingKeyId",
                            "initialisationVector": "$initialisationVector"
                        }
                    }
                }
            """, JsonObject::class.java)
            validateRight(json)
        }

        "Returns right if extra fields present" {
            val json = Gson().fromJson("""
                {
                    "message": {  
                        "encryption": {
                            "encryptedEncryptionKey": "$encryptedKey",
                            "keyEncryptionKeyId": "$encryptingKeyId",
                            "initialisationVector": "$initialisationVector",
                            "ignoredField": "IGNORED_FIELD"
                        }
                    }
                }
            """, JsonObject::class.java)
            validateRight(json)
        }

        "Returns left if encryption block missing" {
            val json = Gson().fromJson("""{ "message": {} }""", JsonObject::class.java)
            val queueRecord = mock<SourceRecord> {
                on { key() } doReturn "key".toByteArray()
            }
            val result = ExtractionProcessorImpl().process(Pair(queueRecord, Pair(json ,DatabaseAction.MONGO_INSERT)))
            result shouldBeLeft queueRecord
        }


        "Returns left if encryptedEncryptionKey missing" {
            val encryptionBlock = """{
                |"keyEncryptionKeyId": "$encryptingKeyId",
                |"initialisationVector": "$initialisationVector"
            |}""".trimMargin()
            validateLeft(encryptionBlock)
        }

        "Returns left if keyEncryptionKeyId missing" {
            val encryptionBlock = """{
                "encryptedEncryptionKey": "$encryptedKey",
                "initialisationVector": "$initialisationVector"
            }"""
            validateLeft(encryptionBlock)
        }

        "Returns left if initialisationVector missing" {
            val encryptionBlock = """{
                "encryptedEncryptionKey": "$encryptedKey",
                "keyEncryptionKeyId": "$encryptingKeyId"
            }"""
            validateLeft(encryptionBlock)
        }

    }

    private fun validateRight(json: JsonObject) {
        val queueRecord = mock<SourceRecord>()
        val result = ExtractionProcessorImpl().process(Pair(queueRecord, Pair(json, DatabaseAction.MONGO_INSERT)))
        result shouldBeRight { (record, result) ->
            record shouldBeSameInstanceAs queueRecord
            result shouldBe EncryptionExtractionResult(json, EncryptionMetadata(encryptingKeyId,
                    encryptedKey, initialisationVector))
        }
    }

    private fun validateLeft(encryptionBlock: String) {
        val (_, json) = encryptionBlockAndJson(encryptionBlock)
        val queueRecord = mock<SourceRecord> {
            on { key() } doReturn "key".toByteArray()
        }
        val result = ExtractionProcessorImpl().process(Pair(queueRecord, Pair(json, DatabaseAction.MONGO_UPDATE)))
        result shouldBeLeft queueRecord
    }

    private fun encryptionBlockAndJson(encryptionBlock: String): Pair<JsonObject, JsonObject> {
        val encryptionJson = Gson().fromJson(encryptionBlock, JsonObject::class.java)
        val json = Gson().fromJson("""{
                "message": {  
                    "encryption": $encryptionBlock 
                }
            }""", JsonObject::class.java)
        return Pair(encryptionJson, json)
    }
}
