/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */

package me.tb.cashuclient.mint

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class MintQuoteResponse(
    @SerialName("quote") public val quoteId: String,
    public val request: String,
    public val paid: Boolean,
    public val expiry: Int
)
