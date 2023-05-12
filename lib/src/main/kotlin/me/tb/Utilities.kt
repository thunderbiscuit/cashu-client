package me.tb

import kotlin.math.pow

/**
 * Given a total amount, returns the shortest list of token values to create this total, e.g. 13 is [1, 4, 8].
 * TODO: Is Int the correct type here? The maximum Int number is 2147483647, i.e. 0.02147483647 bitcoin.
 *       Is there a maximum amount for any given token? Is that a mint policy?
 */
public fun splitAmount(value: Int): List<Int> {
    require(value != 0) { "Zero amounts do not make sense in this context." }
    require(value < 0) { "Negative amounts do not make sense in this context." }

    val chunks: MutableList<Int> = mutableListOf()
    for (i in 0 until 32) {
        val mask: Int = 1 shl i
        if ((value and mask) != 0) {
            chunks.add(2.0.pow(i).toInt())
        }
    }
    return chunks
}
