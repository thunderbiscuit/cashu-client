package me.tb.cashuclient

import fr.acinq.bitcoin.PublicKey
import fr.acinq.secp256k1.Hex
import me.tb.cashuclient.hashToCurve
import me.tb.cashuclient.splitAmount
import java.lang.IllegalArgumentException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UtilitiesTest {
    @Test
    fun `splitAmount correctly splits small amounts`() {
        assertEquals<List<ULong>>(
            expected = listOf<ULong>(1uL, 4uL, 8uL),
            actual = splitAmount(13uL)
        )
    }

    // ULong.MAX_VALUE = 18446744073709551615
    @Test
    fun `splitAmount correctly splits large amounts`() {
        assertEquals<List<ULong>>(
            expected = listOf(1uL, 2uL, 4uL, 8uL, 16uL, 32uL, 64uL, 128uL, 256uL, 512uL, 1024uL, 2048uL, 4096uL, 8192uL, 16384uL, 32768uL, 65536uL, 131072uL, 262144uL, 524288uL, 1048576uL, 2097152uL, 4194304uL, 8388608uL, 16777216uL, 33554432uL, 67108864uL, 134217728uL, 268435456uL, 536870912uL, 1073741824uL, 2147483648uL, 4294967296UL, 8589934592UL, 17179869184UL, 34359738368UL, 68719476736UL, 137438953472UL, 274877906944UL, 549755813888UL, 1099511627776uL, 2199023255552uL, 4398046511104uL, 8796093022208uL, 17592186044416uL, 35184372088832uL, 70368744177664uL, 140737488355328uL, 281474976710656uL, 562949953421312uL, 1125899906842624uL, 2251799813685248uL, 4503599627370496uL, 9007199254740992uL, 18014398509481984uL, 36028797018963968uL, 72057594037927936uL, 144115188075855872uL, 288230376151711744uL, 576460752303423488uL, 1152921504606846976uL, 2305843009213693952uL, 4611686018427387904uL, 9223372036854775808uL),
            actual = splitAmount(18446744073709551615uL)
        )
    }

    @Test
    fun `splitAmount throws an exception on 0 amount values`() {
        assertFailsWith<IllegalArgumentException> {
            splitAmount(0uL)
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
            expected = "0266687aadf862bd776c8fc18b8e9f8e20089714856ee233b3902a591d0d5f2925",
            actual = point.toHex()
        )
    }

    @Test
    fun `hashToCurve returns correct point 2`() {
        val point: PublicKey = hashToCurve(Hex.decode("0000000000000000000000000000000000000000000000000000000000000001"))
        assertEquals<String>(
            expected = "02ec4916dd28fc4c10d78e287ca5d9cc51ee1ae73cbfde08c6b37324cbfaac8bc5",
            actual = point.toHex()
        )
    }

    @Test
    fun `hashToCurve returns correct point after some iterations`() {
        val point: PublicKey = hashToCurve(Hex.decode("0000000000000000000000000000000000000000000000000000000000000002"))
        assertEquals<String>(
            expected = "02076c988b353fcbb748178ecb286bc9d0b4acf474d4ba31ba62334e46c97c416a",
            actual = point.toHex()
        )
    }
}
