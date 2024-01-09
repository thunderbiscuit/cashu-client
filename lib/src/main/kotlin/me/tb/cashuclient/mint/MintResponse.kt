/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */

package me.tb.cashuclient.mint

import kotlinx.serialization.Serializable
import me.tb.cashuclient.types.BlindedSignature
import me.tb.cashuclient.types.BlindedSignaturesResponse

/**
 * This is the object returned by the mint endpoint and consists of a list of blinded signatures.
 *
 * @property signatures List of blinded signatures.
 */
@Serializable
public data class MintResponse(
    override val signatures: List<BlindedSignature>
) : BlindedSignaturesResponse
