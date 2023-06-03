package me.tb

import java.security.SecureRandom

// TODO: Should we check for the characters in the secret string? Not sure if there are corner cases here.
//       Maybe only accepting base64 encoded strings would be better?
//       It also feels like we should require secrets to be a certain size, otherwise they'd be easy to brute force?
//       The issue is that test vectors in other libraries use secrets that are quite short
public class Secret(secret: String?) {
    public val value: ByteArray = generateSecret(secret)

    // init {
    //     require(secret.isNullOrBlank() || secret.length >= 32) { "Secret must be at least 32 bytes long" }
    // }

    private fun generateSecret(secret: String?): ByteArray {
        return if (secret.isNullOrBlank()) {
            val random = SecureRandom()
            val secretBytes = ByteArray(32)
            random.nextBytes(secretBytes)
            secretBytes
        } else {
            secret.toByteArray(Charsets.UTF_8)
        }
    }
}
