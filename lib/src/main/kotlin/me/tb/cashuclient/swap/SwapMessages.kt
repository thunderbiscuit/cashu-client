/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */

package me.tb.cashuclient.swap

import kotlinx.serialization.Serializable
import me.tb.cashuclient.types.BlindedMessage
import me.tb.cashuclient.types.BlindedSignature
import me.tb.cashuclient.types.BlindedSignaturesResponse
import me.tb.cashuclient.types.Proof

@Serializable
public data class SwapRequest(
    public val inputs: List<Proof>,
    public val outputs: List<BlindedMessage>
)

// TODO: The MintResponse and SwapResponse are the same class. We can clean this up.

/**
 * This is the object returned by the swap endpoint and consists of a list of blinded signatures.
 *
 * @param signatures List of blinded signatures.
 */
@Serializable
public data class SwapResponse(
    override val signatures: List<BlindedSignature>
) : BlindedSignaturesResponse
