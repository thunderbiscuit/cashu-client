/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */
 
package me.tb.cashuclient.split

import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.PublicKey
import me.tb.cashuclient.Secret
import me.tb.cashuclient.db.DBProof
import me.tb.cashuclient.db.DBSettings
import me.tb.cashuclient.decomposeAmount
import me.tb.cashuclient.types.BlindedMessage
import me.tb.cashuclient.types.BlindingData
import me.tb.cashuclient.types.PreRequestBundle
import me.tb.cashuclient.types.Proof
import me.tb.cashuclient.types.createBlindingData
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * The data bundle Alice must create prior to communicating with the mint requesting a split. Once the mint sends a
 * response [SplitResponse], the data from this [PreSplitBundle] object is combined with it to create valid tokens
 * (promises).
 *
 * @property proofToSplit The proof the wallet intends to send to the mint for splitting.
 * @property blindingDataItems The list of [PreSplitItem]s that will be used to create the [BlindedMessage]s sent to the
 * mint.
 */
public class PreSplitBundle private constructor(
    public val proofToSplit: Proof,
    public override val blindingDataItems: List<PreSplitItem>
) : PreRequestBundle {
    public fun buildSplitRequest(): SplitRequest {
        val outputs: List<BlindedMessage> = blindingDataItems.map { preSplitItem ->
            BlindedMessage(
                amount = preSplitItem.amount,
                blindedSecret = preSplitItem.blindedSecret.toString()
            )
        }

        return SplitRequest(
            proofToSplit,
            outputs
        )
    }

    public companion object {
        /**
         * Creates a [PreSplitBundle] from a denomination the user wishes to split and a target amount.
         *
         * @param denominationToSplit The denomination we wish to use to create the split.
         * @param requiredAmount The target amount.
         */
        public fun create(
            denominationToSplit: ULong,
            requiredAmount: ULong,
        ): PreSplitBundle {
            val requiredDenominations: List<ULong> = decomposeAmount(requiredAmount)
            val changeDenominations: List<ULong> = decomposeAmount(denominationToSplit - requiredAmount)
            val requestDenominations = requiredDenominations + changeDenominations

            val preSplitItems: List<PreSplitItem> = requestDenominations.map { amount ->
                PreSplitItem.create(
                    amount = amount,
                )
            }

            // Go get a proof in the database for the denomination required
            DBSettings.db
            val proof: Proof = transaction {
                SchemaUtils.create(DBProof)
                val proof: Proof? = DBProof.select { DBProof.amount eq denominationToSplit }.firstOrNull()?.let {
                    Proof(
                        amount = it[DBProof.amount],
                        secret = it[DBProof.secret],
                        C = it[DBProof.C],
                        id = it[DBProof.id],
                        script = it[DBProof.script]
                    )
                }
                proof ?: throw Exception("No proof found for denomination $denominationToSplit")
            }

            return PreSplitBundle(proof, preSplitItems)
        }
    }
}

/**
 * The data structures that get combined into a [PreSplitBundle] required to build a [SplitRequest], and are combined
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
public class PreSplitItem private constructor(
    public override val amount: ULong,
    public override val secret: Secret,
    public override val blindedSecret: PublicKey,
    public override val blindingFactor: PrivateKey
) : BlindingData {
    public companion object {
        public fun create(amount: ULong): PreSplitItem {
            val (secret, blindedSecret, blindingFactor) = createBlindingData()
            return PreSplitItem(amount, secret, blindedSecret, blindingFactor)
        }
    }
}
