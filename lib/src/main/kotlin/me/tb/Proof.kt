package me.tb

import kotlinx.serialization.Serializable

/**
 * Cashu token.
 */
@Serializable
public data class Proof(
    public val id: String,
    public val amount: ULong,
    public val secret: String,
    public val C: String,
    public val script: String? = null
)
