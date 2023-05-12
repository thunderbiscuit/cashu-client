package me.tb

import java.lang.IllegalArgumentException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UtilitiesTest {
    @Test
    fun `splitAmount correctly splits small amounts`() {
        assertEquals(splitAmount(13), listOf(1, 4, 8))
    }

    @Test
    fun `splitAmount correctly splits large amounts`() {
        assertEquals(splitAmount(2147483647), listOf(1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432, 67108864, 134217728, 268435456, 536870912, 1073741824))
    }

    @Test
    fun `splitAmount throws an exception on 0 amount values`() {
        assertFailsWith<IllegalArgumentException> {
            splitAmount(0)
        }
    }

    @Test
    fun `splitAmount throws an exception on negative amount values`() {
        assertFailsWith<IllegalArgumentException> {
            splitAmount(-1)
        }
    }
}
