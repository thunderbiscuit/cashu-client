/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */

package me.tb.cashuclient.types

import kotlinx.serialization.Serializable

/**
 * This is the object returned by the mint endpoint and consists of a list of blinded signatures.
 *
 * @param promises List of blinded signatures.
 */
@Serializable
public data class MintingResponse(
    val promises: List<BlindedSignature>
)
