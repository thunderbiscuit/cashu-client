/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */

package me.tb.cashuclient.melt

import kotlinx.serialization.Serializable

/**
 * Response from the mint when checking fees. This response is the maximum potential Lightning network fees the mint
 * expects is required for the payment. Note that this is likely not the actual fee that will be paid, but a slight overestimate.
 */
@Serializable
public data class CheckFeesResponse(
    public val fee: ULong,
)
