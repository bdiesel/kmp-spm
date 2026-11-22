package com.example.spacesdk.domain

import com.example.spacesdk.FakeApodRepository
import com.example.spacesdk.FakeNewsRepository
import com.example.spacesdk.NewsResult
import com.example.spacesdk.PictureOfTheDayResult
import com.example.spacesdk.article
import com.example.spacesdk.data.source.SpaceApiException
import com.example.spacesdk.picture
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UseCasesTest {

    @Test
    fun latestPicture() = runTest {
        val repository = FakeApodRepository(mapOf(null to picture("2026-11-22")))

        val result = GetPictureOfTheDayUseCase(repository)(date = null)

        assertEquals(PictureOfTheDayResult.Success(picture("2026-11-22")), result)
    }

    @Test
    fun pictureForDateIsParsedAndTrimmed() = runTest {
        val repository = FakeApodRepository(mapOf(LocalDate(2014, 9, 18) to picture("2014-09-18")))

        val result = GetPictureOfTheDayUseCase(repository)(" 2014-09-18 ")

        assertTrue(result is PictureOfTheDayResult.Success)
        assertEquals(listOf<LocalDate?>(LocalDate(2014, 9, 18)), repository.requested)
    }

    @Test
    fun malformedDateFailsWithoutCallingTheRepository() = runTest {
        val repository = FakeApodRepository()

        val result = GetPictureOfTheDayUseCase(repository)("18/09/2014")

        assertEquals(PictureOfTheDayResult.Failure("Use a date like 2014-09-18"), result)
        assertTrue(repository.requested.isEmpty())
    }

    @Test
    fun dateBeforeFirstApodFails() = runTest {
        val result = GetPictureOfTheDayUseCase(FakeApodRepository())("1990-01-01")

        assertEquals(PictureOfTheDayResult.Failure("APOD starts on 1995-06-16"), result)
    }

    @Test
    fun missingPictureIsNotFound() = runTest {
        val result = GetPictureOfTheDayUseCase(FakeApodRepository())("2001-01-01")

        assertEquals(PictureOfTheDayResult.NotFound("2001-01-01"), result)
    }

    @Test
    fun repositoryErrorsBecomeFailures() = runTest {
        val repository = FakeApodRepository().apply { failWith = SpaceApiException("NASA APOD returned HTTP 503") }

        val result = GetPictureOfTheDayUseCase(repository)(null)

        assertEquals(PictureOfTheDayResult.Failure("Couldn't load: NASA APOD returned HTTP 503"), result)
    }

    @Test
    fun newsLimitIsClamped() = runTest {
        val repository = FakeNewsRepository(listOf(article(1)))
        val useCase = GetLatestNewsUseCase(repository)

        useCase(limit = 0, forceRefresh = false)
        useCase(limit = 500, forceRefresh = false)

        assertEquals(listOf(1, 50), repository.requestedLimits)
    }

    @Test
    fun transportErrorsGetAFriendlyMessage() = runTest {
        // e.g. a raw NSURLError dump from the Darwin engine
        val repository = FakeNewsRepository().apply { failWith = IllegalStateException("Error Domain=NSURLErrorDomain Code=-1004 ...") }

        assertEquals(
            NewsResult.Failure("Couldn't reach the server. Check your connection and try again."),
            GetLatestNewsUseCase(repository)(5, false),
        )
    }
}
