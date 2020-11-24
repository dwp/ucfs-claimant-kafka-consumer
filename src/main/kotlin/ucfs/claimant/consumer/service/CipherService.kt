package ucfs.claimant.consumer.service

import ucfs.claimant.consumer.domain.CipherServiceResult

interface CipherService {
    fun decrypt(key: String, initializationVector: String, encrypted: String): CipherServiceResult
}
