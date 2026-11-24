package com.example.spacesdk.data.local

import com.example.spacesdk.ImageUseCases
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle
class ImageCacheTest {

    private class FakeFiles : ImageFiles {
        val stored = mutableMapOf<String, ByteArray>()
        override suspend fun write(name: String, bytes: ByteArray) { stored[name] = bytes }
        override suspend fun localUrl(name: String) = if (name in stored) "local://$name" else null
        override suspend fun delete(name: String) { stored.remove(name) }
    }

    private class FakeDownloads {
        val calls = mutableListOf<String>()
        var failWith: Exception? = null
        var size = 10

        suspend fun download(url: String): ByteArray {
            calls += url
            delay(100.milliseconds) // so overlapping requests really overlap
            failWith?.let { throw it }
            return ByteArray(size)
        }
    }

    private val files = FakeFiles()
    private val downloads = FakeDownloads()
    private val index = InMemoryImageIndex()
    private var now = 0L

    private fun TestScope.cache(maxImages: Int = 3, maxBytes: Long = 1_000) = ImageCache(
        index = index,
        files = files,
        download = downloads::download,
        maxImages = maxImages,
        maxBytes = maxBytes,
        dispatcher = StandardTestDispatcher(testScheduler),
        nowMillis = { now++ },
    )

    private suspend fun ImageCache.isCached(url: String) = index.get(url) != null && localUrl(url) != null

    @Test
    fun downloadsOnceThenServesTheLocalCopy() = runTest {
        val cache = cache()

        val first = cache.localUrl("https://x/a.jpg")
        val second = cache.localUrl("https://x/a.jpg")

        assertEquals("local://${imageFileName("https://x/a.jpg")}", first)
        assertEquals(first, second)
        assertEquals(1, downloads.calls.size)
    }

    @Test
    fun simultaneousRequestsShareOneDownload() = runTest {
        val cache = cache()

        val urls = List(5) { async { cache.localUrl("https://x/a.jpg") } }.awaitAll()

        assertEquals(1, downloads.calls.size)
        assertEquals(1, urls.toSet().size)
    }

    @Test
    fun keepsOnlyTheMostRecentlyUsedImages() = runTest {
        val cache = cache(maxImages = 2)
        cache.localUrl("https://x/a.jpg")
        cache.localUrl("https://x/b.jpg")
        cache.localUrl("https://x/a.jpg") // a is now more recent than b

        cache.localUrl("https://x/c.jpg")

        assertNull(index.get("https://x/b.jpg"))
        assertEquals(setOf("https://x/a.jpg", "https://x/c.jpg"), index.all().map { it.url }.toSet())
        assertEquals(2, files.stored.size) // b's file was deleted too
    }

    @Test
    fun pinnedImageIsNeverRemovedAndDoesNotCount() = runTest {
        val cache = cache(maxImages = 2)
        cache.pin("https://x/today.jpg")
        advanceUntilIdle()

        listOf("a", "b", "c", "d").forEach { cache.localUrl("https://x/$it.jpg") }

        assertNotNull(index.get("https://x/today.jpg"))
        assertEquals(3, index.all().size) // today + the 2 most recent
    }

    @Test
    fun sizeLimitRemovesImagesEvenUnderTheCount() = runTest {
        val cache = cache(maxImages = 10, maxBytes = 25)
        downloads.size = 10

        listOf("a", "b", "c").forEach { cache.localUrl("https://x/$it.jpg") }

        assertEquals(setOf("https://x/b.jpg", "https://x/c.jpg"), index.all().map { it.url }.toSet())
    }

    @Test
    fun imageTooBigToKeepIsNotServedLocally() = runTest {
        downloads.size = 2_000

        assertNull(cache(maxBytes = 1_000).localUrl("https://x/huge.jpg"))
        assertEquals(0, files.stored.size)
    }

    @Test
    fun failedDownloadFallsBackToTheOriginalUrl() = runTest {
        downloads.failWith = IllegalStateException("offline")

        val url = ImageUseCases(cache()).displayUrl("https://x/a.jpg")

        assertEquals("https://x/a.jpg", url)
    }

    @Test
    fun noCacheMeansTheOriginalUrl() = runTest {
        assertEquals("https://x/a.jpg", ImageUseCases(cache = null).displayUrl("https://x/a.jpg"))
    }

    @Test
    fun fileClearedByTheSystemIsDownloadedAgain() = runTest {
        val cache = cache()
        cache.localUrl("https://x/a.jpg")

        files.stored.clear() // the OS emptied the cache folder

        assertNotNull(cache.localUrl("https://x/a.jpg"))
        assertEquals(2, downloads.calls.size)
    }

    @Test
    fun prefetchDownloadsInTheBackground() = runTest {
        val cache = cache()

        cache.prefetch(listOf("https://x/a.jpg", "https://x/b.jpg", "https://x/a.jpg"))
        advanceUntilIdle()

        assertEquals(listOf("https://x/a.jpg", "https://x/b.jpg"), downloads.calls.sorted())
        assertEquals(true, cache.isCached("https://x/b.jpg"))
    }

    @Test
    fun fileNamesAreStableAndKeepTheExtension() {
        assertEquals(imageFileName("https://x/a.JPG?w=200"), imageFileName("https://x/a.JPG?w=200"))
        assertEquals(true, imageFileName("https://x/a.JPG?w=200").endsWith(".jpg"))
        assertEquals(false, imageFileName("https://x/a") .contains('.'))
        assertEquals(false, imageFileName("https://x/a.jpg") == imageFileName("https://x/b.jpg"))
    }
}
