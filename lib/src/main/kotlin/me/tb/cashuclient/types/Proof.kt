/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */
 
package me.tb.cashuclient.types

import kotlinx.serialization.Serializable

/**
 * A proof as defined in NUT-00, also known as a Cashu note.
 */
@Suppress("PropertyName")
@Serializable
public data class Proof(
    public val amount: ULong,
    public val id: String,
    public val secret: String,
    public val C: String,
    public val script: String? = null
)
