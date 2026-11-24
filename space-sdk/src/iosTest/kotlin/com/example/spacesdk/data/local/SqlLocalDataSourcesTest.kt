package com.example.spacesdk.data.local

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.spacesdk.MediaType
import com.example.spacesdk.article
import com.example.spacesdk.data.local.db.SpaceDatabase
import com.example.spacesdk.picture
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Runs against real SQLite files on the iOS simulator (the same driver apps use).
class SqlLocalDataSourcesTest {

    private val name = "test-${Random.nextLong().toULong()}.db"

    private fun openDriver(): SqlDriver = NativeSqliteDriver(SpaceDatabase.Schema.synchronous(), name)

    private fun lazyDatabase(driver: SqlDriver) = lazyOf(SpaceDatabase(driver))

    @Test
    fun picturesSurviveReopeningTheDatabase() = runTest {
        val first = openDriver()
        SqlApodLocalDataSource(lazyDatabase(first)).apply {
            save(picture("2014-09-18").copy(mediaType = MediaType.VIDEO, imageUrl = null), isLatest = false)
            save(picture("2026-11-22"), isLatest = true)
        }
        first.close()

        val second = openDriver() // like a new app launch
        val reopened = SqlApodLocalDataSource(lazyDatabase(second))
        val dated = reopened.pictureFor(LocalDate(2014, 9, 18))
        assertEquals(picture("2014-09-18").copy(mediaType = MediaType.VIDEO, imageUrl = null), dated)
        assertEquals("2026-11-22", reopened.latest()?.date)
        assertNull(reopened.pictureFor(LocalDate(2000, 1, 1)))
        second.close()
    }

    @Test
    fun latestPointsAtTheMostRecentlySavedLatest() = runTest {
        val driver = openDriver()
        val source = SqlApodLocalDataSource(lazyDatabase(driver))

        assertNull(source.latest())
        source.save(picture("2026-11-21"), isLatest = true)
        source.save(picture("2026-11-22"), isLatest = true)
        source.save(picture("2014-09-18"), isLatest = false)

        assertEquals("2026-11-22", source.latest()?.date)
        driver.close()
    }

    @Test
    fun onlyTheMostRecentlyViewedPicturesAreKept() = runTest {
        val driver = openDriver()
        var now = 0L
        val source = SqlApodLocalDataSource(lazyDatabase(driver), maxPictures = 2, nowMillis = { now++ })

        source.save(picture("2026-11-22"), isLatest = true)
        source.save(picture("2020-01-01"), isLatest = false)
        source.save(picture("2020-01-02"), isLatest = false)
        source.pictureFor(LocalDate(2020, 1, 1)) // viewed again: now newer than 01-02
        source.save(picture("2020-01-03"), isLatest = false)
        source.save(picture("2020-01-04"), isLatest = false)

        assertNull(source.pictureFor(LocalDate(2020, 1, 2)))
        assertNull(source.pictureFor(LocalDate(2020, 1, 1)))
        assertEquals("2020-01-03", source.pictureFor(LocalDate(2020, 1, 3))?.date)
        assertEquals("2026-11-22", source.latest()?.date) // kept although it's the oldest
        driver.close()
    }

    @Test
    fun imageIndexSurvivesReopeningTheDatabase() = runTest {
        val first = openDriver()
        SqlImageIndex(lazyDatabase(first)).apply {
            put(ImageEntry("https://x/a.jpg", "a.jpg", 100, 1))
            put(ImageEntry("https://x/b.jpg", "b.jpg", 200, 2))
            touch("https://x/a.jpg", nowMillis = 5)
            remove("https://x/b.jpg")
            setPinned("https://x/today.jpg")
        }
        first.close()

        val second = openDriver()
        val index = SqlImageIndex(lazyDatabase(second))
        assertEquals(listOf(ImageEntry("https://x/a.jpg", "a.jpg", 100, 5)), index.all())
        assertEquals("https://x/today.jpg", index.pinned())
        second.close()
    }

    @Test
    fun imageFilesAreWrittenServedAndDeleted() = runTest {
        val directory = "${imageCacheDirectory()}/test-$name"
        val files = FileImageFiles(directory)

        assertNull(files.localUrl("a.jpg"))
        files.write("a.jpg", ByteArray(3) { it.toByte() })
        val url = files.localUrl("a.jpg")
        assertEquals(true, url?.startsWith("file:///"))
        assertEquals(true, url?.endsWith("/a.jpg"))
        assertEquals(3L, SystemFileSystem.metadataOrNull(Path(directory, "a.jpg"))?.size)

        files.delete("a.jpg")
        assertNull(files.localUrl("a.jpg"))
        SystemFileSystem.delete(Path(directory))
    }

    @Test
    fun newsIsReplacedInOrderAndSurvivesReopening() = runTest {
        val first = openDriver()
        SqlNewsLocalDataSource(lazyDatabase(first)).apply {
            assertNull(latest(10))
            replace(listOf(article(1), article(2), article(3)), fetchedAtMillis = 100)
            replace(listOf(article(7), article(5), article(9)), fetchedAtMillis = 200)
        }
        first.close()

        val second = openDriver()
        val reopened = SqlNewsLocalDataSource(lazyDatabase(second)).latest(2)
        assertEquals(listOf(7, 5), reopened?.articles?.map { it.id })
        assertEquals(article(7), reopened?.articles?.first())
        assertEquals(200, reopened?.fetchedAtMillis)
        second.close()
    }
}
