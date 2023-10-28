/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */
 
package me.tb.cashuclient

import fr.acinq.bitcoin.PublicKey
import fr.acinq.bitcoin.crypto.Digest
import java.security.SecureRandom
import kotlin.math.pow

/**
 * Given a total amount, returns the shortest list of token values to create this total, e.g. 13 is [1, 4, 8].
 * Note: this function is not related with the split operation that happens in the wallet.
 */
public fun splitAmount(value: ULong): List<ULong> {
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
 * Given a list of available token amounts and a target amount, returns a [SplitRequired] that lets you know if you'll
 * need a split or not. If you don't need a split, the final list of token amounts is returned. If you do need a split,
 * a list of token amounts that almost add up to the target amount is returned, along with the list of denominations
 * you're missing to reach the target amount.
 *
 * TODO: This function is where a lot of the gains could be made in terms of performance and resource utilization.
 *
 * @param availableDenominations The list of denominations available to the wallet.
 * @param targetAmount The target amount to reach.
 */
public fun isSplitRequired(availableDenominations: List<ULong>, targetAmount: ULong): SplitRequired {
    var remainingAmount: ULong = targetAmount
    val finalList: MutableList<ULong> = mutableListOf()

    for (availableAmount in availableDenominations) {
        if (remainingAmount == 0.toULong()) {
            return SplitRequired.No(finalList)
        }

        if (availableAmount <= remainingAmount) {
            finalList.add(availableAmount)
            remainingAmount -= availableAmount
        } else if (availableAmount > remainingAmount) {
            // Small optimization in case we get lucky: if the remaining amount is in the list of available amounts,
            // we can just add it and return the final list.
            if (remainingAmount in availableDenominations) {
                finalList.add(remainingAmount)
                remainingAmount -= remainingAmount
            } else {
                val requiredDenominations = splitAmount(remainingAmount)
                return SplitRequired.Yes(finalList, requiredDenominations)
            }
        }
    }

    throw Exception("Something went wrong in isSplitRequired")
}

public sealed class SplitRequired {
    public data class No(
        val finalList: List<ULong>
    ): SplitRequired()

    public data class Yes(
        val almostFinishedList: List<ULong>,
        val requiredTokens: List<ULong>,
    ): SplitRequired()
}
