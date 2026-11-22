package com.example.spacesdk

import com.example.spacesdk.data.UserRepository
import com.example.spacesdk.data.source.UserRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class FakeRemote(private vararg val users: User) : UserRemoteDataSource {
    var calls = 0
    var failWith: Exception? = null

    override suspend fun fetchUser(id: String): User? {
        calls++
        failWith?.let { throw it }
        return users.firstOrNull { it.id == id }
    }
}

internal class FakeRepository(private vararg val users: User) : UserRepository {
    var failWith: Exception? = null

    override suspend fun getUser(id: String, forceRefresh: Boolean): User? {
        failWith?.let { throw it }
        return users.firstOrNull { it.id == id }
    }

    override fun observeUser(id: String): Flow<User?> =
        flowOf(users.firstOrNull { it.id == id })
}
