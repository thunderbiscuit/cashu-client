package me.tb

import fr.acinq.bitcoin.PublicKey
import kotlinx.serialization.SerialName
import java.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A Cashu token that includes proofs and their respective mints. Can include proofs from multiple different mints and keysets.
 */
@Serializable
public data class TokenV3(
    @SerialName("token") public val tokenEntries: List<TokenEntry> = listOf(),
    public val memo: String? = null,
) {
    public companion object {
        public fun deserialize(serializedTokenV3: String): TokenV3 {
            val fullV3Prefix: String = PREFIX + V3VERSION

            // For V3 tokens the prefix must be "cashuA"
            require(serializedTokenV3.startsWith(fullV3Prefix)) { "Invalid token V3 prefix." }

            val encoded: String = serializedTokenV3.substring(startIndex = fullV3Prefix.length)
            val jsonString: String = Base64.getUrlDecoder().decode(encoded).toString(Charsets.UTF_8)
            val tokenV3: TokenV3 = Json.decodeFromString(TokenV3.serializer(), jsonString)

            return tokenV3
        }
    }

    /**
     * Takes a TokenV3 and serializes it as "cashuA<json_urlsafe_base64>".
     */
    public fun serialize(): String {
        val fullPrefix: String = PREFIX + V3VERSION

        return fullPrefix + Base64.getUrlEncoder().encodeToString(
            Json.encodeToString(serializer(), this).toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * Returns the total value of the token.
     */
    public fun getAmount(): Long {
        return tokenEntries.sumOf { tokenEntry ->
            tokenEntry.proofs.sumOf { proof ->
                proof.amount
            }
        }
    }
}

// TODO: Having the mint as an element to each TokenEntry implies a token can be composed of multiple tokens from different mints?
@Serializable
public data class TokenEntry(
    public val mint: String? = null,
    public val proofs: List<Proof>,
)
