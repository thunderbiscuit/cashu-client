/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */

package me.tb.cashuclient.mint

import fr.acinq.bitcoin.Satoshi
import fr.acinq.lightning.payment.PaymentRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.tb.cashuclient.types.PaymentRequestSerializer

/**
 * In order to proceed with a mint, Alice must request a quote for a given bolt11 invoice she would like paid. This is
 * the object sent to the mint for this purpose.
 *
 * @property amount The amount to be minted.
 * @property unit   The unit the wallet is requesting tokens for.
 */
@Serializable
public data class MintQuoteRequest(
    public val amount: ULong,
    public val unit: String,
)

/**
 * The mint's response to a [MintQuoteRequest].
 *
 * @property quoteId The unique identifier for this quote.
 * @property request The bolt11 payment request for the quote.
 * @property paid    Whether the payment has been paid.
 * @property expiry  The expiry time of the quote.
 */
@Serializable
public data class MintQuoteResponse(
    @SerialName("quote") public val quoteId: String,
    @Serializable(with = PaymentRequestSerializer::class) public val request: PaymentRequest,
    public val paid: Boolean,
    public val expiry: Int
)

public data class MintQuoteData(
    val paymentAmount: Satoshi,
    val fee: Satoshi,
    val feeRate: Double,
    val quote: MintQuoteResponse
) {
    public companion object {
        public fun fromMintQuoteResponse(initialPaymentAmount: Satoshi, mintQuoteResponse: MintQuoteResponse): MintQuoteData {
            val paymentRequest: PaymentRequest = mintQuoteResponse.request
            val totalPayment: Satoshi = paymentRequest.amount?.truncateToSatoshi() ?: throw IllegalStateException("Payment request has no amount")
            val fee = totalPayment.sat - initialPaymentAmount.sat
            val feeRate = fee.toDouble() / initialPaymentAmount.sat.toDouble()

            return MintQuoteData(
                paymentAmount = initialPaymentAmount,
                fee = Satoshi(fee),
                feeRate = feeRate,
                quote = mintQuoteResponse
            )
        }
    }
}
