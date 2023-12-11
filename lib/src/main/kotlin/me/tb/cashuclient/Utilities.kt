/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */
 
package me.tb.cashuclient

import fr.acinq.bitcoin.PublicKey
import fr.acinq.bitcoin.crypto.Digest
import me.tb.cashuclient.types.SwapRequired
import java.security.SecureRandom
import kotlin.math.pow

/**
 * Given a total amount, returns the shortest list of denominations to create this total, e.g. 13 is [1, 4, 8].
 */
public fun decomposeAmount(value: ULong): List<ULong> {
    require(value != 0uL) { "Zero amounts do not make sense in this context." }
    require(value > 0uL) { "Negative amounts do not make sense in this context." }

    val chunks: MutableList<ULong> = mutableListOf()
    for (i: Int in 0 until ULong.SIZE_BITS) {
        val mask: ULong = 1uL shl i
        if ((value and mask) != 0uL) {
            chunks.add(2.0.pow(i).toULong())
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

/**
 * Use Java's SecureRandom to generate a random byte array of the given size.
 *
 * @param size The size (in bytes) of the ByteArray to generate.
 */
public fun randomBytes(size: Int): ByteArray {
    val random = SecureRandom()
    val secret = ByteArray(size)
    random.nextBytes(secret)
    return secret
}

public fun base64ToBase64UrlSafe(base64: String): String {
    return base64.replace('+', '-').replace('/', '_').replace("=", "")
}

/*
 * Given a list of available denominations and a target amount, return a [SwapRequired] that lets you know if you'll
 * need a swap or not. If you don't need a swap, the final list of denominations is returned. If you do need a swap,
 * a list of token amounts that almost add up to the target amount is returned, along with one more denomination you'll
 * need to swap in order to hit the target amount.
 *
 * TODO: This function is where a lot of the gains could be made in terms of performance and resource utilization.
 *
 * @param availableDenominations The list of denominations available to the wallet.
 * @param targetAmount The target amount to reach.
 */
public fun isSplitRequired(availableDenominations: List<ULong>, targetAmount: ULong): SwapRequired {
    val sortedDenominations = availableDenominations.sortedDescending()
    val selectedDenominations = mutableListOf<ULong>()
    var currentSum = 0uL

    for (denomination in sortedDenominations) {
        if (currentSum == targetAmount) {
            return SwapRequired.No(selectedDenominations)
        }

        if (currentSum + denomination > targetAmount) {
            val requiredAmount = targetAmount - currentSum
            return SwapRequired.Yes(selectedDenominations, denomination, requiredAmount)
        } else {
            selectedDenominations.add(denomination)
            currentSum += denomination
        }
    }

    throw Exception("Something went wrong in isSplitRequired")
}
