package me.tb.cashuclient.db

import me.tb.cashuclient.types.Proof
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class SQLiteDBTest {
    val db = SQLiteDB(sqlDriver = testSqlDriver())

    @Test
    fun `spendable note sizes is empty when there are no proofs`() {
        assertTrue(db.spendableNoteSizes().isEmpty())
    }

    @Test
    fun `spendable note sizes is a list of proof amounts`() {
        db.insertProof(Proof(amount = 1u, id = "id1", secret = "secret2", C = "C1"))
        db.insertProof(Proof(amount = 2u, id = "id2", secret = "secret1", C = "C2"))

        val sizes = db.spendableNoteSizes()
        assertContentEquals(listOf(1u, 2u), sizes)
    }

    @Test
    fun `spendable note sizes is updated when a proof is deleted`() {
        val proof1 = Proof(amount = 1u, id = "id1", secret = "secret1", C = "C1")
        val proof2 = Proof(amount = 2u, id = "id2", secret = "secret2", C = "C2")

        db.insertProof(proof1)
        db.insertProof(proof2)

        assertContentEquals(listOf(1u, 2u), db.spendableNoteSizes())

        db.deleteProof(proof1)
        assertContentEquals(listOf(2u), db.spendableNoteSizes())

        db.deleteProof(proof2)

        assertTrue(db.spendableNoteSizes().isEmpty())
    }

    @Test
    fun `insert proofs`() {
        // Populate proofs
        val proof1 = Proof(amount = 1u, id = "id1", secret = "secret1", C = "C1")
        val proof2 = Proof(amount = 2u, id = "id2", secret = "secret2", C = "C2")
        db.insertProof(proof1)
        db.insertProof(proof2)

        // Look up proofs by their amounts
        val proofs = db.proofsForAmounts(listOf(proof1.amount, proof2.amount))
        assertContentEquals(listOf(proof1, proof2), proofs)
    }

    @Test
    fun `delete an existing proof`() {
        // Populate proofs
        val proof1 = Proof(amount = 1u, id = "id1", secret = "secret1", C = "C1")
        val proof2 = Proof(amount = 2u, id = "id2", secret = "secret2", C = "C2")
        db.insertProof(proof1)
        db.insertProof(proof2)

        // Delete one of the proofs
        db.deleteProof(proof1)

        // No longer able to look up a proof by its amount
        assertThrows<Throwable> {
            println(db.proofsForAmounts(listOf(proof1.amount)))
        }
    }
}