package com.example.spacesdk.data.local

import com.example.spacesdk.data.ImagePrefetcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Default for `SpaceSdk(maxCachedImages)`. */
internal const val DEFAULT_MAX_CACHED_IMAGES = 50

/** Backstop so a few very large images can't fill the device. Not configurable. */
internal const val MAX_IMAGE_CACHE_BYTES = 200L * 1024 * 1024

/** A downloaded image. */
internal data class ImageEntry(val url: String, val file: String, val sizeBytes: Long, val lastUsedMillis: Long)

/** Which images are stored, and how recently each was used. */
internal interface ImageIndex {
    suspend fun get(url: String): ImageEntry?
    suspend fun all(): List<ImageEntry>
    suspend fun put(entry: ImageEntry)
    suspend fun touch(url: String, nowMillis: Long)
    suspend fun remove(url: String)
    suspend fun pinned(): String?
    suspend fun setPinned(url: String)
}

/** Where image bytes live. */
internal interface ImageFiles {
    suspend fun write(name: String, bytes: ByteArray)

    /** A URL apps can display, or null if the file is gone (the OS may clear caches). */
    suspend fun localUrl(name: String): String?
    suspend fun delete(name: String)
}

/**
 * Downloads images and keeps the [maxImages] most recently used ones (and at
 * most [maxBytes] in total), least recently used removed first. The pinned
 * image (today's picture) is never removed and counts toward neither limit.
 * The same URL is only downloaded once at a time.
 */
@OptIn(ExperimentalTime::class)
internal class ImageCache(
    private val index: ImageIndex,
    private val files: ImageFiles,
    private val download: suspend (url: String) -> ByteArray,
    private val maxImages: Int,
    private val maxBytes: Long = MAX_IMAGE_CACHE_BYTES,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ImagePrefetcher {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutex = Mutex()
    private val inFlight = mutableMapOf<String, Deferred<String?>>()

    /** A local URL for [url], downloading it if needed; null if it can't be downloaded or kept. */
    suspend fun localUrl(url: String): String? = cached(url) ?: fetch(url).await()

    override fun prefetch(urls: List<String>) {
        urls.distinct().forEach { url -> scope.launch { if (cached(url) == null) fetch(url).await() } }
    }

    override fun pin(url: String) {
        scope.launch {
            mutex.withLock { index.setPinned(url) }
            if (cached(url) == null) fetch(url).await()
        }
    }

    private suspend fun cached(url: String): String? = mutex.withLock {
        val entry = index.get(url) ?: return@withLock null
        val local = files.localUrl(entry.file)
        if (local == null) index.remove(url) else index.touch(url, nowMillis())
        local
    }

    /** Starts downloading [url], or joins the download already running. */
    private suspend fun fetch(url: String): Deferred<String?> = mutex.withLock {
        inFlight.getOrPut(url) {
            scope.async {
                try {
                    store(url, download(url))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null // offline, or the image is gone: apps fall back to the original URL
                } finally {
                    mutex.withLock { inFlight.remove(url) }
                }
            }
        }
    }

    private suspend fun store(url: String, bytes: ByteArray): String? {
        val name = imageFileName(url)
        files.write(name, bytes)
        mutex.withLock {
            index.put(ImageEntry(url, name, bytes.size.toLong(), nowMillis()))
            evict()
        }
        return files.localUrl(name) // null if it was too big to keep
    }

    /** Caller holds [mutex]. */
    private suspend fun evict() {
        val pinned = index.pinned()
        var count = 0
        var bytes = 0L
        for (entry in index.all().filter { it.url != pinned }.sortedByDescending { it.lastUsedMillis }) {
            if (count < maxImages && bytes + entry.sizeBytes <= maxBytes) {
                count++
                bytes += entry.sizeBytes
            } else {
                index.remove(entry.url)
                files.delete(entry.file)
            }
        }
    }
}

/** A stable file name for [url] (64-bit FNV-1a hash), keeping a known image extension. */
internal fun imageFileName(url: String): String {
    var hash = -0x340d631b7bdddcdbL // FNV offset basis 0xcbf29ce484222325
    for (c in url) {
        hash = hash xor c.code.toLong()
        hash *= 0x100000001b3L
    }
    val extension = IMAGE_EXTENSION.find(url)?.groupValues?.get(1)?.lowercase()
    return hash.toULong().toString(16) + (extension?.let { ".$it" } ?: "")
}

private val IMAGE_EXTENSION = Regex("""\.(jpe?g|png|gif|webp)(?:$|[?#])""", RegexOption.IGNORE_CASE)

/** Keeps the index in memory (tests). */
internal class InMemoryImageIndex : ImageIndex {
    private val entries = mutableMapOf<String, ImageEntry>()
    private var pinnedUrl: String? = null

    override suspend fun get(url: String) = entries[url]
    override suspend fun all() = entries.values.toList()
    override suspend fun put(entry: ImageEntry) { entries[entry.url] = entry }
    override suspend fun touch(url: String, nowMillis: Long) {
        entries[url]?.let { entries[url] = it.copy(lastUsedMillis = nowMillis) }
    }
    override suspend fun remove(url: String) { entries.remove(url) }
    override suspend fun pinned() = pinnedUrl
    override suspend fun setPinned(url: String) { pinnedUrl = url }
}
