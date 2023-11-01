/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */

package me.tb.cashuclient.split

import kotlinx.serialization.Serializable
import me.tb.cashuclient.types.BlindedSignature

// TODO: The MintResponse and SplitResponse are the same class. We can clean this up.

/**
 * This is the object returned by the split endpoint and consists of a list of blinded signatures.
 *
 * @param promises List of blinded signatures.
 */
@Serializable
public data class SplitResponse(
    val promises: List<BlindedSignature>
)
