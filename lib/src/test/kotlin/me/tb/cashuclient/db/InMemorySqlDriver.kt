package me.tb.cashuclient.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

fun testSqlDriver() = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
