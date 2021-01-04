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

data class EncryptionExtractionResult(val json: JsonObject, val encryptionMetadata: EncryptionMetadata)
data class DataKeyResult(val json: JsonObject, val initializationVector: String, val datakey: String)

data class CipherServiceEncryptionResult(val encryptingKeyId: String, val initialisationVector: String,
                                         val encryptedDataKey: String, val cipherText: String)

data class DecryptionResult(val json: JsonObject, val plainText: String)
data class TransformationResult(val json: JsonObject, val transformedDbObject: String, val naturalId: String)

typealias DecryptionData = Either<Any, String>
typealias CipherServiceEncryptionData = Either<Throwable, CipherServiceEncryptionResult>
typealias DataKeyServiceResponse = Either<Any, String>

typealias SourceRecord = ConsumerRecord<ByteArray, ByteArray>

typealias SourceRecordProcessingResult = Pair<SourceRecord, String>
typealias ValidationProcessingResult = Pair<SourceRecord, String>
typealias JsonProcessingResult = Pair<SourceRecord, JsonObject>
typealias ExtractionProcessingResult = Pair<SourceRecord, EncryptionExtractionResult>
typealias DatakeyProcessingResult = Pair<SourceRecord, DataKeyResult>
typealias DecryptionProcessingResult = Pair<SourceRecord, DecryptionResult>
typealias TransformationProcessingResult = Pair<SourceRecord, TransformationResult>

typealias SourceRecordProcessingOutput = Either<SourceRecord, SourceRecordProcessingResult>
typealias JsonProcessingOutput = Either<SourceRecord, JsonProcessingResult>
typealias ValidationProcessingOutput = Either<SourceRecord, ValidationProcessingResult>
typealias ExtractionProcessingOutput = Either<SourceRecord, ExtractionProcessingResult>
typealias DatakeyProcessingOutput = Either<SourceRecord, DatakeyProcessingResult>
typealias DecryptionProcessingOutput = Either<SourceRecord, DecryptionProcessingResult>
typealias TransformationProcessingOutput = Either<SourceRecord, TransformationProcessingResult>
