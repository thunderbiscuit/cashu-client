/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */
 
package me.tb.cashuclient

import fr.acinq.bitcoin.PublicKey
import fr.acinq.bitcoin.crypto.Digest
import me.tb.cashuclient.types.SwapRequired
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
    val domainSeparator: ByteArray = "Secp256k1_HashToCurve_Cashu_".toByteArray()
    val msgToHash: ByteArray = Digest.sha256().hash(domainSeparator + message)
    var counter: UInt = 0u

    while (point == null || !point.isValid()) {
        val counterBytes = ByteBuffer.allocate(Long.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putLong(counter.toLong())
            .array()
            .take(4)
            .toByteArray()

        val hash: ByteArray = Digest.sha256().hash(msgToHash + counterBytes)

        // The point on the curve will always have an even y-coordinate (0x02 prefix)
        // For more information as to why this doesn't impact security, see: https://github.com/cashubtc/nuts/issues/24
        point = PublicKey(byteArrayOf(0x02) + hash)

        // If the hash of the message did not produce a valid point, we bump the counter and try again
        if (!point.isValid()) {
            counter++
        }
        // TODO: If we've tried 2^32 times and still haven't found a valid point, we throw an error
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

// public fun base64ToBase64UrlSafe(base64: String): String {
//     return base64.replace('+', '-').replace('/', '_').replace("=", "")
// }

// TODO: This function is where a lot of the gains could be made in terms of performance and resource utilization.
/**
 * Given a list of available denominations and a target amount, return a [SwapRequired] that lets you know if you'll
 * need a swap or not. If you don't need a swap, the final list of denominations is returned. If you do need a swap,
 * a list of token amounts that almost add up to the target amount is returned, along with one more denomination you'll
 * need to swap in order to hit the target amount.
 *
 * @param allDenominations The list of denominations available to the wallet.
 * @param targetAmount     The target amount to reach.
 */
public fun isSwapRequired(allDenominations: List<ULong>, targetAmount: ULong): SwapRequired {
    val sortedDenominations = allDenominations.sortedDescending()
    val availableDenominations = sortedDenominations.toMutableList()
    val selectedDenominations = mutableListOf<ULong>()
    var currentSum = 0uL

    for (denomination in sortedDenominations) {
        if (currentSum == targetAmount) {
            return SwapRequired.No(selectedDenominations)
        }

        if (currentSum + denomination > targetAmount) {
            val requiredAmount = targetAmount - currentSum
            return SwapRequired.Yes(
                requiredAmount = requiredAmount,
                almostFinishedList = selectedDenominations,
                availableForSwap = availableDenominations.toList(),
            )
        } else {
            moveFromAvailableToSelected(denomination, availableDenominations, selectedDenominations)
            currentSum += denomination
        }
    }

    throw Exception("Something went wrong in isSplitRequired")
}

private fun moveFromAvailableToSelected(
    denomination: ULong,
    availableDenominations: MutableList<ULong>,
    selectedDenominations: MutableList<ULong>,
) {
    availableDenominations.remove(denomination)
    selectedDenominations.add(denomination)
}
