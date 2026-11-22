package com.example.spacesdk.data.source

import com.example.spacesdk.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Local store and single source of truth for users. Swap for a database
 * (e.g. Room KMP or SQLDelight) to persist across launches.
 */
internal interface UserLocalDataSource {
    fun get(id: String): User?
    fun observe(id: String): Flow<User?>
    fun upsert(user: User)
}

internal class InMemoryUserLocalDataSource : UserLocalDataSource {
    private val users = MutableStateFlow<Map<String, User>>(emptyMap())

    override fun get(id: String): User? = users.value[id]

    override fun observe(id: String): Flow<User?> =
        users.map { it[id] }.distinctUntilChanged()

    override fun upsert(user: User) {
        users.update { it + (user.id to user) }
    }
}
