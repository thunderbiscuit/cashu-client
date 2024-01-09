/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */

package me.tb.cashuclient.melt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.tb.cashuclient.types.Proof

/**
 * Message sent to the mint when requesting a melt.
 *
 * @property quoteId The quote ID received from the mint in response to a [MeltQuoteRequest].
 * @property proofs  The list of [Proof]s that will be sent to the mint to pay for the melt.
 */
@Serializable
public data class MeltRequest(
    @SerialName("quote") public val quoteId: String,
    @SerialName("inputs") public val proofs: List<Proof>,
)

/**
 * Response from the mint when melting tokens.
 *
 * @property paid     Whether the payment was made successfully.
 * @property preimage The preimage of the payment if it was paid successfully.
 */
@Serializable
public data class MeltResponse(
    public val paid: Boolean,
    @SerialName("payment_preimage") public val preimage: String?
)
