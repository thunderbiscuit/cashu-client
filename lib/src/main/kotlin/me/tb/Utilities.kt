package me.tb

import kotlin.math.pow

/**
 * Given a total amount, returns the shortest list of token values to create this total, e.g. 13 is [1, 4, 8].
 *
 * TODO: After seeing that the default mint on the Python cashu implementation returns
 *       keys for 64 bit integers I'm choosing Long as the type here, but keep in mind that because the amounts
 *       are a mint policy, this library will need more resilience against mints that support different token amounts.
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
