package com.example.spacesdk.domain

import com.example.spacesdk.UserResult
import com.example.spacesdk.data.UserRepository
import kotlinx.coroutines.CancellationException

/**
 * Looks up a user and turns every outcome — including invalid input and
 * failures — into a [UserResult], so consumers never have to catch exceptions.
 */
internal class GetUserUseCase(private val repository: UserRepository) {

    suspend operator fun invoke(id: String): UserResult {
        val userId = id.trim()
        if (userId.isEmpty()) return UserResult.Failure("Enter a user id")

        return try {
            repository.getUser(userId)
                ?.let { UserResult.Found(it) }
                ?: UserResult.NotFound(userId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UserResult.Failure(e.message ?: "Something went wrong")
        }
    }
}
