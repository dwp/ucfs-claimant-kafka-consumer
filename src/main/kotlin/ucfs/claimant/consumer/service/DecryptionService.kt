package ucfs.claimant.consumer.service

import ucfs.claimant.consumer.domain.DecryptionData

interface DecryptionService {
    fun decrypt(key: String, initializationVector: String, encrypted: String): DecryptionData
}
