package me.tb

import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.PublicKey

/**
 * The data bundle Alice must create prior to communicating with the mint. Once the mint sends a response,
 * this data is then combined with the [MintResponse] to create valid tokens (promises).
 *
 * TODO: Open issue in spec about exact name for the B_ key.
 */
public class PreMintBundle(
    public val preMintItems: List<PreMintItem>
) {
    public fun buildMintRequest(): MintRequest {
        val outputs: List<BlindedMessage> = preMintItems.map { preMintItem ->
            BlindedMessage(
                amount = preMintItem.amount,
                blindedSecret = preMintItem.blindedSecret.toString(),
            )
        }

        return MintRequest(outputs = outputs)
    }
}

/**
 * The data structures that get combined into a [PreMintBundle], required to build a [MintRequest].
 *
 * @param amount The amount of the token.
 * @param secret The secret x that is used in hashToCurve(x) to create Y.
 * @param blindedSecret The blinded secret B_ that is sent to the mint.
 * @param blindingFactor The blinding factor r, private key of the point R that is used to blind key Y.
 */
public class PreMintItem private constructor (
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
