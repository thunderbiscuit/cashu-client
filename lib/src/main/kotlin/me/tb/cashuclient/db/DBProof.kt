/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */
 
package me.tb.cashuclient.db

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

@OptIn(ExperimentalUnsignedTypes::class)
public object DBProof : Table() {
    public val amount: Column<ULong> = ulong("amount")
    public val secret: Column<String> = varchar("secret", 100)
    public val C: Column<String> = varchar("C", 100)
    public val id: Column<String> = varchar("id", 100)
    public val script: Column<String?> = varchar("script", 100).nullable()
}
