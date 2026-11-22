@file:OptIn(ExperimentalCoroutinesApi::class)

package com.example.spacesdk

import com.example.spacesdk.domain.GetLatestNewsUseCase
import com.example.spacesdk.domain.GetPictureOfTheDayUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

// Holders launch into backgroundScope (cancelled when the test ends).
// runCurrent() runs that work; advanceUntilIdle() would skip it.
class StateHoldersTest {

    private val OFFLINE_MESSAGE = "Couldn't reach the server. Check your connection and try again."

    private fun TestScope.pictureHolder(repository: FakeApodRepository) =
        PictureOfTheDayStateHolder(GetPictureOfTheDayUseCase(repository), backgroundScope)

    private fun TestScope.newsHolder(repository: FakeNewsRepository, limit: Int = 2) =
        NewsStateHolder(GetLatestNewsUseCase(repository), limit, backgroundScope)

    @Test
    fun pictureStartsLoadingThenShowsLatest() = runTest {
        val holder = pictureHolder(FakeApodRepository(mapOf(null to picture("2026-11-22"))))
        assertEquals(PictureOfTheDayUiState.Loading, holder.currentState)

        runCurrent()

        assertEquals(PictureOfTheDayUiState.Success(picture("2026-11-22")), holder.currentState)
    }

    @Test
    fun pictureLoadForDate() = runTest {
        val repository = FakeApodRepository(
            mapOf(null to picture("2026-11-22"), LocalDate(2014, 9, 18) to picture("2014-09-18")),
        )
        val holder = pictureHolder(repository)
        runCurrent()

        holder.load("2014-09-18")
        runCurrent()

        assertEquals(PictureOfTheDayUiState.Success(picture("2014-09-18")), holder.currentState)
    }

    @Test
    fun pictureErrorState() = runTest {
        val holder = pictureHolder(FakeApodRepository().apply { failWith = IllegalStateException("offline") })

        runCurrent()

        assertEquals(PictureOfTheDayUiState.Error(OFFLINE_MESSAGE), holder.currentState)
    }

    @Test
    fun newsLoadsOnStart() = runTest {
        val holder = newsHolder(FakeNewsRepository(listOf(article(1), article(2), article(3))))

        runCurrent()

        assertEquals(NewsUiState.Success(listOf(article(1), article(2))), holder.currentState)
    }

    @Test
    fun newsErrorState() = runTest {
        val holder = newsHolder(FakeNewsRepository().apply { failWith = IllegalStateException("offline") })

        runCurrent()

        assertEquals(NewsUiState.Error(OFFLINE_MESSAGE), holder.currentState)
    }
}
