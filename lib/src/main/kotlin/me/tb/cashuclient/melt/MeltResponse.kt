/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */

package me.tb.cashuclient.melt

import kotlinx.serialization.Serializable

/**
 * Response from the mint when melting tokens.
 */
@Serializable
public data class MeltResponse(
    public val paid: Boolean,
    public val preimage: String
)
