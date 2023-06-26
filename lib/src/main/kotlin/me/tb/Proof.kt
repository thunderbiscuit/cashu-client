/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */
 
package me.tb

import kotlinx.serialization.Serializable

/**
 * Cashu token.
 */
@Serializable
public data class Proof(
    public val id: String,
    public val amount: ULong,
    public val secret: String,
    public val C: String,
    public val script: String? = null
)
