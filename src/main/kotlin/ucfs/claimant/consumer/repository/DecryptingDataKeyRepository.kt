package ucfs.claimant.consumer.repository

import ucfs.claimant.consumer.domain.DataKeyServiceResponse

interface DecryptingDataKeyRepository {
    fun decryptDataKey(encryptingKeyId: String, encryptedKey: String): DataKeyServiceResponse
}
