package com.example.spacesdk.data.source

import com.example.spacesdk.User
import kotlinx.coroutines.delay

/** Data source for the network. Works with exactly one source of data. */
internal interface UserRemoteDataSource {
    /** Returns the user, or null when the server has no such user. Throws on network failure. */
    suspend fun fetchUser(id: String): User?
}

/**
 * Stand-in for a real HTTP client. Replaced by OpenAPI-generated clients
 * (APOD, Spaceflight News) in the next step.
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
