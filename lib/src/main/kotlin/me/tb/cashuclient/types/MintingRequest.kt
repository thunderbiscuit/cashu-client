/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */

package me.tb.cashuclient.types

import kotlinx.serialization.Serializable

/**
 * This is the object sent to the mint endpoint and consists of a list of blinded messages to sign.
 *
 * NOTE: These are called PostMintRequest in the specification (NUT-04). The reason why I changed the name is that
 *       PostMintRequest describes the object in terms of _when_ it is sent (after the "mint request" described in
 *       NUT-03 has been sent) rather than what it is. `MintingRequest` is, IMO, a better name, and conveys its purpose,
 *       particularly when coupled with its sibling object, `MintingResponse`.
 *
 * @param outputs List of blinded messages to be signed.
 */
@Serializable
public data class MintingRequest(
    val outputs: List<BlindedMessage>
)
