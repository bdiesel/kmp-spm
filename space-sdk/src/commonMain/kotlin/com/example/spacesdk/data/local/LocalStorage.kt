package com.example.spacesdk.data.local

import app.cash.sqldelight.db.SqlDriver
import com.example.spacesdk.data.local.db.SpaceDatabase

internal const val DATABASE_NAME = "spacesdk.db"

/**
 * Opens this platform's SQLite database, or null when the platform keeps the
 * cache in memory instead (currently the web).
 */
internal expect fun sqlDriverFactory(): (() -> SqlDriver)?

/** Where the SDK keeps its offline cache. [images] is null when images aren't cached. */
internal class LocalStorage(
    val apod: ApodLocalDataSource,
    val news: NewsLocalDataSource,
    val images: ImageCache?,
) {
    companion object {
        fun create(maxCachedImages: Int): LocalStorage {
            val openDriver = sqlDriverFactory() ?: return inMemory()
            // Opened lazily, on first use (a background dispatcher), not on the caller's thread.
            val database = lazy { SpaceDatabase(openDriver()) }
            val images = imageCacheDirectory()?.let { directory ->
                ImageCache(
                    index = SqlImageIndex(database),
                    files = FileImageFiles(directory),
                    download = httpImageDownloader(),
                    maxImages = maxCachedImages.coerceAtLeast(0),
                )
            }
            return LocalStorage(SqlApodLocalDataSource(database), SqlNewsLocalDataSource(database), images)
        }

        fun inMemory() = LocalStorage(InMemoryApodLocalDataSource(), InMemoryNewsLocalDataSource(), images = null)
    }
}
