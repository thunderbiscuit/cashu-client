/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */
 
package me.tb.cashuclient.mint

import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.PublicKey
import me.tb.cashuclient.Secret
import me.tb.cashuclient.hashToCurve
import me.tb.cashuclient.randomBytes
import me.tb.cashuclient.decomposeAmount
import me.tb.cashuclient.types.BlindedMessage

/**
 * The data bundle Alice must create prior to communicating with the mint. Once the mint sends a response,
 * this data is then combined with the [MintResponse] to create valid tokens (promises).
 *
 * @property preMintItems The list of [PreMintItem]s that will be sent to the mint.
 */
public class PreMintBundle private constructor(
    public val preMintItems: List<PreMintItem>
) {
    public fun buildMintRequest(): MintRequest {
        val outputs: List<BlindedMessage> = preMintItems.map { preMintItem ->
            BlindedMessage(
                amount = preMintItem.amount,
                blindedSecret = preMintItem.blindedSecret.toString()
            )
        }

        return MintRequest(outputs = outputs)
    }

    public companion object {
        public fun create(value: ULong): PreMintBundle {
            val tokenAmounts = decomposeAmount(value)
            val preMintItems: List<PreMintItem> = tokenAmounts.map { tokenAmount ->
                PreMintItem.create(tokenAmount)
            }

            return PreMintBundle(preMintItems)
        }
    }
}

/**
 * The data structures that get combined into a [PreMintBundle], required to build a [MintRequest], and are combined
 * with the mint's response to create [Proof]s.
 *
 * The amount and blindedSecret are required to build the [BlindedMessage]s that are sent to the mint and which the mint
 * sign. Upon return, the signed [BlindedSignature]s are unblinded using the blindingFactor and, combined with the
 * secret, stored as [Proof]s in the database.
 *
 * @param amount The amount of the token.
 * @param secret The secret x that is used in hashToCurve(x) to create Y.
 * @param blindedSecret The blinded secret B_ that is sent to the mint.
 * @param blindingFactor The blinding factor r, private key of the point R that is used to blind key Y.
 */
public class PreMintItem private constructor(
    public val amount: ULong,
    public val secret: Secret,
    public val blindedSecret: PublicKey,
    public val blindingFactor: PrivateKey
) {
    public companion object {
        public fun create(amount: ULong): PreMintItem {
            val secret = Secret()
            val blindingFactorBytes = randomBytes(32)
            val blindingFactor: PrivateKey = PrivateKey(blindingFactorBytes)
            val blindedSecret: PublicKey = hashToCurve(secret.value) + blindingFactor.publicKey()

            return PreMintItem(amount, secret, blindedSecret, blindingFactor)
        }
    }
}
