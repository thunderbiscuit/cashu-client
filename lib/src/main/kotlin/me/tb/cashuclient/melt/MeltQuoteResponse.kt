/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */

package me.tb.cashuclient.melt

import kotlinx.serialization.Serializable

// TODO: Look at making the expiry timestamp property typesafe.

/**
 * This is the quote returned by the mint in response to a [MeltQuoteRequest].
 *
 * @property quote      The quote ID.
 * @property amount     The amount that must be provided (this amount includes fees but not the fee reserve).
 * @property feeReserve The additional fee reserve required.
 * @property paid       Whether the invoice has been paid.
 * @property expiry     A Unix timestamp until which the melt quote is valid.
 */
@Serializable
public data class MeltQuoteResponse(
    public val quote: String,
    public val amount: ULong,
    public val feeReserve: ULong,
    public val paid: Boolean,
    public val expiry: ULong,
)
