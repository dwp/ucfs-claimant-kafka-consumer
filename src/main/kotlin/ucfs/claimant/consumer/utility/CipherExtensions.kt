package ucfs.claimant.consumer.utility

import ucfs.claimant.consumer.utility.FunctionalUtility.encase
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object CipherExtensions {
    fun Cipher.finally(it: ByteArray) = encase { doFinal(it) }
    fun ByteArray.key(algorithm: String) = encase { SecretKeySpec(this, algorithm) }
    fun String.decoded() = encase { Base64.getDecoder().decode(this.toByteArray()) }
    fun ByteArray.encoded(): String = Base64.getEncoder().encodeToString(this)
}
