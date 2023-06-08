package me.tb.db

import org.jetbrains.exposed.sql.Database

public object DBSettings {
    public val db: Database by lazy {
        Database.connect("jdbc:sqlite:./cashu.sqlite3", "org.sqlite.JDBC")
    }
}
