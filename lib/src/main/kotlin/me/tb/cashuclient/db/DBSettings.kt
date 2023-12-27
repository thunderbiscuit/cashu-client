/*
 * Copyright 2023 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE.txt file.
 */
 
package me.tb.cashuclient.db

import org.jetbrains.exposed.sql.Database

public object DBSettings {
    public val db: Database by lazy {
        Database.connect("jdbc:sqlite:./cashu.sqlite3", "org.sqlite.JDBC")
    }
}
