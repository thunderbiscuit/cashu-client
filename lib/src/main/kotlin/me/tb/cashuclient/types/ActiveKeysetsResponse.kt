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

// TODO (SPEC-RELATED): I don't know why the specific keyset endpoint returns a list of keysets and don't think it
//                      should.
/**
 * The data structure the mint returns when we ask for a specific keyset.
 *
 * @param keysets A list of serializable [KeysetJson] objects.
 */
public typealias SpecificKeysetResponse = ActiveKeysetsResponse
