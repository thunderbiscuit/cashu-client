/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */
 
package me.tb.cashuclient.types

import fr.acinq.bitcoin.PublicKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This is the smallest unit of data returned by the mint endpoint. These aggregate into a
 * [me.tb.cashuclient.mint.MintResponse] object.
 *
 * @property amount     The value of the token.
 * @property id         The id of the keyset of the mint that signed the token.
 * @property blindedKey The blinded signature, a point on the curve referred to as C_ in the spec (NUT-00).
 */
@Serializable
public data class BlindedSignature(
    val amount: ULong,
    val id: String,
    @SerialName("C_") val blindedKey: String,
) {
    init {
        require(PublicKey.fromHex(blindedKey).isValid()) { "Invalid blinded key: $blindedKey" }
    }
}
