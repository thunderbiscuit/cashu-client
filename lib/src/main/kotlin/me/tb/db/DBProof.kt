package me.tb.db

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

public object DBProof : Table() {
    @OptIn(ExperimentalUnsignedTypes::class)
    public val amount: Column<ULong> = ulong("amount")
    public val secret: Column<String> = varchar("secret", 100)
    public val C: Column<String> = varchar("C", 100)
    public val id: Column<String> = varchar("id", 100)
    public val script: Column<String?> = varchar("script", 100).nullable()
}
