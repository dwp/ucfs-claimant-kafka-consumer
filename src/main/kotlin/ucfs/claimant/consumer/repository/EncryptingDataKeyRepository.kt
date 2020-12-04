package ucfs.claimant.consumer.repository

import ucfs.claimant.consumer.domain.EncryptedDataKeyServiceData

interface EncryptingDataKeyRepository {
    fun encryptedDataKey(): EncryptedDataKeyServiceData
}
