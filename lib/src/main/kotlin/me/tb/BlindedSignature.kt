package me.tb

import kotlinx.serialization.Serializable

@Serializable
public data class BlindedSignature(
    val amount: Long,
    val blindedKey: String,
    val id: Long
)

/**
 * This is the object returned by the mint endpoint and consists of a list of blinded signatures.
 */
@Serializable
public data class BlindedSignatures(
    val blindedSignatures: List<BlindedSignature>
)
