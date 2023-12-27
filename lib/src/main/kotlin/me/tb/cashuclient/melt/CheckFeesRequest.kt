/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */

package me.tb.cashuclient.melt

import fr.acinq.lightning.payment.PaymentRequest
import kotlinx.serialization.Serializable
import me.tb.cashuclient.types.PaymentRequestSerializer

/**
 * Request to the mint to check fees for a given BOLT-11 Lightning invoice.
 */
@Serializable
public data class CheckFeesRequest(
    @Serializable(with = PaymentRequestSerializer::class)
    public val pr: PaymentRequest
)
