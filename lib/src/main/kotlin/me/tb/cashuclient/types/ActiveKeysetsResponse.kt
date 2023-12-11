package me.tb.cashuclient.types

import kotlinx.serialization.Serializable

/**
 * The data structure the mint returns when we ask for the active keysets.
 *
 * @param keysets A list of serializable [KeysetJson] objects.
 */
@Serializable
public data class ActiveKeysetsResponse(
    public val keysets: List<KeysetJson>
)
