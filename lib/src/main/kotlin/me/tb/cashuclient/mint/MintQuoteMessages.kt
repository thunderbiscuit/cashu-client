/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */

package me.tb.cashuclient.mint

import fr.acinq.lightning.payment.PaymentRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.tb.cashuclient.types.PaymentRequestSerializer

@Serializable
public data class MintQuoteRequest(
    public val amount: ULong,
    public val unit: String,
)

@Serializable
public data class MintQuoteResponse(
    @SerialName("quote") public val quoteId: String,
    @Serializable(with = PaymentRequestSerializer::class) public val request: PaymentRequest,
    public val paid: Boolean,
    public val expiry: Int
)