/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */

package me.tb.cashuclient.mint

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.tb.cashuclient.types.BlindedMessage
import me.tb.cashuclient.types.BlindedSignature
import me.tb.cashuclient.types.BlindedSignaturesResponse

/**
 * This is the object sent to the mint endpoint and consists of a list of blinded messages to sign.
 *
 * @property quoteId The quote ID received from the mint in response to a [MintQuoteRequest].
 * @property outputs List of blinded messages to be signed.
 */
@Serializable
public data class MintRequest(
    @SerialName("quote") val quoteId: String,
    val outputs: List<BlindedMessage>
)

/**
 * This is the object returned by the mint endpoint and consists of a list of blinded signatures.
 *
 * @property signatures List of blinded signatures.
 */
@Serializable
public data class MintResponse(
    override val signatures: List<BlindedSignature>
) : BlindedSignaturesResponse
