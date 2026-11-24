package com.example.spacesdk.data.local

import app.cash.sqldelight.db.SqlDriver

// Not yet: SQLDelight's standard web driver (sql.js) keeps the database in
// memory, so it wouldn't survive a reload. Browser persistence needs SQLite on
// OPFS, a separate step. Until then the web keeps its cache in memory.
internal actual fun sqlDriverFactory(): (() -> SqlDriver)? = null
