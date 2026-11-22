package com.example.spacesdk.data

import com.example.spacesdk.FakeApodRemote
import com.example.spacesdk.FakeNewsRemote
import com.example.spacesdk.article
import com.example.spacesdk.picture
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RepositoriesTest {

    private fun TestScope.apodRepo(remote: FakeApodRemote) =
        DefaultApodRepository(remote, StandardTestDispatcher(testScheduler))

    private fun TestScope.newsRepo(remote: FakeNewsRemote) =
        DefaultNewsRepository(remote, StandardTestDispatcher(testScheduler))

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
    fun newsFallsBackToCacheWhenRefreshFails() = runTest {
        val remote = FakeNewsRemote(listOf(article(1)))
        val repository = newsRepo(remote)
        repository.getLatest(limit = 1, forceRefresh = false)

        remote.failWith = IllegalStateException("offline")

        assertEquals(listOf(1), repository.getLatest(limit = 1, forceRefresh = true).map { it.id })
    }
}
