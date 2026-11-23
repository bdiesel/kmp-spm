package com.example.spacesdk

import com.example.spacesdk.data.NewsRepository
import com.example.spacesdk.data.source.SpaceApiException
import kotlinx.coroutines.CancellationException

/**
 * Use cases for spaceflight news.
 *
 * Never throws (except cancellation); failures come back as [NewsResult.Failure].
 */
public class NewsUseCases internal constructor(
    private val repository: NewsRepository,
) {
    /** The [count] most recent articles (1–50). Uses cached articles when available. */
    public suspend fun latest(count: Int): NewsResult = load(count, forceRefresh = false)

    /** Like [latest], but always fetches fresh articles (e.g. pull-to-refresh). */
    public suspend fun refresh(count: Int): NewsResult = load(count, forceRefresh = true)

    private suspend fun load(count: Int, forceRefresh: Boolean): NewsResult = try {
        NewsResult.Success(repository.getLatest(count.coerceIn(1, 50), forceRefresh))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        NewsResult.Failure(e.toSpaceError())
    }
}

/** A service that answered with an error is SERVER; anything else couldn't reach it. */
internal fun Exception.toSpaceError(): SpaceError =
    if (this is SpaceApiException) SpaceError.SERVER else SpaceError.NETWORK
