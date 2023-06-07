package me.tb

import kotlinx.serialization.Serializable
import fr.acinq.bitcoin.PublicKey

/**
 * This is the smallest unit of data returned by the mint endpoint. These aggregate into a [MintResponse] object.
 *
 * @param amount The value of the token.
 * @param blindedKey The blinded signature, a point on the curve referred to as C_ in the spec (NUT-00).
 * @param id The id of the keyset of the mint that signed the token.
 */
@Serializable
public data class BlindedSignature(
    val amount: ULong,
    val blindedKey: String,
    val id: Long
) {
    init {
        require(PublicKey.fromHex(blindedKey).isValid()) { "Invalid blinded key: $blindedKey" }
    }
}

/**
 * This is the object returned by the mint endpoint and consists of a list of blinded signatures.
 */
@Serializable
public data class MintResponse(
    val promises: List<BlindedSignature>
)
