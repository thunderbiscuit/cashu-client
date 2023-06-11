package me.tb

import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.PublicKey
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import me.tb.db.DBProof
import me.tb.db.DBSettings
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

public class Wallet(
    public var activeKeyset: Keyset? = null,
    private val mintUrl: String
) {
    public val inactiveKeysets: MutableList<Keyset> = mutableListOf()

    private fun addKeyset(keyset: Keyset) {
        val currentActiveKeyset = this.activeKeyset
        if (currentActiveKeyset != null) inactiveKeysets.add(currentActiveKeyset)
        this.activeKeyset = keyset
    }

    /**
     * Query the mint for the active [Keyset] and set it as the active keyset.
     *
     * TODO: This method doesn't handle mint errors yet.
     */
    public fun getActiveKeyset(): Unit = runBlocking(Dispatchers.IO) {
        val keyset = async {
            val client = HttpClient(OkHttp)
            val keysetJson = client.get("$mintUrl/keys").bodyAsText()
            client.close()
            Keyset.fromJson(keysetJson)
        }
        addKeyset(keyset.await())
    }

    /**
     * Query the mint for the [Keyset] associated with a given [KeysetId].
     *
     * TODO: This method doesn't handle mint errors yet.
     */
    public fun getSpecificKeyset(keysetId: KeysetId): Keyset = runBlocking(Dispatchers.IO) {
        val urlSafeKeysetId = base64ToBase64UrlSafe(keysetId.value)
        val oldKeyset = async {
            val client = HttpClient(OkHttp)
            val keysetJson = client.get("$mintUrl/keys/$urlSafeKeysetId").bodyAsText()
            client.close()
            Keyset.fromJson(keysetJson)
        }
        oldKeyset.await()
    }

    /**
     * The wallet processes the mint's response by unblinding the signatures and adding the [Proof]s to its database.
     */
    public fun processMintResponse(preMintBundle: PreMintBundle, mintResponse: MintResponse) {
        require(preMintBundle.preMintItems.size == mintResponse.promises.size) {
            "The number of outputs in the mint request and promises in the mint response must be the same."
        }
        val scopedActiveKeyset = this.activeKeyset ?: throw Exception("The wallet must have an active keyset for the mint.")

        (preMintBundle.preMintItems zip mintResponse.promises).forEach { (preMintItem, promise) ->
            // Unblinding is done like so: C = C_ - rK
            val r: PrivateKey = preMintItem.blindingFactor
            val K: PublicKey = scopedActiveKeyset.getKey(preMintItem.amount.toULong())
            val rK: PublicKey = K.times(r)
            val unblindedKey: PublicKey = PublicKey.fromHex(promise.blindedKey).minus(rK)

            val proof: Proof = Proof(
                amount = promise.amount,
                secret = r.toHex(),
                C = unblindedKey.toHex(),
                id = scopedActiveKeyset.keysetId.value,
                script = null
            )

            DBSettings.db
            transaction {
                SchemaUtils.create(DBProof)

                DBProof.insert {
                    it[amount] = proof.amount
                    it[secret] = proof.secret
                    it[C] = proof.C
                    it[id] = proof.id
                    it[script] = null
                }
            }
        }
    }
}
