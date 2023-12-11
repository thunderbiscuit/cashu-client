/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */

package me.tb.cashuclient

import me.tb.cashuclient.types.Keyset
import me.tb.cashuclient.types.KeysetId
import java.lang.IllegalArgumentException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KeysetTest {
    @Test
    fun `Build keyset from json object`() {
        val jsonString = """{"1":"03ba786a2c0745f8c30e490288acd7a72dd53d65afd292ddefa326a4a3fa14c566","2":"03361cd8bd1329fea797a6add1cf1990ffcf2270ceb9fc81eeee0e8e9c1bd0cdf5","4":"036e378bcf78738ddf68859293c69778035740e41138ab183c94f8fee7572214c7"}"""
        Keyset.fromJson(jsonString)
    }

    // The key #2 is missing its last byte.
    @Test
    fun `Attempting to build keyset from malformed json object`() {
        assertFailsWith<IllegalArgumentException> {
            val jsonString = """{"1":"03ba786a2c0745f8c30e490288acd7a72dd53d65afd292ddefa326a4a3fa14c566","2":"03361cd8bd1329fea797a6add1cf1990ffcf2270ceb9fc81eeee0e8e9c1bd0cd","4":"036e378bcf78738ddf68859293c69778035740e41138ab183c94f8fee7572214c7"}"""
            Keyset.fromJson(jsonString)
        }
    }

    // The key #4 is not encoded in compressed format.
    @Test
    fun `Attempting to build keyset with uncompressed public key`() {
        assertFailsWith<IllegalArgumentException> {
            val jsonString = """{"1":"03ba786a2c0745f8c30e490288acd7a72dd53d65afd292ddefa326a4a3fa14c566","2":"03361cd8bd1329fea797a6add1cf1990ffcf2270ceb9fc81eeee0e8e9c1bd0cd","4":"04fd4ce5a16b65576145949e6f99f445f8249fee17c606b688b504a849cdc452de3625246cb2c27dac965cb7200a5986467eee92eb7d496bbf1453b074e223e481"}"""
            Keyset.fromJson(jsonString)
        }
    }

    @Test
    fun `Derive keyset id`() {
        val keyset: Keyset = Keyset.fromJson(TEST_KEYSET)
        assertEquals<String>(expected = "000f01df73ea149a", actual = keyset.keysetId.value)
    }

    @Test
    fun `Get version byte from keyset id`() {
        val keyset: KeysetId = KeysetId("009a1f293253e41e")
        assertEquals<String>(expected = "00", actual = keyset.versionByte())
    }
}
