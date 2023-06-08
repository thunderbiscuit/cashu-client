package me.tb

import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.PublicKey
import me.tb.db.DBProof
import me.tb.db.DBSettings
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

public class Wallet(
    private var activeKeyset: Keyset? = null,
    private val mintUrl: String,
) {
    private var inactiveKeysets: List<Keyset> = listOf()

    public fun addKeyset(keyset: Keyset): Unit {
        if (this.activeKeyset != null) {
            this.inactiveKeysets.plus(this.activeKeyset)
        }
        this.activeKeyset = keyset
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
