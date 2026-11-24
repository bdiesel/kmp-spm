package com.example.spacesdk.data

import com.example.spacesdk.NewsArticle
import com.example.spacesdk.PictureOfTheDay
import com.example.spacesdk.data.local.ApodLocalDataSource
import com.example.spacesdk.data.local.InMemoryApodLocalDataSource
import com.example.spacesdk.data.local.InMemoryNewsLocalDataSource
import com.example.spacesdk.data.local.NewsLocalDataSource
import com.example.spacesdk.data.source.ApodRemoteDataSource
import com.example.spacesdk.data.source.NewsRemoteDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

/** Exposes APOD data to the rest of the SDK and hides where it comes from. */
internal interface ApodRepository {
    /** The picture for [date], or the most recent one when [date] is null. Null if none exists. */
    suspend fun getPicture(date: LocalDate?): PictureOfTheDay?
}

/** Exposes spaceflight news to the rest of the SDK. */
internal interface NewsRepository {
    suspend fun getLatest(limit: Int, forceRefresh: Boolean): List<NewsArticle>
}

/**
 * Offline-first: local storage is the single source of truth and the network
 * refreshes it. A dated picture never changes, so it's fetched once. "Latest"
 * always asks the network but falls back to the last known picture when
 * offline. Main-safe: work runs on [dispatcher].
 */
internal class DefaultApodRepository(
    private val remote: ApodRemoteDataSource,
    private val local: ApodLocalDataSource = InMemoryApodLocalDataSource(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ApodRepository {

    override suspend fun getPicture(date: LocalDate?): PictureOfTheDay? = withContext(dispatcher) {
        if (date != null) {
            local.pictureFor(date) ?: remote.fetchByDate(date)?.also { local.save(it, isLatest = false) }
        } else {
            try {
                remote.fetchLatest()?.also { local.save(it, isLatest = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                local.latest() ?: throw e
            }
        }
    }
}

/**
 * Offline-first news: serves the stored list while it's younger than [maxAge]
 * (unless asked to refresh), otherwise fetches; falls back to the stored list,
 * however old, when offline.
 */
@OptIn(ExperimentalTime::class)
internal class DefaultNewsRepository(
    private val remote: NewsRemoteDataSource,
    private val local: NewsLocalDataSource = InMemoryNewsLocalDataSource(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val maxAge: Duration = 15.minutes,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : NewsRepository {

    override suspend fun getLatest(limit: Int, forceRefresh: Boolean): List<NewsArticle> =
        withContext(dispatcher) {
            val stored = local.latest(limit)
            val isFresh = stored != null && nowMillis() - stored.fetchedAtMillis < maxAge.inWholeMilliseconds
            if (!forceRefresh && isFresh && stored.articles.size >= limit) return@withContext stored.articles
            try {
                remote.fetchLatest(limit).also { local.replace(it, nowMillis()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                stored?.articles?.takeIf { it.isNotEmpty() } ?: throw e
            }
        }
}
