/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */

package me.tb.cashuclient

import fr.acinq.bitcoin.PublicKey
import fr.acinq.secp256k1.Hex
import me.tb.cashuclient.types.SwapRequired
import java.lang.IllegalArgumentException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UtilitiesTest {
    @Test
    fun `splitAmount correctly splits small amounts`() {
        assertEquals<List<ULong>>(
            expected = listOf<ULong>(1uL, 4uL, 8uL),
            actual = decomposeAmount(13uL)
        )
    }

    // ULong.MAX_VALUE = 18446744073709551615
    @Test
    fun `splitAmount correctly splits large amounts`() {
        assertEquals<List<ULong>>(
            expected = listOf(1uL, 2uL, 4uL, 8uL, 16uL, 32uL, 64uL, 128uL, 256uL, 512uL, 1024uL, 2048uL, 4096uL, 8192uL, 16384uL, 32768uL, 65536uL, 131072uL, 262144uL, 524288uL, 1048576uL, 2097152uL, 4194304uL, 8388608uL, 16777216uL, 33554432uL, 67108864uL, 134217728uL, 268435456uL, 536870912uL, 1073741824uL, 2147483648uL, 4294967296UL, 8589934592UL, 17179869184UL, 34359738368UL, 68719476736UL, 137438953472UL, 274877906944UL, 549755813888UL, 1099511627776uL, 2199023255552uL, 4398046511104uL, 8796093022208uL, 17592186044416uL, 35184372088832uL, 70368744177664uL, 140737488355328uL, 281474976710656uL, 562949953421312uL, 1125899906842624uL, 2251799813685248uL, 4503599627370496uL, 9007199254740992uL, 18014398509481984uL, 36028797018963968uL, 72057594037927936uL, 144115188075855872uL, 288230376151711744uL, 576460752303423488uL, 1152921504606846976uL, 2305843009213693952uL, 4611686018427387904uL, 9223372036854775808uL),
            actual = decomposeAmount(18446744073709551615uL)
        )
    }

    @Test
    fun `splitAmount throws an exception on 0 amount values`() {
        assertFailsWith<IllegalArgumentException> {
            decomposeAmount(0uL)
        }
    }

    // @Test
    // fun `splitAmount throws an exception on negative amount values`() {
    //     assertFailsWith<IllegalArgumentException> {
    //         splitAmount(-1)
    //     }
    // }

    @Test
    fun `hashToCurve returns correct point 1`() {
        val point: PublicKey = hashToCurve(Hex.decode("0000000000000000000000000000000000000000000000000000000000000000"))
        assertEquals<String>(
            expected = "024cce997d3b518f739663b757deaec95bcd9473c30a14ac2fd04023a739d1a725",
            actual = point.toHex()
        )
    }

    @Test
    fun `hashToCurve returns correct point 2`() {
        val point: PublicKey = hashToCurve(Hex.decode("0000000000000000000000000000000000000000000000000000000000000001"))
        assertEquals<String>(
            expected = "022e7158e11c9506f1aa4248bf531298daa7febd6194f003edcd9b93ade6253acf",
            actual = point.toHex()
        )
    }

    @Test
    fun `hashToCurve returns correct point after some iterations`() {
        val point: PublicKey = hashToCurve(Hex.decode("0000000000000000000000000000000000000000000000000000000000000002"))
        assertEquals<String>(
            expected = "026cdbe15362df59cd1dd3c9c11de8aedac2106eca69236ecd9fbe117af897be4f",
            actual = point.toHex()
        )
    }

    @Test
    fun `isSplitRequired works as intended`() {
        val denominations = listOf<ULong>(64uL, 32uL, 16uL, 16uL, 4uL, 4uL, 4uL, 1uL)
        val splitRequired = isSwapRequired(denominations, 87uL)

        assertEquals(
            expected = SwapRequired.Yes(requiredAmount = 23uL, almostFinishedList = listOf(64uL), availableForSwap = listOf<ULong>(32uL, 16uL, 16uL, 4uL, 4uL, 4uL, 1uL)),
            actual = splitRequired
        )
    }
}
