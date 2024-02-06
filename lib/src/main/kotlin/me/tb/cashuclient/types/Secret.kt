/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */
 
package me.tb.cashuclient.types

import me.tb.cashuclient.randomBytes

/**
 * Secret used to generate a token. This is the x in NUT-00, the bytes we'll use in the [me.tb.cashuclient.hashToCurve]
 * function to generate key Y.
 *
 * @property value The secret to use. Secrets are always 32 bytes of randomness.
 */
public class Secret {
    public val value: ByteArray = randomBytes(32)

    @OptIn(ExperimentalStdlibApi::class)
    public fun toHex(): String {
        return value.toHexString()
    }
}
