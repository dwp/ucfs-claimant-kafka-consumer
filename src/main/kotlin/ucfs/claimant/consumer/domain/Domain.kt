package ucfs.claimant.consumer.domain

import arrow.core.Either
import com.google.gson.JsonObject
import org.apache.kafka.clients.consumer.ConsumerRecord

data class EncryptionMetadata(val encryptingKeyId: String, val encryptedKey: String, val initialisationVector: String)
data class DataKeyServiceResult(val dataKeyEncryptionKeyId: String, val plaintextDataKey: String, val ciphertextDataKey: String)

data class EncryptionExtractionResult(val json: JsonObject, val encryptionMetadata: EncryptionMetadata)
data class DatakeyResult(val json: JsonObject, val initializationVector: String, val datakey: String)
data class DecryptionResult(val json: JsonObject, val decryptedDbObject: String)


typealias CipherServiceResult = Either<Throwable, String>
typealias DatakeyServiceResult = Either<Any, String>

typealias SourceRecord = ConsumerRecord<ByteArray, ByteArray>

typealias SourceRecordProcessingResult = Pair<SourceRecord, String>
typealias ValidationProcessingResult = Pair<SourceRecord, String>
typealias JsonProcessingResult = Pair<SourceRecord, JsonObject>
typealias ExtractionProcessingResult = Pair<SourceRecord, EncryptionExtractionResult>
typealias DatakeyProcessingResult = Pair<SourceRecord, DatakeyResult>
typealias DecryptionProcessingResult = Pair<SourceRecord, DecryptionResult>

typealias SourceRecordProcessingOutput = Either<SourceRecord, SourceRecordProcessingResult>
typealias JsonProcessingOutput = Either<SourceRecord, JsonProcessingResult>
typealias ValidationProcessingOutput = Either<SourceRecord, ValidationProcessingResult>
typealias ExtractionProcessingOutput = Either<SourceRecord, ExtractionProcessingResult>
typealias DatakeyProcessingOutput = Either<SourceRecord, DatakeyProcessingResult>
typealias DecryptionProcessingOutput = Either<SourceRecord, DecryptionProcessingResult>
