package ucfs.claimant.consumer.service

import ucfs.claimant.consumer.domain.DatakeyServiceResult

interface DatakeyService {
    fun decryptKey(encryptionKeyId: String, encryptedKey: String): DatakeyServiceResult
}
