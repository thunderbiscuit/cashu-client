/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */

package me.tb.cashuclient.mockdb

import me.tb.cashuclient.db.DBProof
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun buildMockDB() {
    // Connect to a unique in-memory H2 database for each test
    Database.connect("jdbc:h2:mem:test${UUID.randomUUID()};DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")

    // Create the table
    transaction {
        SchemaUtils.create(DBProof)
    }

    // Insert some rows for testing
    transaction {
        DBProof.batchInsert(
            listOf(
                mapOf(
                    DBProof.amount to 16uL,
                    DBProof.secret to "secret1",
                    DBProof.C to "C1",
                    DBProof.id to "I2yN+iRYfkzT",
                    DBProof.script to null
                ),
            )
        ) {}
    }
}
