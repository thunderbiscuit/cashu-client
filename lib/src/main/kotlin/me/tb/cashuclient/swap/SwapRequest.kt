/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */

package me.tb.cashuclient.swap

import kotlinx.serialization.Serializable
import me.tb.cashuclient.types.BlindedMessage
import me.tb.cashuclient.types.Proof

@Serializable
public data class SwapRequest(
    public val inputs: List<Proof>,
    public val outputs: List<BlindedMessage>
)
