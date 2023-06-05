package me.tb

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fr.acinq.bitcoin.PublicKey
import fr.acinq.bitcoin.crypto.Digest
import fr.acinq.secp256k1.Hex
import java.util.*

public class Keyset(keyset: Map<Long, PublicKey>) {
    private lateinit var sortedKeyset: SortedMap<Long, PublicKey>

    init {
        keyset.forEach { (value, publicKey) ->
            require(publicKey.isValid()) { "Invalid public key $publicKey (hex: ${publicKey.toHex()} for value $value." }
        }
        sortedKeyset = keyset.toSortedMap()
    }

    /**
     * Derive the [KeysetId] for a given [Keyset].
     */
    public fun deriveKeysetId(): KeysetId {
        val allKeys: ByteArray = ByteArray(0)
        sortedKeyset.values.forEach { publicKey ->
            allKeys.plus(publicKey.value.toByteArray())
        }
        val sha256Bytes: ByteArray = Digest.sha256().hash(allKeys)
        val hex: String = Hex.encode(sha256Bytes)
        return KeysetId(hex)
    }

    public fun getKey(tokenValue: Long): PublicKey {
        return sortedKeyset[tokenValue] ?: throw Exception("No key found in keyset for token value $tokenValue")
    }

    public companion object {
        public fun fromJson(jsonString: String): Keyset {
            val typeToken = object : TypeToken<Map<String, String>>() {}.type
            val json: Map<String, String> = Gson().fromJson(jsonString, typeToken)

            val keyset: Map<Long, PublicKey> = json.map { (tokenValue, publicKeyHex) ->
                tokenValue.toLong() to PublicKey.fromHex(publicKeyHex)
            }.toMap()

            return Keyset(keyset)
        }
    }
}

public data class KeysetId(
    val value: String
) {
    init {
        // require(keysetId.length == 12) { "Invalid length for keyset id: ${keysetId.length}, must be 12 characters (6 bytes)" }
    }
}
