/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */

package me.tb.cashuclient.melt

import fr.acinq.lightning.payment.PaymentRequest
import kotlinx.serialization.Serializable
import me.tb.cashuclient.types.EcashUnit
import me.tb.cashuclient.types.PaymentRequestSerializer

/**
 * In order to proceed with a melt, Alice must request a quote for a given bolt11 invoice she would like paid. This is
 * the object sent to the mint for this purpose.
 *
 * @property request The bolt11 Lightning invoice to be paid.
 * @property unit    The unit the wallet would like to pay with.
 */
@Serializable
public data class MeltQuoteRequest(
    @Serializable(with = PaymentRequestSerializer::class) public val request: PaymentRequest,
    public val unit: EcashUnit = EcashUnit.SAT,
)
