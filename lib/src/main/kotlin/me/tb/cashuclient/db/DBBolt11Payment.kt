/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */

package me.tb.cashuclient.db

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

/**
 * A table to store payment requests made by the mint. Once paid, these will later be used to request tokens.
 */
@OptIn(ExperimentalUnsignedTypes::class)
public object DBBolt11Payment : Table() {
    public val pr: Column<String> = varchar("pr", 400)
    // The hash here is really just a secret payment ID used to identify the payment between the mint and the client.
    public val hash: Column<String> = varchar("hash", 100)
    public val value: Column<ULong> = ulong("value")
}
