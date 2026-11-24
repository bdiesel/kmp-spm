package com.example.spacesdk.data.local

import app.cash.sqldelight.db.SqlDriver
import com.example.spacesdk.data.local.db.SpaceDatabase

internal const val DATABASE_NAME = "spacesdk.db"

/**
 * Opens this platform's SQLite database, or null when the platform keeps the
 * cache in memory instead (currently the web).
 */
internal expect fun sqlDriverFactory(): (() -> SqlDriver)?

/** Where the SDK keeps its offline cache. */
internal class LocalStorage(
    val apod: ApodLocalDataSource,
    val news: NewsLocalDataSource,
) {
    companion object {
        fun create(): LocalStorage {
            val openDriver = sqlDriverFactory() ?: return inMemory()
            // Opened lazily, on first use (a background dispatcher), not on the caller's thread.
            val database = lazy { SpaceDatabase(openDriver()) }
            return LocalStorage(SqlApodLocalDataSource(database), SqlNewsLocalDataSource(database))
        }

        fun inMemory() = LocalStorage(InMemoryApodLocalDataSource(), InMemoryNewsLocalDataSource())
    }
}
