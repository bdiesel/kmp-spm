package com.example.spacesdk

import com.example.spacesdk.data.NewsRepository
import com.example.spacesdk.data.source.SpaceApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/**
 * Use cases for spaceflight news.
 *
 * Live data: [observeLatest] streams the stored articles and [refresh]
 * updates them from the network, so every observer sees the new list.
 *
 * ```
 * sdk.news.observeLatest(count = 20).collect { articles -> show(articles) }
 * sdk.news.refresh(count = 20)   // on screen start, pull-to-refresh…
 * ```
 *
 * Never throws (except cancellation); failures come back as
 * [NewsResult.Failure] or [RefreshResult.Failure].
 */
public class NewsUseCases internal constructor(
    private val repository: NewsRepository,
) {
    /** The [count] most recent articles (1–50). Uses stored articles when they're recent. */
    public suspend fun latest(count: Int): NewsResult = try {
        NewsResult.Success(repository.getLatest(count.clamped(), forceRefresh = false))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        NewsResult.Failure(e.toSpaceError())
    }

    /**
     * Up to [count] stored articles (1–50; empty until some have been loaded),
     * and again each time they change. Never fetches: call [refresh] for that.
     */
    public fun observeLatest(count: Int): Flow<List<NewsArticle>> = repository.observeLatest(count.clamped())

    /** Fetches the [count] most recent articles (1–50); [observeLatest] then emits them. */
    public suspend fun refresh(count: Int): RefreshResult = try {
        repository.refresh(count.clamped())
        RefreshResult.Success
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        RefreshResult.Failure(e.toSpaceError())
    }

    private fun Int.clamped() = coerceIn(1, 50)
}

/** A service that answered with an error is SERVER; anything else couldn't reach it. */
internal fun Exception.toSpaceError(): SpaceError =
    if (this is SpaceApiException) SpaceError.SERVER else SpaceError.NETWORK
