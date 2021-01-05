package ucfs.claimant.consumer.repository

interface SecretRepository {
    fun secret(name: String): String
}
