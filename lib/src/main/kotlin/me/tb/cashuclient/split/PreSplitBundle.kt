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
import me.tb.cashuclient.hashToCurve
import me.tb.cashuclient.randomBytes
import me.tb.cashuclient.types.BlindedMessage
import me.tb.cashuclient.types.Proof
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * The data bundle Alice must create prior to communicating with the mint requesting a split. Once the mint sends a
 * response [SplitResponse], the data from this [PreSplitBundle] object is combined with it to create valid tokens
 * (promises).
 *
 * @property preSplitProofs The list of proofs the wallet intends to send to the mint for splitting.
 * @property preSplitItems The list of
 */
public class PreSplitBundle private constructor(
    public val preSplitProofs: List<Proof>,
    public val preSplitItems: List<PreSplitItem>
) {
    public fun buildSplitRequest(): SplitRequest {
        val outputs: List<BlindedMessage> = preSplitItems.map { preSplitItem ->
            BlindedMessage(
                amount = preSplitItem.amount,
                blindedSecret = preSplitItem.blindedSecret.toString()
            )
        }

        return SplitRequest(
            preSplitProofs,
            outputs
        )
    }

    public companion object {
        /**
         * Creates a [PreSplitBundle] from a list of required denominations and available denominations.
         *
         * @param allAvailableDenominations The list of all available amounts that can be used to create the split.
         * @param requiredDenominations The list of required token denominations.
         */
        public fun create(
            allAvailableDenominations: List<ULong>,
            requiredDenominations: List<ULong>,
        ): PreSplitBundle {
            val totalRequired: ULong = requiredDenominations.sum()
            val denominationsToUse = chooseDenominations(allAvailableDenominations, totalRequired)
            val preSplitItems: List<PreSplitItem> = requiredDenominations.map { tokenAmount ->
                PreSplitItem.create(
                    amount = tokenAmount,
                    secret = Secret(),
                    blindingFactorBytes = null
                )
            }

            // Go get proofs in the database for the amounts required
            DBSettings.db
            // Check if we have these amounts in the database
            val proofs: List<Proof> = denominationsToUse.map { amt ->
                transaction {
                    SchemaUtils.create(DBProof)
                    val proof: Proof? = DBProof.select { DBProof.amount eq amt }.firstOrNull()?.let {
                        Proof(
                            amount = it[DBProof.amount],
                            secret = it[DBProof.secret],
                            C = it[DBProof.C],
                            id = it[DBProof.id],
                            script = it[DBProof.script]
                        )
                    }
                    proof ?: throw Exception("No proof found for amount $amt")
                }
            }

            return PreSplitBundle(
                preSplitProofs = proofs,
                preSplitItems = preSplitItems
            )
        }

        // We know the denominations we want, but we still need to decide which of our available denominations to
        // use. Moreover, what we have might not equal the amount exactly, so we'll need to add extra outputs to our
        // list of required outputs to make the amounts match.
        // TODO: The denominations to send will be chosen by picking the biggest denominations first up until we
        //       reach our target amount. This will likely result in wallets that have a ton of smaller
        //       denominations. Consider a more sophisticated approach. Maybe even building from lowest amount first
        //       half of the time?
        private fun chooseDenominations(availableDenominations: List<ULong>, targetAmount: ULong): List<ULong> {
            val sortedDenominations = availableDenominations.sortedDescending()
            val selectedDenominations = mutableListOf<ULong>()
            var currentSum = 0uL

            for (denomination in sortedDenominations) {
                if (currentSum < targetAmount) {
                    selectedDenominations.add(denomination)
                    currentSum += denomination
                }
            }

            val changeRequired = currentSum - targetAmount
            selectedDenominations.add(changeRequired)

            return selectedDenominations
        }
    }
}

// TODO: I don't think this create static method requires the secret and blindingFactorBytes parameters. We can build
//       them internally.
// TODO: If the PreSplitItem and PreMintItem are the same, we should consider using a common type.
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
    public val amount: ULong,
    private val secret: Secret,
    public val blindedSecret: PublicKey,
    public val blindingFactor: PrivateKey
) {
    public companion object {
        public fun create(amount: ULong, secret: Secret, blindingFactorBytes: ByteArray?): PreSplitItem {
            require(blindingFactorBytes == null || blindingFactorBytes.size == 32) { "Blinding factor must be 32 bytes long because it's a private key" }

            val blindingFactorBytes = blindingFactorBytes ?: randomBytes(32)
            val blindingFactor: PrivateKey = PrivateKey(blindingFactorBytes)
            val blindedSecret: PublicKey = hashToCurve(secret.value) + blindingFactor.publicKey()

            return PreSplitItem(amount, secret, blindedSecret, blindingFactor)
        }
    }
}
