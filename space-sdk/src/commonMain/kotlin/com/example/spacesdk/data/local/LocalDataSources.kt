package com.example.spacesdk.data.local

import com.example.spacesdk.NewsArticle
import com.example.spacesdk.PictureOfTheDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

/** Stored pictures of the day. */
internal interface ApodLocalDataSource {
    suspend fun pictureFor(date: LocalDate): PictureOfTheDay?

    /** The picture that was most recent when last fetched. */
    suspend fun latest(): PictureOfTheDay?

    /** [latest], re-emitted whenever it changes. */
    fun observeLatest(): Flow<PictureOfTheDay?>

    /** Stores [picture] by date; [isLatest] also marks it as the most recent one. */
    suspend fun save(picture: PictureOfTheDay, isLatest: Boolean)
}

/** A stored news list and when it was fetched. */
internal data class CachedNews(val articles: List<NewsArticle>, val fetchedAtMillis: Long)

/** The stored news list. */
internal interface NewsLocalDataSource {
    /** Up to [limit] stored articles, or null if nothing has been stored yet. */
    suspend fun latest(limit: Int): CachedNews?

    /** Up to [limit] stored articles (empty if none), re-emitted whenever the list changes. */
    fun observeLatest(limit: Int): Flow<List<NewsArticle>>

    /** Replaces the stored list. */
    suspend fun replace(articles: List<NewsArticle>, fetchedAtMillis: Long)
}

/** Keeps pictures for the lifetime of the SDK instance (web, tests). */
internal class InMemoryApodLocalDataSource : ApodLocalDataSource {
    private data class State(val byDate: Map<String, PictureOfTheDay> = emptyMap(), val latestDate: String? = null)

    private val state = MutableStateFlow(State())

    override suspend fun pictureFor(date: LocalDate) = state.value.byDate[date.toString()]

    override suspend fun latest() = state.value.latestOrNull()

    override fun observeLatest(): Flow<PictureOfTheDay?> = state.map { it.latestOrNull() }.distinctUntilChanged()

    override suspend fun save(picture: PictureOfTheDay, isLatest: Boolean) = state.update {
        State(it.byDate + (picture.date to picture), if (isLatest) picture.date else it.latestDate)
    }

    private fun State.latestOrNull() = latestDate?.let(byDate::get)
}

/** Keeps the news list for the lifetime of the SDK instance (web, tests). */
internal class InMemoryNewsLocalDataSource : NewsLocalDataSource {
    private val cached = MutableStateFlow<CachedNews?>(null)

    override suspend fun latest(limit: Int) = cached.value?.let { it.copy(articles = it.articles.take(limit)) }

    override fun observeLatest(limit: Int): Flow<List<NewsArticle>> =
        cached.map { it?.articles.orEmpty().take(limit) }.distinctUntilChanged()

    override suspend fun replace(articles: List<NewsArticle>, fetchedAtMillis: Long) {
        cached.value = CachedNews(articles, fetchedAtMillis)
    }
}
