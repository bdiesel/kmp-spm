package com.example.sdk.data.source

import com.example.sdk.User
import kotlinx.coroutines.delay

/** Data source for the network. Works with exactly one source of data. */
internal interface UserRemoteDataSource {
    /** Returns the user, or null when the server has no such user. Throws on network failure. */
    suspend fun fetchUser(id: String): User?
}

/**
 * Stand-in for a real HTTP client. Replace with a Ktor-backed implementation
 * when the SDK talks to a backend.
 */
internal class FakeUserRemoteDataSource(
    private val latencyMillis: Long = 300,
) : UserRemoteDataSource {
    private val users = listOf(
        User("1", "Ada Lovelace"),
        User("2", "Grace Hopper"),
        User("3", "Alan Turing"),
    ).associateBy { it.id }

    override suspend fun fetchUser(id: String): User? {
        delay(latencyMillis)
        if (id == "error") error("Network unavailable")
        return users[id]
    }
}
