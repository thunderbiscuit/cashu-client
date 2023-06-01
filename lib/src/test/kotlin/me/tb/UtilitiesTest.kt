package me.tb

import fr.acinq.bitcoin.PublicKey
import fr.acinq.secp256k1.Hex
import java.lang.IllegalArgumentException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UtilitiesTest {
    @Test
    fun `splitAmount correctly splits small amounts`() {
        assertEquals<List<Long>>(
            expected = listOf<Long>(1, 4, 8),
            actual = splitAmount(13L)
        )
    }

    // Long.MAX_VALUE = 9223372036854775807L
    @Test
    fun `splitAmount correctly splits large amounts`() {
        assertEquals<List<Long>>(
            expected = listOf(1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432, 67108864, 134217728, 268435456, 536870912, 1073741824, 2147483648),
            actual = splitAmount(9223372036854775807L)
        )
    }

    @Test
    fun `splitAmount throws an exception on 0 amount values`() {
        assertFailsWith<IllegalArgumentException> {
            splitAmount(0L)
        }
    }

    @Test
    fun `splitAmount throws an exception on negative amount values`() {
        assertFailsWith<IllegalArgumentException> {
            splitAmount(-1L)
        }
    }

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
