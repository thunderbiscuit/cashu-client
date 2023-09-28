/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */

package me.tb.cashuclient.melt

import fr.acinq.lightning.payment.PaymentRequest
import kotlinx.serialization.Serializable
import me.tb.cashuclient.types.Proof

/**
 * Object sent to the mint when requesting a melt.
 */
@Serializable
public data class MeltRequest(
    public val proofs: List<Proof>,
    public val paymentRequest: PaymentRequest
)
