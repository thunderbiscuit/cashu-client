/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */

package me.tb.cashuclient.mint

import kotlinx.serialization.Serializable

@Serializable
public data class MintQuoteRequest(
    public val amount: ULong,
    public val unit: String,
)
