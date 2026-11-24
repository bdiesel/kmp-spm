package com.example.spacesdk.data.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.example.spacesdk.data.local.db.SpaceDatabase
import com.example.spacesdk.data.source.SpaceApiException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/** The folder for downloaded images (the OS may clear it), or null for no image cache. */
internal expect fun imageCacheDirectory(): String?

/** A `file://` URL for an absolute [path]. */
internal expect fun fileUrl(path: String): String

/** The image index in the SDK's SQLite database. */
internal class SqlImageIndex(database: Lazy<SpaceDatabase>) : ImageIndex {
    private val database by database
    private val queries get() = database.imageQueries

    override suspend fun get(url: String) = queries.byUrl(url, ::ImageEntry).awaitAsOneOrNull()
    override suspend fun all() = queries.all(::ImageEntry).awaitAsList()
    override suspend fun put(entry: ImageEntry) {
        queries.upsert(entry.url, entry.file, entry.sizeBytes, entry.lastUsedMillis)
    }
    override suspend fun touch(url: String, nowMillis: Long) { queries.touch(now = nowMillis, url = url) }
    override suspend fun remove(url: String) { queries.remove(url) }
    override suspend fun pinned() = queries.pinned().awaitAsOneOrNull()
    override suspend fun setPinned(url: String) { queries.setPinned(url) }
}

/**
 * Image files in [directory]. Writes go to a temporary file first, so a
 * half-written image is never served.
 */
internal class FileImageFiles(directory: String) : ImageFiles {
    private val directory = Path(directory)

    override suspend fun write(name: String, bytes: ByteArray) {
        SystemFileSystem.createDirectories(directory)
        val temporary = Path(directory, "$name.part")
        SystemFileSystem.sink(temporary).buffered().use { it.write(bytes) }
        SystemFileSystem.atomicMove(temporary, Path(directory, name))
    }

    override suspend fun localUrl(name: String): String? =
        Path(directory, name).takeIf { SystemFileSystem.exists(it) }?.let { fileUrl(it.toString()) }

    override suspend fun delete(name: String) {
        SystemFileSystem.delete(Path(directory, name), mustExist = false)
    }
}

/** Downloads image bytes with a plain HTTP client. */
internal fun httpImageDownloader(client: HttpClient = HttpClient()): suspend (String) -> ByteArray = { url ->
    val response = client.get(url)
    if (!response.status.isSuccess()) throw SpaceApiException("Image returned HTTP ${response.status.value}")
    response.body<ByteArray>()
}
