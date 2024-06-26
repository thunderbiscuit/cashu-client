/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */

package me.tb.cashuclient.melt

import fr.acinq.lightning.payment.PaymentRequest
import me.tb.cashuclient.db.CashuDB
import me.tb.cashuclient.melt.PreMeltBundle.Companion.create
import me.tb.cashuclient.types.BlindedMessage
import me.tb.cashuclient.types.Proof

/**
 * The data bundle Alice must create prior to communicating with the mint requesting a melt.
 * Once the mint sends a response, this data is then combined with the [MeltResponse] to update the database
 * and remove the tokens and consider the invoice paid. You cannot instantiate this class directly, and must use the
 * [create] factory method instead.
 *
 * Note: NUT-08 defines a protocol for sending blank outputs to the mint in case the fees were lower than what was sent,
 * but this is not implemented here.
 */
public class PreMeltBundle private constructor(
    public val proofs: List<Proof>,
    private val quoteId: String,
    public val potentialChangeOutputs: List<BlindedMessage>? = null,
) {
    /**
     * Builds a [MeltRequest] from the data in this bundle. This [MeltRequest] is the data structure that is then
     * serialized and sent over the wire to the mint when requesting a melt.
     */
    public fun buildMeltRequest(): MeltRequest {
        return MeltRequest(
            quoteId = quoteId,
            proofs = proofs,
        )
    }

    public companion object {
        /**
         * A factory method to create a [PreMeltBundle] from a list of denominations and a [PaymentRequest].
         *
         * @param denominationsToUse The denominations to use for the melt.
         * @param quoteId            The id of the quote given by the mint.
         * @return A [PreMeltBundle] containing the proofs and payment request.
         */
        public fun create(
            denominationsToUse: List<ULong>,
            quoteId: String,
            db: CashuDB,
        ): PreMeltBundle {
            // Check if we have these amounts in the database
            val proofs = db.proofsForAmounts(denominationsToUse)

            return PreMeltBundle(
                proofs = proofs,
                quoteId = quoteId,
                potentialChangeOutputs = null,
            )
        }
    }
}
