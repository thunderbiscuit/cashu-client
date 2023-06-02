package me.tb

import fr.acinq.bitcoin.PublicKey
import fr.acinq.bitcoin.PrivateKey
import java.security.SecureRandom

/**
 * The data bundle Alice must create prior to communicating with the mint.
 * @param secret The secret x that is used in hashToCurve(x) to create Y
 * @param blindedSecret The blinded secret B_ that is sent to the mint
 * @param blindingFactor The blinding factor r, private key of the point R that is used to blind key Y
 */
public class PreMintBundle private constructor(
    public val secret: Secret,
    public val blindedSecret: PublicKey,
    public val blindingFactor: ByteArray,
    // public val blindingKey: PrivateKey,
) {
    public companion object {
        public fun create(secret: Secret, blindingFactor: ByteArray?): PreMintBundle {
            require(blindingFactor == null || blindingFactor.size == 32) { "Blinding factor must be 32 bytes long because it's a private key" }

            val blindingFactor: ByteArray = blindingFactor ?: randomBytes(32)
            val blindedSecret: PublicKey = hashToCurve(secret.value) + PrivateKey(blindingFactor).publicKey()
            // val blindingKey: PrivateKey = PrivateKey(blindingFactor)
            return PreMintBundle(secret, blindedSecret, blindingFactor)
        }
    }
}

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

public fun randomBytes(size: Int): ByteArray {
    val random = SecureRandom()
    val secret = ByteArray(size)
    random.nextBytes(secret)
    return secret
}
