package com.example.spacesdk.data

import com.example.spacesdk.FakeRemote
import com.example.spacesdk.User
import com.example.spacesdk.data.source.InMemoryUserLocalDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultUserRepositoryTest {
    private val grace = User("2", "Grace Hopper")

    @Test
    fun cachesRemoteResultInLocalSourceOfTruth() = runTest {
        val remote = FakeRemote(grace)
        val local = InMemoryUserLocalDataSource()
        val repository = DefaultUserRepository(remote, local, StandardTestDispatcher(testScheduler))

        assertEquals(grace, repository.getUser("2"))
        assertEquals(grace, repository.getUser("2"))

        assertEquals(1, remote.calls, "second lookup should be served from the cache")
        assertEquals(grace, repository.observeUser("2").first())
    }

    @Test
    fun forceRefreshHitsRemoteAgain() = runTest {
        val remote = FakeRemote(grace)
        val repository = DefaultUserRepository(remote, InMemoryUserLocalDataSource(), StandardTestDispatcher(testScheduler))

        repository.getUser("2")
        repository.getUser("2", forceRefresh = true)

        assertEquals(2, remote.calls)
    }

    @Test
    fun unknownUserIsNull() = runTest {
        val repository = DefaultUserRepository(FakeRemote(), InMemoryUserLocalDataSource(), StandardTestDispatcher(testScheduler))

        assertNull(repository.getUser("42"))
    }
}
