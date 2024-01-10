/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */

package me.tb.cashuclient.types

import fr.acinq.bitcoin.Satoshi
import fr.acinq.lightning.payment.PaymentRequest
import me.tb.cashuclient.mint.MintQuoteResponse

public data class MintQuoteData(
    val paymentAmount: Satoshi,
    val fee: Satoshi,
    val feeRate: Double,
    val quote: MintQuoteResponse
) {
    public companion object {
        public fun fromMintQuoteResponse(initialPaymentAmount: Satoshi, mintQuoteResponse: MintQuoteResponse): MintQuoteData {
            println("Inside the fromMintQuoteResponse mint quote response is $mintQuoteResponse")
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
