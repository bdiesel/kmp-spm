package com.example.spacesdk.data

import com.example.spacesdk.FakeApodRemote
import com.example.spacesdk.FakeImagePrefetcher
import com.example.spacesdk.FakeNewsRemote
import com.example.spacesdk.article
import com.example.spacesdk.picture
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class RepositoriesTest {

    private val images = FakeImagePrefetcher()

    private fun TestScope.apodRepo(remote: FakeApodRemote) =
        DefaultApodRepository(remote, images = images, dispatcher = StandardTestDispatcher(testScheduler))

    private fun TestScope.newsRepo(remote: FakeNewsRemote, nowMillis: () -> Long = { 0L }) =
        DefaultNewsRepository(
            remote,
            images = images,
            dispatcher = StandardTestDispatcher(testScheduler),
            maxAge = 15.minutes,
            nowMillis = nowMillis,
        )

    @Test
    fun datedPictureIsFetchedOnceThenCached() = runTest {
        val date = LocalDate(2014, 9, 18)
        val remote = FakeApodRemote().apply { byDate[date] = picture("2014-09-18") }
        val repository = apodRepo(remote)

        repository.getPicture(date)
        repository.getPicture(date)

        assertEquals(1, remote.calls)
    }

    @Test
    fun latestFallsBackToLastKnownPictureWhenOffline() = runTest {
        val remote = FakeApodRemote(latest = picture("2026-11-22"))
        val repository = apodRepo(remote)
        repository.getPicture(null)

        remote.failWith = IllegalStateException("offline")

        assertEquals("2026-11-22", repository.getPicture(null)?.date)
    }

    @Test
    fun latestFailsWhenOfflineWithNothingCached() = runTest {
        val repository = apodRepo(FakeApodRemote().apply { failWith = IllegalStateException("offline") })

        assertFailsWith<IllegalStateException> { repository.getPicture(null) }
    }

    @Test
    fun latestPictureIsAlsoCachedByDate() = runTest {
        val remote = FakeApodRemote(latest = picture("2026-11-22"))
        val repository = apodRepo(remote)

        repository.getPicture(null)
        repository.getPicture(LocalDate(2026, 11, 22))

        assertEquals(1, remote.calls)
    }

    @Test
    fun newsIsServedFromCacheUnlessRefreshed() = runTest {
        val remote = FakeNewsRemote(listOf(article(1), article(2), article(3)))
        val repository = newsRepo(remote)

        repository.getLatest(limit = 2, forceRefresh = false)
        assertEquals(listOf(1, 2), repository.getLatest(limit = 2, forceRefresh = false).map { it.id })
        assertEquals(1, remote.calls)

        repository.getLatest(limit = 2, forceRefresh = true)
        assertEquals(2, remote.calls)
    }

    @Test
    fun latestPicturesImageIsPinnedAndDatedOnesArePrefetched() = runTest {
        val date = LocalDate(2014, 9, 18)
        val remote = FakeApodRemote(latest = picture("2026-11-22")).apply { byDate[date] = picture("2014-09-18") }
        val repository = apodRepo(remote)

        repository.getPicture(null)
        repository.getPicture(date)

        assertEquals(listOf("https://example.com/2026-11-22.jpg"), images.pinned)
        assertEquals(listOf("https://example.com/2014-09-18.jpg"), images.prefetched)
    }

    @Test
    fun fetchedArticlesImagesArePrefetched() = runTest {
        val withImages = listOf(article(1).copy(imageUrl = "https://x/1.jpg"), article(2), article(3).copy(imageUrl = "https://x/3.jpg"))
        val repository = newsRepo(FakeNewsRemote(withImages))

        repository.getLatest(limit = 3, forceRefresh = false)
        repository.getLatest(limit = 3, forceRefresh = false) // served from storage: nothing new to fetch

        assertEquals(listOf("https://x/1.jpg", "https://x/3.jpg"), images.prefetched)
    }

    @Test
    fun refreshCommandsReportFailuresButKeepWhatIsStored() = runTest {
        val news = FakeNewsRemote(listOf(article(1)))
        val apod = FakeApodRemote(latest = picture("2026-11-22"))
        val newsRepository = newsRepo(news)
        val apodRepository = apodRepo(apod)
        newsRepository.refresh(1)
        apodRepository.refreshLatest()

        news.failWith = IllegalStateException("offline")
        apod.failWith = IllegalStateException("offline")

        // Unlike getLatest/getPicture, commands don't fall back: the app needs the error.
        assertFailsWith<IllegalStateException> { newsRepository.refresh(1) }
        assertFailsWith<IllegalStateException> { apodRepository.refreshLatest() }
        assertEquals(listOf(1), newsRepository.observeLatest(1).first().map { it.id })
        assertEquals("2026-11-22", apodRepository.observeLatest().first()?.date)
    }

    @Test
    fun staleNewsIsRefetched() = runTest {
        var now = 0L
        val remote = FakeNewsRemote(listOf(article(1)))
        val repository = newsRepo(remote) { now }
        repository.getLatest(limit = 1, forceRefresh = false)

        now += 14.minutes.inWholeMilliseconds
        repository.getLatest(limit = 1, forceRefresh = false)
        assertEquals(1, remote.calls) // still fresh

        now += 2.minutes.inWholeMilliseconds
        repository.getLatest(limit = 1, forceRefresh = false)
        assertEquals(2, remote.calls) // older than 15 minutes
    }

    @Test
    fun staleNewsIsStillServedWhenOffline() = runTest {
        var now = 0L
        val remote = FakeNewsRemote(listOf(article(1)))
        val repository = newsRepo(remote) { now }
        repository.getLatest(limit = 1, forceRefresh = false)

        now += 3.days.inWholeMilliseconds
        remote.failWith = IllegalStateException("offline")

        assertEquals(listOf(1), repository.getLatest(limit = 1, forceRefresh = false).map { it.id })
    }

    @Test
    fun newsFallsBackToCacheWhenRefreshFails() = runTest {
        val remote = FakeNewsRemote(listOf(article(1)))
        val repository = newsRepo(remote)
        repository.getLatest(limit = 1, forceRefresh = false)

        remote.failWith = IllegalStateException("offline")

        assertEquals(listOf(1), repository.getLatest(limit = 1, forceRefresh = true).map { it.id })
    }
}
