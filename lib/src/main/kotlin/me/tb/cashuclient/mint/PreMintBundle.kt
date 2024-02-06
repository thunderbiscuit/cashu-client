/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */
 
package me.tb.cashuclient.mint

import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.PublicKey
import me.tb.cashuclient.types.KeysetId
import me.tb.cashuclient.types.Secret
import me.tb.cashuclient.decomposeAmount
import me.tb.cashuclient.types.BlindedMessage
import me.tb.cashuclient.types.BlindingData
import me.tb.cashuclient.types.PreRequestBundle
import me.tb.cashuclient.types.createBlindingData

/**
 * The data bundle Alice must create prior to communicating with the mint. Once the mint sends a response,
 * this data is then combined with the [MintResponse] to create valid tokens (promises).
 *
 * @property blindingDataItems The list of [PreMintItem]s that will be sent to the mint.
 * @property keysetId          The [KeysetId] of the keyset the wallet expects will be signing the [BlindedMessage]s.
 */
public class PreMintBundle private constructor(
    override val blindingDataItems: List<PreMintItem>,
    private val quoteId: String,
    private val keysetId: KeysetId
) : PreRequestBundle {
    public fun buildMintRequest(): MintRequest {
        val outputs: List<BlindedMessage> = blindingDataItems.map { preMintItem ->
            BlindedMessage(
                amount = preMintItem.amount,
                id = keysetId.value,
                blindedSecret = preMintItem.blindedSecret.toString()
            )
        }

        return MintRequest(
            quoteId = quoteId,
            outputs = outputs
        )
    }

    public companion object {
        public fun create(value: ULong, quoteId: String, keysetId: KeysetId): PreMintBundle {
            val tokenAmounts = decomposeAmount(value)
            val preMintItems: List<PreMintItem> = tokenAmounts.map { tokenAmount ->
                PreMintItem.create(tokenAmount)
            }

            return PreMintBundle(preMintItems, quoteId, keysetId)
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
 * @property amount The amount of the token.
 * @property secret The secret x that is used in hashToCurve(x) to create Y.
 * @property blindedSecret The blinded secret B_ that is sent to the mint.
 * @property blindingFactor The blinding factor r, private key of the point R that is used to blind key Y.
 */
public class PreMintItem private constructor(
    public override val amount: ULong,
    public override val secret: Secret,
    public override val blindedSecret: PublicKey,
    public override val blindingFactor: PrivateKey
) : BlindingData {
    public companion object {
        public fun create(amount: ULong): PreMintItem {
            val (secret, blindedSecret, blindingFactor) = createBlindingData()
            return PreMintItem(amount, secret, blindedSecret, blindingFactor)
        }
    }
}
