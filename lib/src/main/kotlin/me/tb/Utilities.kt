package me.tb

import fr.acinq.bitcoin.PublicKey
import fr.acinq.bitcoin.crypto.Digest
import java.security.SecureRandom
import kotlin.math.pow

/**
 * Given a total amount, returns the shortest list of token values to create this total, e.g. 13 is [1, 4, 8].
 */
public fun splitAmount(value: Long): List<Long> {
    require(value != 0L) { "Zero amounts do not make sense in this context." }
    require(value > 0L) { "Negative amounts do not make sense in this context." }

    val chunks: MutableList<Long> = mutableListOf()
    for (i: Int in 0 until 32) {
        val mask: Long = 1L shl i
        if ((value and mask) != 0L) {
            chunks.add(2.0.pow(i).toLong())
        }
    }
    return chunks
}

/**
 * Given a message, deterministically return a valid point on the secp256k1 curve.
 */
public fun hashToCurve(message: ByteArray): PublicKey {
    var point: PublicKey? = null
    var msgToHash: ByteArray = message

    while (point == null || !point.isValid()) {
        val hash: ByteArray = Digest.sha256().hash(msgToHash)

        // The point on the curve will always have an even y-coordinate (0x02 prefix)
        // For more information as to why this doesn't impact security, see: https://github.com/cashubtc/nuts/issues/24
        point = PublicKey(byteArrayOf(0x02) + hash)

        // If the hash of the message did not produce a valid point, we hash the hash and try again
        if (!point.isValid()) {
            msgToHash = hash
        }
    }
    return point
}

public fun randomBytes(size: Int): ByteArray {
    val random = SecureRandom()
    val secret = ByteArray(size)
    random.nextBytes(secret)
    return secret
}
