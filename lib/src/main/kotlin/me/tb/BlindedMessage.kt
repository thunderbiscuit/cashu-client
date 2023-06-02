package me.tb

import kotlinx.serialization.Serializable

@Serializable
public data class BlindedMessage(
    val amount: Long,
    val blindedSecret: String
)
