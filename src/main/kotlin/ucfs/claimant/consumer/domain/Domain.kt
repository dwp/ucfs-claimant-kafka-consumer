package ucfs.claimant.consumer.domain

import arrow.core.Either
import com.google.gson.JsonObject
import org.apache.kafka.clients.consumer.ConsumerRecord

data class EncryptionMetadata(val encryptingKeyId: String, val encryptedKey: String, val initialisationVector: String)
data class DataKeyDecryptionServiceData(val dataKeyEncryptionKeyId: String, val plaintextDataKey: String, val ciphertextDataKey: String)

data class EncryptedDataKeyServiceData(val encryptingKeyId: String, val dataKey: ByteArray, val encryptedDataKey: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedDataKeyServiceData

        if (encryptingKeyId != other.encryptingKeyId) return false
        if (!dataKey.contentEquals(other.dataKey)) return false
        if (encryptedDataKey != other.encryptedDataKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptingKeyId.hashCode()
        result = 31 * result + dataKey.contentHashCode()
        result = 31 * result + encryptedDataKey.hashCode()
        return result
    }
}

enum class DatabaseAction { MONGO_INSERT, MONGO_UPDATE, MONGO_DELETE }

data class EncryptionExtractionResult(val extract: JsonProcessingExtract, val encryptionMetadata: EncryptionMetadata)
data class DataKeyResult(val extract: JsonProcessingExtract, val initializationVector: String, val datakey: String)

data class CipherServiceEncryptionResult(val encryptingKeyId: String, val initialisationVector: String,
                                         val encryptedDataKey: String, val cipherText: String)

data class DecryptionResult(val extract: JsonProcessingExtract, val plainText: String)
data class TransformationResult(val extract: JsonProcessingExtract, val transformedDbObject: String)
data class FilterResult(val transformationResult: TransformationResult, val passThrough: Boolean)
data class JsonProcessingExtract(val jsonObject: JsonObject, val id: String, val action: DatabaseAction,
                                 val timestampAndSource: Pair<String, String>)

typealias DecryptionData = Either<Any, String>
typealias CipherServiceEncryptionData = Either<Throwable, CipherServiceEncryptionResult>
typealias DataKeyServiceResponse = Either<Any, String>

typealias SourceRecord = ConsumerRecord<ByteArray, ByteArray>

typealias SourceRecordProcessingResult = Pair<SourceRecord, String>
typealias ValidationProcessingResult = Pair<SourceRecord, String>
typealias JsonProcessingResult = Pair<SourceRecord, JsonProcessingExtract>
typealias ExtractionProcessingResult = Pair<SourceRecord, EncryptionExtractionResult>
typealias DatakeyProcessingResult = Pair<SourceRecord, DataKeyResult>
typealias DecryptionProcessingResult = Pair<SourceRecord, DecryptionResult>
typealias TransformationProcessingResult = Pair<SourceRecord, TransformationResult>
typealias FilterProcessingResult = Pair<SourceRecord, FilterResult>

typealias SourceRecordProcessingOutput = Either<SourceRecord, SourceRecordProcessingResult>
typealias JsonProcessingOutput = Either<SourceRecord, JsonProcessingResult>
typealias ValidationProcessingOutput = Either<SourceRecord, ValidationProcessingResult>
typealias ExtractionProcessingOutput = Either<SourceRecord, ExtractionProcessingResult>
typealias DatakeyProcessingOutput = Either<SourceRecord, DatakeyProcessingResult>
typealias DecryptionProcessingOutput = Either<SourceRecord, DecryptionProcessingResult>
typealias TransformationProcessingOutput = Either<SourceRecord, TransformationProcessingResult>
typealias FilterProcessingOutput = Either<SourceRecord, FilterProcessingResult>


