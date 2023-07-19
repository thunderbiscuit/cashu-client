/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */
 
package me.tb.cashuclient

import fr.acinq.lightning.payment.PaymentRequest
import kotlinx.serialization.Serializable

@Serializable
public data class InvoiceResponse(
    public val pr: String,
    public val hash: String
) {
    init {
        println("InvoiceResponse from the mint is: $pr")
    }
    // TODO: The ACINQ library cannot parse payment requests coming from
    //       https://mutinynet-cashu.thesimplekid.space or https://testnut.cashu.space
    //       so there is not checking that the payment request is valid at this point.
    // public val paymentRequest: PaymentRequest = PaymentRequest.read(pr)
}
