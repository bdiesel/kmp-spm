package com.example.sdk

import com.example.sdk.data.DefaultUserRepository
import com.example.sdk.data.UserRepository
import com.example.sdk.data.source.FakeUserRemoteDataSource
import com.example.sdk.data.source.InMemoryUserLocalDataSource
import com.example.sdk.domain.GetUserUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * SDK entry point and composition root.
 *
 * Dependencies are wired by hand here (constructor injection). Hilt is
 * Android-only, and a DI framework would leak into the Swift/JS API. Create
 * one instance and share it for the app's lifetime: it owns the caches.
 */
public class KmpSdk internal constructor(repository: UserRepository) {

    public constructor() : this(
        DefaultUserRepository(
            remote = FakeUserRemoteDataSource(),
            local = InMemoryUserLocalDataSource(),
        ),
    )

    private val getUserUseCase = GetUserUseCase(repository)

    /** One-shot lookup. Never throws; failures come back as [UserResult.Failure]. */
    public suspend fun getUser(id: String): UserResult = getUserUseCase(id)

    /** Creates a state holder for one "look up a user" screen. */
    public fun createUserLookup(): UserLookupStateHolder =
        UserLookupStateHolder(
            getUser = getUserUseCase,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
}
