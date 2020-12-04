package ucfs.claimant.consumer.service

import ucfs.claimant.consumer.domain.CipherServiceEncryptionData

interface EncryptionService {
    fun encrypt(plaintext: String): CipherServiceEncryptionData
}
