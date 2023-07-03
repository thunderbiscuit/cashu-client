/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */
 
package me.tb.cashuclient

import kotlinx.serialization.Serializable

/**
 * The smallest unit of request to the mint endpoint.
 *
 * @param amount The amount of the token.
 * @param blindedSecret The blinded secret, referred to as B_ in the spec (NUT-00).
 */
@Serializable
public data class BlindedMessage(
    val amount: ULong,
    val blindedSecret: String
)

/**
 * This is the object sent to the mint endpoint and consists of a list of blinded messages to sign.
 *
 * NOTE: These are called PostMintRequest in the spec (NUT-04). The reason why I changed the name is that
 *       PostMintRequest describes the object in terms of _when_ it is sent (after the "mint request" described in
 *       NUT-03 has been sent) rather than what it is. `MintRequest` is, IMO, a better name.
 *       See cashu-rs which also chose that name.
 *
 * @param outputs List of blinded messages to be signed.
 */
@Serializable
public data class MintRequest(
    val outputs: List<BlindedMessage>
)