package com.example.sdk.domain

import com.example.sdk.FakeRepository
import com.example.sdk.User
import com.example.sdk.UserResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetUserUseCaseTest {
    private val grace = User("2", "Grace Hopper")

    @Test
    fun foundUser() = runTest {
        assertEquals(UserResult.Found(grace), GetUserUseCase(FakeRepository(grace))("2"))
    }

    @Test
    fun trimsInput() = runTest {
        assertEquals(UserResult.Found(grace), GetUserUseCase(FakeRepository(grace))("  2 "))
    }

    @Test
    fun missingUser() = runTest {
        assertEquals(UserResult.NotFound("9"), GetUserUseCase(FakeRepository(grace))("9"))
    }

    @Test
    fun blankIdIsAFailureNotAnException() = runTest {
        assertEquals(UserResult.Failure("Enter a user id"), GetUserUseCase(FakeRepository())("   "))
    }

    @Test
    fun repositoryErrorsBecomeFailures() = runTest {
        val repository = FakeRepository().apply { failWith = IllegalStateException("Network unavailable") }

        assertEquals(UserResult.Failure("Network unavailable"), GetUserUseCase(repository)("2"))
    }
}
