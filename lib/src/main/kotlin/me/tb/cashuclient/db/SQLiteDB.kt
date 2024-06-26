package me.tb.cashuclient.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import fr.acinq.lightning.utils.getValue
import me.tb.cashuclient.types.Proof
import me.tb.cashulib.Database

/**
 * The default implementation of the [CashuDB] interface, using SQLite.
 */
public class SQLiteDB(
    private val sqlDriver: SqlDriver = sqlDriver()
) : CashuDB {

    private val database by lazy {
        Database.Schema.create(sqlDriver)
        Database(sqlDriver)
    }

    override fun proofsForAmounts(amounts: List<ULong>): List<Proof> {
        return amounts.map { amt ->
            val record = database.proofQueries.selectProofByAmount(amt.toLong()).executeAsOne()
            Proof(
                amount = record.amount.toULong(),
                secret = record.secret,
                C = record.C,
                id = record.id
            )
        }
    }

    override fun insertProof(proof: Proof) {
        database.proofQueries.insert(
            amount = proof.amount.toLong(),
            secret = proof.secret,
            C = proof.C,
            id = proof.id,
            script = proof.script
        )
    }

    override fun deleteProof(proof: Proof) {
        database.proofQueries.deleteById(proof.id)
    }

    override fun spendableNoteSizes(): List<ULong> {
        return database.proofQueries.selectAll().executeAsList().map { it.amount.toULong() }
    }
}

/**
 * Creates a new [SqlDriver] instance that connects to the SQLite database.
 */
private fun sqlDriver(): SqlDriver {
    return JdbcSqliteDriver(url = "jdbc:sqlite:./cashu.sqlite3")
}
