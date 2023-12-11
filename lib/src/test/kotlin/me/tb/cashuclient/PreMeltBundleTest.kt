/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */

package me.tb.cashuclient

import me.tb.cashuclient.db.DBProof
import me.tb.cashuclient.mockdb.buildMockDB
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

// class PreMeltBundleTest {
//     @BeforeTest
//     fun setUp() {
//         buildMockDB()
//     }
//
//     @Test
//     fun testMockDB() {
//         transaction {
//             val result = DBProof.selectAll()
//             assertEquals(1, result.count())
//         }
//     }
// }
