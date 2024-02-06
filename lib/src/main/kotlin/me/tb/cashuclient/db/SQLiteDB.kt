package me.tb.cashuclient.db

import me.tb.cashuclient.types.Proof
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * The default implementation of the [CashuDB] interface, using SQLite.
 */
public class SQLiteDB : CashuDB {
    override fun insertProof(proof: Proof) {
        transaction(DBSettings.db) {
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

    override fun deleteProof(proof: Proof) {
        transaction(DBSettings.db) {
            SchemaUtils.create(DBProof)
            val secretOfProofToDelete = proof.secret
            DBProof.deleteWhere { secret eq secretOfProofToDelete }
        }
    }

    override fun spendableNoteSizes(): List<ULong> {
        return transaction(DBSettings.db) {
            SchemaUtils.create(DBProof)
            DBProof.selectAll().map { it[DBProof.amount] }
        }
    }
}
