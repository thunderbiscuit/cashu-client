package me.tb

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fr.acinq.bitcoin.PublicKey
import fr.acinq.bitcoin.crypto.Digest
import java.util.SortedMap
import java.util.Base64

public class Keyset(keyset: Map<ULong, PublicKey>) {
    init {
        keyset.forEach { (value, publicKey) ->
            require(publicKey.isValid()) { "Invalid public key $publicKey (hex: ${publicKey.toHex()} for value $value." }
        }
    }

    public val sortedKeyset: SortedMap<ULong, PublicKey> = keyset.toSortedMap()
    public val keysetId: KeysetId = deriveKeysetId()

    /**
     * Derive the [KeysetId] for a given [Keyset].
     */
    private fun deriveKeysetId(): KeysetId {
        val allKeysConcatenated: String = buildString {
            sortedKeyset.values.forEach { publicKey ->
                append(publicKey)
            }
        }
        val sha256Bytes: ByteArray = Digest
            .sha256()
            .hash(allKeysConcatenated.toByteArray(Charsets.UTF_8))
            .sliceArray(0..8)
        val base64String = Base64.getEncoder().encodeToString(sha256Bytes)
        return KeysetId(base64String)
    }

    public fun getKey(tokenValue: ULong): PublicKey {
        return sortedKeyset[tokenValue] ?: throw Exception("No key found in keyset for token value $tokenValue")
    }

    public companion object {
        public fun fromJson(jsonString: String): Keyset {
            val typeToken = object : TypeToken<Map<String, String>>() {}.type
            val json: Map<String, String> = Gson().fromJson(jsonString, typeToken)

            val keyset: Map<ULong, PublicKey> = json.map { (tokenValue, publicKeyHex) ->
                tokenValue.toULong() to PublicKey.fromHex(publicKeyHex)
            }.toMap()

            return Keyset(keyset)
        }
    }
}

@JvmInline
public value class KeysetId(public val value: String) {
    init {
        // require(keysetId.length == 12) { "Invalid length for keyset id: ${keysetId.length}, must be 12 characters (6 bytes)" }
    }
}
