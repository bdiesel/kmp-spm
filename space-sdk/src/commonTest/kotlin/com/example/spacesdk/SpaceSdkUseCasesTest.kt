package com.example.spacesdk

import com.example.spacesdk.data.source.SpaceApiException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The public API, exercised the way an app uses it: through SpaceSdk. */
@OptIn(ExperimentalCoroutinesApi::class) // UnconfinedTestDispatcher
class SpaceSdkUseCasesTest {

    private fun sdk(
        apod: FakeApodRepository = FakeApodRepository(),
        news: FakeNewsRepository = FakeNewsRepository(),
    ) = SpaceSdk(apod, news)

    @Test
    fun todayReturnsLatestPicture() = runTest {
        val sdk = sdk(apod = FakeApodRepository(mapOf(null to picture("2026-11-22"))))

        assertEquals(PictureResult.Success(picture("2026-11-22")), sdk.pictureOfTheDay.today())
    }

    @Test
    fun forDateAsksForThatDate() = runTest {
        val apod = FakeApodRepository(mapOf(LocalDate(2014, 9, 18) to picture("2014-09-18")))

        val result = sdk(apod).pictureOfTheDay.forDate(2014, 9, 18)

        assertEquals(PictureResult.Success(picture("2014-09-18")), result)
        assertEquals(listOf<LocalDate?>(LocalDate(2014, 9, 18)), apod.requested)
    }

    @Test
    fun impossibleDateIsInvalidWithoutAnyRequest() = runTest {
        val apod = FakeApodRepository()

        assertEquals(PictureResult.Failure(SpaceError.INVALID_DATE), sdk(apod).pictureOfTheDay.forDate(2014, 2, 30))
        assertTrue(apod.requested.isEmpty())
    }

    @Test
    fun dateBeforeFirstPictureIsInvalid() = runTest {
        assertEquals(PictureResult.Failure(SpaceError.INVALID_DATE), sdk().pictureOfTheDay.forDate(1995, 6, 15))
    }

    @Test
    fun missingPictureIsNotFound() = runTest {
        assertEquals(PictureResult.Failure(SpaceError.NOT_FOUND), sdk().pictureOfTheDay.forDate(2001, 1, 1))
    }

    @Test
    fun serviceErrorIsServer() = runTest {
        val apod = FakeApodRepository().apply { failWith = SpaceApiException("NASA APOD returned HTTP 503") }

        assertEquals(PictureResult.Failure(SpaceError.SERVER), sdk(apod).pictureOfTheDay.today())
    }

    @Test
    fun transportErrorIsNetwork() = runTest {
        // e.g. a raw NSURLError from the iOS network stack
        val news = FakeNewsRepository().apply { failWith = IllegalStateException("Error Domain=NSURLErrorDomain Code=-1004") }

        assertEquals(NewsResult.Failure(SpaceError.NETWORK), sdk(news = news).news.latest(5))
    }

    @Test
    fun latestNews() = runTest {
        val news = FakeNewsRepository(listOf(article(1), article(2), article(3)))

        assertEquals(NewsResult.Success(listOf(article(1), article(2))), sdk(news = news).news.latest(2))
    }

    @Test
    fun pictureRefreshIsDeliveredThroughTheStream() = runTest {
        val sdk = sdk(apod = FakeApodRepository(mapOf(null to picture("2026-11-22"))))
        val seen = mutableListOf<PictureOfTheDay?>()
        val observing = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            sdk.pictureOfTheDay.observeToday().toList(seen)
        }

        assertEquals(RefreshResult.Success, sdk.pictureOfTheDay.refresh())

        assertEquals(listOf(null, picture("2026-11-22")), seen)
        observing.cancel()
    }

    @Test
    fun failedRefreshReportsTheErrorAndLeavesTheStreamAlone() = runTest {
        val apod = FakeApodRepository().apply { failWith = SpaceApiException("HTTP 503") }
        val news = FakeNewsRepository().apply { failWith = IllegalStateException("offline") }
        val sdk = sdk(apod, news)

        assertEquals(RefreshResult.Failure(SpaceError.SERVER), sdk.pictureOfTheDay.refresh())
        assertEquals(RefreshResult.Failure(SpaceError.NETWORK), sdk.news.refresh(5))
        assertEquals(null, sdk.pictureOfTheDay.observeToday().first())
    }

    @Test
    fun refreshWithNoPictureIsNotFound() = runTest {
        assertEquals(RefreshResult.Failure(SpaceError.NOT_FOUND), sdk().pictureOfTheDay.refresh())
    }

    @Test
    fun newsRefreshIsDeliveredThroughTheStream() = runTest {
        val sdk = sdk(news = FakeNewsRepository(listOf(article(1), article(2), article(3))))
        val seen = mutableListOf<List<Int>>()
        val observing = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            sdk.news.observeLatest(2).collect { seen += it.map(NewsArticle::id) }
        }

        assertEquals(RefreshResult.Success, sdk.news.refresh(2))

        assertEquals(listOf(emptyList(), listOf(1, 2)), seen)
        observing.cancel()
    }

    @Test
    fun newsCountIsClampedTo1Through50() = runTest {
        val news = FakeNewsRepository(listOf(article(1)))
        val sdk = sdk(news = news)

        sdk.news.latest(0)
        sdk.news.refresh(500)

        assertEquals(listOf(1, 50), news.requestedLimits)
    }
}
