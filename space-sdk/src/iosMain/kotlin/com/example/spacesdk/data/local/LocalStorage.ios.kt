package com.example.spacesdk.data.local

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.spacesdk.data.local.db.SpaceDatabase

// A SQLite file in the app's sandbox (Library/Application Support/databases).
internal actual fun sqlDriverFactory(): (() -> SqlDriver)? =
    { NativeSqliteDriver(SpaceDatabase.Schema.synchronous(), DATABASE_NAME) }
