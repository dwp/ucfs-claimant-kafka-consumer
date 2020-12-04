package ucfs.claimant.consumer.repository

interface SaltRepository {
    fun salt(): String
}
