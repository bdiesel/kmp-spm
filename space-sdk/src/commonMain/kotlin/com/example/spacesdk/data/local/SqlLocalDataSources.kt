package com.example.spacesdk.data.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import com.example.spacesdk.MediaType
import com.example.spacesdk.NewsArticle
import com.example.spacesdk.PictureOfTheDay
import com.example.spacesdk.data.local.db.SpaceDatabase
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** How many pictures are kept: the most recently viewed ones, plus the latest. */
internal const val MAX_STORED_PICTURES = 100L

/**
 * Pictures stored in SQLite, so they survive app restarts. [database] is
 * opened on first use, which happens on the repositories' background dispatcher.
 */
@OptIn(ExperimentalTime::class)
internal class SqlApodLocalDataSource(
    database: Lazy<SpaceDatabase>,
    private val maxPictures: Long = MAX_STORED_PICTURES,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ApodLocalDataSource {
    private val database by database
    private val queries get() = database.pictureQueries

    override suspend fun pictureFor(date: LocalDate): PictureOfTheDay? {
        val key = date.toString()
        return queries.byDate(key, ::toPicture).awaitAsOneOrNull()
            ?.also { queries.touch(now = nowMillis(), date = key) }
    }

    override suspend fun latest(): PictureOfTheDay? = queries.latest(::toPicture).awaitAsOneOrNull()

    // SQLDelight re-runs the query whenever a write to `picture` or `latest_picture` commits.
    override fun observeLatest(): Flow<PictureOfTheDay?> =
        queries.latest(::toPicture).asFlow().mapToOneOrNull(Dispatchers.Default).distinctUntilChanged()

    override suspend fun save(picture: PictureOfTheDay, isLatest: Boolean) {
        database.transaction {
            with(picture) {
                queries.upsert(date, title, explanation, mediaType.name, imageUrl, pageUrl, credit, altText, nowMillis())
            }
            if (isLatest) queries.setLatest(picture.date)
            queries.trim(keep = maxPictures)
        }
    }
}

/** The news list stored in SQLite, so it survives app restarts. */
internal class SqlNewsLocalDataSource(database: Lazy<SpaceDatabase>) : NewsLocalDataSource {
    private val database by database
    private val queries get() = database.articleQueries

    override suspend fun latest(limit: Int): CachedNews? {
        val fetchedAt = queries.fetchedAt().awaitAsOneOrNull() ?: return null
        return CachedNews(queries.latest(limit.toLong(), ::toArticle).awaitAsList(), fetchedAt)
    }

    override fun observeLatest(limit: Int): Flow<List<NewsArticle>> =
        queries.latest(limit.toLong(), ::toArticle).asFlow().mapToList(Dispatchers.Default).distinctUntilChanged()

    override suspend fun replace(articles: List<NewsArticle>, fetchedAtMillis: Long) {
        database.transaction {
            queries.clear()
            articles.forEachIndexed { position, a ->
                queries.insert(position.toLong(), a.id.toLong(), a.title, a.summary, a.url, a.imageUrl, a.newsSite, a.publishedAt)
            }
            queries.setFetchedAt(fetchedAtMillis)
        }
    }
}

private fun toPicture(
    date: String,
    title: String,
    explanation: String,
    mediaType: String,
    imageUrl: String?,
    pageUrl: String,
    credit: String,
    altText: String,
): PictureOfTheDay {
    val parsed = LocalDate.parse(date)
    return PictureOfTheDay(
        date = date,
        year = parsed.year,
        month = parsed.month.ordinal + 1,
        day = parsed.day,
        title = title,
        explanation = explanation,
        mediaType = MediaType.entries.firstOrNull { it.name == mediaType } ?: MediaType.OTHER,
        imageUrl = imageUrl,
        pageUrl = pageUrl,
        credit = credit,
        altText = altText,
    )
}

private fun toArticle(
    id: Long,
    title: String,
    summary: String,
    url: String,
    imageUrl: String?,
    newsSite: String,
    publishedAt: String,
) = NewsArticle(id.toInt(), title, summary, url, imageUrl, newsSite, publishedAt)
