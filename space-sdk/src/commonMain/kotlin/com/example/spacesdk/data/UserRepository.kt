package com.example.spacesdk.data

import com.example.spacesdk.User
import com.example.spacesdk.data.source.UserLocalDataSource
import com.example.spacesdk.data.source.UserRemoteDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Exposes user data to the rest of the SDK and hides where it comes from. */
internal interface UserRepository {
    /** Returns the user, fetching and caching it if needed. Null if it doesn't exist. */
    suspend fun getUser(id: String, forceRefresh: Boolean = false): User?

    /** Streams the cached user; emits again whenever the cache changes. */
    fun observeUser(id: String): Flow<User?>
}

/**
 * Offline-first repository: the local data source is the single source of
 * truth, the remote one only refreshes it. Main-safe: work is moved to
 * [dispatcher] here, so callers never need to switch threads.
 */
internal class DefaultUserRepository(
    private val remote: UserRemoteDataSource,
    private val local: UserLocalDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : UserRepository {

    override suspend fun getUser(id: String, forceRefresh: Boolean): User? =
        withContext(dispatcher) {
            if (forceRefresh || local.get(id) == null) {
                remote.fetchUser(id)?.let(local::upsert)
            }
            local.get(id)
        }

    override fun observeUser(id: String): Flow<User?> = local.observe(id)
}
