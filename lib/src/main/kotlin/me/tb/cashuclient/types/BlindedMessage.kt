/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */
 
package me.tb.cashuclient.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The smallest unit of request to the mint endpoint.
 *
 * @param amount The amount of the token.
 * @param blindedSecret The blinded secret, referred to as B_ in the spec (NUT-00).
 */
@Serializable
public data class BlindedMessage(
    val amount: ULong,
    @SerialName("B_") val blindedSecret: String
)
