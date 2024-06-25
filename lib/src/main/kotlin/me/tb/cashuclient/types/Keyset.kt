/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */

package me.tb.cashuclient.types

import fr.acinq.bitcoin.PublicKey
import fr.acinq.bitcoin.crypto.Digest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.SortedMap

/**
 * A keyset is a set of public keys, each associated with a token value. The mint uses the key appropriate to the amount
 * to perform the signature on the corresponding blinded message.
 *
 * @param keyset A map of token values to public keys.
 */
public class Keyset(
    keyset: Map<ULong, PublicKey>,
) {
    init {
        keyset.forEach { (value, publicKey) ->
            require(publicKey.isValid()) { "Invalid public key $publicKey (hex: ${publicKey.toHex()}) for value $value." }
        }
    }

    public val sortedKeyset: SortedMap<ULong, PublicKey> = keyset.toSortedMap()
    public val keysetId: KeysetId by lazy { deriveKeysetId() }
    public val unit: EcashUnit = EcashUnit.SAT

    /**
     * Derive the [KeysetId] for a given [Keyset]. Currently only derives 0x00 version keyset ids.
     */
    @OptIn(ExperimentalStdlibApi::class)
    private fun deriveKeysetId(): KeysetId {
        // Add all compressed public keys together
        val allKeysConcatenated = sortedKeyset.values.fold(ByteArray(0)) { acc, publicKey ->
            acc + publicKey.value.toByteArray()
        }

        val versionPrefix = byteArrayOf(0x00)
        val bytes: ByteArray = Digest
            .sha256()
            .hash(allKeysConcatenated)
            .sliceArray(0..<7)
        val keysetId = versionPrefix + bytes

        return KeysetId(keysetId.toHexString())
    }

    /**
     * Get the [PublicKey] for a given token value.
     *
     * @param tokenValue The token value to get the key for.
     * @return The [PublicKey] for the given token value.
     * @throws Exception if no key is found for the given token value.
     */
    public fun getKey(tokenValue: ULong): PublicKey {
        return sortedKeyset[tokenValue]
            ?: throw Exception("No key found in keyset for token value $tokenValue")
    }

    public fun toKeysetJson(): KeysetJson {
        val keys: Map<ULong, String> = sortedKeyset.map { (tokenValue, publicKey) ->
            tokenValue to publicKey.toHex()
        }.toMap()
        val unit: String = "sat"
        val id: String = keysetId.value

        return KeysetJson(id, unit, keys)
    }

    /**
     * This override allows us to compare two [Keyset]s for equality (not implementing this means leaving the default
     * implementation, which is to compare references).
     */
    public override fun equals(other: Any?): Boolean {
        if (other == null || other !is Keyset) return false
        return sortedKeyset == other.sortedKeyset
    }

    public override fun hashCode(): Int {
        return sortedKeyset.hashCode()
    }

    public override fun toString(): String {
        return sortedKeyset.toString()
    }

    public companion object {
        /**
         * Create a [Keyset] from a JSON string.
         *
         * @param jsonString The JSON string to create the [Keyset] from.
         * @return The [Keyset] created from the JSON string.
         */
        public fun fromJson(jsonString: String): Keyset {
            val jsonObject = Json.parseToJsonElement(jsonString).jsonObject

            val keyset: Map<ULong, PublicKey> = jsonObject.entries.associate { (tokenValue, element) ->
                val publicKeyHex = element.jsonPrimitive.content
                tokenValue.toULong() to PublicKey.fromHex(publicKeyHex)
            }

            return Keyset(keyset)
        }
    }
}

// TODO: This class should be removed in favour of a proper serialization of the Keyset type.
@Serializable
public data class KeysetJson(
    public val id: String,
    public val unit: String = "sat",
    public val keys: Map<ULong, String>,
) {
    public fun toKeyset(): Keyset {
        val keysetOnly: Map<ULong, PublicKey> = keys.map { (tokenValue, publicKeyHex) ->
            tokenValue to PublicKey.fromHex(publicKeyHex)
        }.toMap()
        val keyset: Keyset = Keyset(keysetOnly)

        require(keyset.keysetId.value == id) { "Keyset id mismatch: ${keyset.keysetId.value} != $id" }

        return keyset
    }
}

// TODO: Write tests for this type
/**
 * A [KeysetId] is a unique identifier for a [Keyset].
 *
 * @param value The value of the [KeysetId], a lowercase hexadecimal String.
 */
@JvmInline
public value class KeysetId(public val value: String) {
    init {
        require(value.length == 16) { "Invalid length for keyset id: $value, must be 16 characters" }
    }

    public fun versionByte(): String {
        return value.take(2)
    }
}
