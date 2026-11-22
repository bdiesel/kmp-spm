package com.example.spacesdk.data

import com.example.spacesdk.NewsArticle
import com.example.spacesdk.PictureOfTheDay
import com.example.spacesdk.data.source.ApodRemoteDataSource
import com.example.spacesdk.data.source.NewsRemoteDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

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
 * Offline-first: an in-memory cache is the single source of truth and the
 * network refreshes it. A dated picture never changes, so it's fetched once.
 * "Latest" always asks the network but falls back to the last known picture
 * when offline. Main-safe: work runs on [dispatcher].
 */
internal class DefaultApodRepository(
    private val remote: ApodRemoteDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ApodRepository {
    private val mutex = Mutex()
    private val byDate = mutableMapOf<LocalDate, PictureOfTheDay>()
    private var latest: PictureOfTheDay? = null

    override suspend fun getPicture(date: LocalDate?): PictureOfTheDay? = withContext(dispatcher) {
        if (date != null) {
            mutex.withLock { byDate[date] } ?: remote.fetchByDate(date)?.also { cache(date, it) }
        } else {
            try {
                remote.fetchLatest()?.also { picture ->
                    cache(LocalDate.parse(picture.date), picture)
                    mutex.withLock { latest = picture }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutex.withLock { latest } ?: throw e
            }
        }
    }

    private suspend fun cache(date: LocalDate, picture: PictureOfTheDay) {
        mutex.withLock { byDate[date] = picture }
    }
}

/** Offline-first news: serves the cached list unless asked to refresh; falls back to it when offline. */
internal class DefaultNewsRepository(
    private val remote: NewsRemoteDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : NewsRepository {
    private val mutex = Mutex()
    private var cached: List<NewsArticle> = emptyList()

    override suspend fun getLatest(limit: Int, forceRefresh: Boolean): List<NewsArticle> =
        withContext(dispatcher) {
            val current = mutex.withLock { cached }
            if (!forceRefresh && current.size >= limit) return@withContext current.take(limit)
            try {
                remote.fetchLatest(limit).also { fresh -> mutex.withLock { cached = fresh } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                current.takeIf { it.isNotEmpty() }?.take(limit) ?: throw e
            }
        }
}
