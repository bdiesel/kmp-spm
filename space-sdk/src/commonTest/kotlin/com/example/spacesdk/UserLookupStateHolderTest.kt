@file:OptIn(ExperimentalCoroutinesApi::class)

package com.example.spacesdk

import com.example.spacesdk.domain.GetUserUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UserLookupStateHolderTest {
    private val grace = User("2", "Grace Hopper")

    // The holder launches into backgroundScope, which runTest cancels at the end.
    // runCurrent() runs that background work; advanceUntilIdle() would skip it.
    private fun TestScope.holder(repository: FakeRepository) =
        UserLookupStateHolder(GetUserUseCase(repository), backgroundScope)

    @Test
    fun startsIdle() = runTest {
        assertEquals(UserLookupUiState.Idle, holder(FakeRepository()).currentState)
    }

    @Test
    fun lookUpEndsInSuccess() = runTest {
        val holder = holder(FakeRepository(grace))

        holder.lookUp("2")
        runCurrent()

        assertEquals(UserLookupUiState.Success(grace), holder.currentState)
    }

    @Test
    fun missingUserIsNotFound() = runTest {
        val holder = holder(FakeRepository(grace))

        holder.lookUp("9")
        runCurrent()

        assertEquals(UserLookupUiState.NotFound("9"), holder.currentState)
    }

    @Test
    fun failuresBecomeErrorState() = runTest {
        val holder = holder(FakeRepository().apply { failWith = IllegalStateException("Network unavailable") })

        holder.lookUp("2")
        runCurrent()

        assertEquals(UserLookupUiState.Error("Network unavailable"), holder.currentState)
    }
}
