package com.example.spacesdk

import com.example.spacesdk.data.DefaultUserRepository
import com.example.spacesdk.data.UserRepository
import com.example.spacesdk.data.source.FakeUserRemoteDataSource
import com.example.spacesdk.data.source.InMemoryUserLocalDataSource
import com.example.spacesdk.domain.GetUserUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * SDK entry point and composition root.
 *
 * Dependencies are wired by hand here (constructor injection). Hilt is
 * Android-only, and a DI framework would leak into the Swift API. Create one
 * instance and share it for the app's lifetime: it owns the caches.
 */
public class SpaceSdk internal constructor(repository: UserRepository) {

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
