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
import me.tb.cashuclient.splitAmount
import me.tb.cashuclient.types.BlindedMessage

// TODO: Open issue in spec about exact name for the B_ key.
/**
 * The data bundle Alice must create prior to communicating with the mint. Once the mint sends a response,
 * this data is then combined with the [MintingResponse] to create valid tokens (promises).
 */
public class PreMintBundle private constructor(
    public val preMintItems: List<PreMintItem>
) {
    public fun buildMintingRequest(): MintingRequest {
        val outputs: List<BlindedMessage> = preMintItems.map { preMintItem ->
            BlindedMessage(
                amount = preMintItem.amount,
                blindedSecret = preMintItem.blindedSecret.toString()
            )
        }

        return MintingRequest(outputs = outputs)
    }

    public companion object {
        public fun create(value: ULong): PreMintBundle {
            val tokenAmounts = splitAmount(value)
            val preMintItems: List<PreMintItem> = tokenAmounts.map { tokenAmount ->
                PreMintItem.create(
                    amount = tokenAmount,
                    secret = Secret(),
                    blindingFactorBytes = null
                )
            }

            return PreMintBundle(preMintItems)
        }
    }
}

// TODO: I don't think this create static method requires the secret and blindingFactorBytes parameters. We can build
//       them internally.
/**
 * The data structures that get combined into a [PreMintBundle], required to build a [MintingRequest].
 *
 * @param amount The amount of the token.
 * @param secret The secret x that is used in hashToCurve(x) to create Y.
 * @param blindedSecret The blinded secret B_ that is sent to the mint.
 * @param blindingFactor The blinding factor r, private key of the point R that is used to blind key Y.
 */
public class PreMintItem private constructor(
    public val amount: ULong,
    private val secret: Secret,
    public val blindedSecret: PublicKey,
    public val blindingFactor: PrivateKey
) {
    public companion object {
        public fun create(amount: ULong, secret: Secret, blindingFactorBytes: ByteArray?): PreMintItem {
            require(blindingFactorBytes == null || blindingFactorBytes.size == 32) { "Blinding factor must be 32 bytes long because it's a private key" }

            val blindingFactorBytes = blindingFactorBytes ?: randomBytes(32)
            val blindingFactor: PrivateKey = PrivateKey(blindingFactorBytes)
            val blindedSecret: PublicKey = hashToCurve(secret.value) + blindingFactor.publicKey()

            return PreMintItem(amount, secret, blindedSecret, blindingFactor)
        }
    }
}
