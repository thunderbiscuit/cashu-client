package me.tb

import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.PublicKey
import me.tb.db.DBProof
import me.tb.db.DBSettings
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.*
import kotlinx.coroutines.*

public class Wallet(
    public var activeKeyset: Keyset? = null,
    private val mintUrl: String,
) {
    public val inactiveKeysets: MutableList<Keyset> = mutableListOf()

    private fun addKeyset(keyset: Keyset): Unit {
        val currentActiveKeyset = this.activeKeyset
        if (currentActiveKeyset != null) inactiveKeysets.add(currentActiveKeyset)
        this.activeKeyset = keyset
    }

    public fun getActiveKeyset(): Unit = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO)

        val keyset = scope.async {
            val client = HttpClient(OkHttp)
            val keysetJson = client.get("https://8333.space:3338/keys").bodyAsText()
            client.close()
            Keyset.fromJson(keysetJson)
        }

        addKeyset(keyset.await())
    }

    /**
     * The wallet processes the mint's response by unblinding the signatures and adding the [Proof]s to its database.
     */
    public fun processMintResponse(preMintBundle: PreMintBundle, mintResponse: MintResponse): Unit {
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
