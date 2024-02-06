/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */
 
package me.tb.cashuclient.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The smallest unit of request to the mint endpoint.
 *
 * @property amount The amount of the token.
 * @property id     The identifier of the keyset from which we expect a signature,
 * @property blindedSecret The blinded secret, referred to as B_ in the spec (NUT-00).
 */
@Serializable
public data class BlindedMessage(
    val amount: ULong,
    val id: String,
    @SerialName("B_") val blindedSecret: String
)
