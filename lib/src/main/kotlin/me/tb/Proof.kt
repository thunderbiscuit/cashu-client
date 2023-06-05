package me.tb

import kotlinx.serialization.Serializable

/**
 * Value token.
 *
 * TODO: Should all amounts be Longs? Look into this.
 */
@Serializable
public data class Proof(
    public val amount: Long,
    public val secret: String,
    public val C: String,
    public val id: String,
    public val script: String? = null
)
