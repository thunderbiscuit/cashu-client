/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */

package me.tb.cashuclient.melt

import fr.acinq.lightning.payment.PaymentRequest
import kotlinx.serialization.Serializable

/**
 * Request to the mint to check fees for a given BOLT-11 Lightning invoice.
 */
@Serializable
public data class CheckFeesRequest(
    public val pr: PaymentRequest
)
