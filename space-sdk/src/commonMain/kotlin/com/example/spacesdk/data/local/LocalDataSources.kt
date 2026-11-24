package com.example.spacesdk.data.local

import com.example.spacesdk.NewsArticle
import com.example.spacesdk.PictureOfTheDay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate

/** Stored pictures of the day. */
internal interface ApodLocalDataSource {
    suspend fun pictureFor(date: LocalDate): PictureOfTheDay?

    /** The picture that was most recent when last fetched. */
    suspend fun latest(): PictureOfTheDay?

    /** Stores [picture] by date; [isLatest] also marks it as the most recent one. */
    suspend fun save(picture: PictureOfTheDay, isLatest: Boolean)
}

/** A stored news list and when it was fetched. */
internal data class CachedNews(val articles: List<NewsArticle>, val fetchedAtMillis: Long)

/** The stored news list. */
internal interface NewsLocalDataSource {
    /** Up to [limit] stored articles, or null if nothing has been stored yet. */
    suspend fun latest(limit: Int): CachedNews?

    /** Replaces the stored list. */
    suspend fun replace(articles: List<NewsArticle>, fetchedAtMillis: Long)
}

/** Keeps pictures for the lifetime of the SDK instance (web, tests). */
internal class InMemoryApodLocalDataSource : ApodLocalDataSource {
    private val mutex = Mutex()
    private val byDate = mutableMapOf<String, PictureOfTheDay>()
    private var latestDate: String? = null

    override suspend fun pictureFor(date: LocalDate) = mutex.withLock { byDate[date.toString()] }

    override suspend fun latest() = mutex.withLock { latestDate?.let(byDate::get) }

    override suspend fun save(picture: PictureOfTheDay, isLatest: Boolean) = mutex.withLock {
        byDate[picture.date] = picture
        if (isLatest) latestDate = picture.date
    }
}

/** Keeps the news list for the lifetime of the SDK instance (web, tests). */
internal class InMemoryNewsLocalDataSource : NewsLocalDataSource {
    private val mutex = Mutex()
    private var cached: CachedNews? = null

    override suspend fun latest(limit: Int) =
        mutex.withLock { cached?.let { it.copy(articles = it.articles.take(limit)) } }

    override suspend fun replace(articles: List<NewsArticle>, fetchedAtMillis: Long) =
        mutex.withLock { cached = CachedNews(articles, fetchedAtMillis) }
}
