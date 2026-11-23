package com.example.spacesdk

import com.example.spacesdk.data.ApodRepository
import com.example.spacesdk.data.NewsRepository
import com.example.spacesdk.data.source.ApodRemoteDataSource
import com.example.spacesdk.data.source.NewsRemoteDataSource
import kotlinx.datetime.LocalDate

internal fun picture(date: String, title: String = "Picture $date") = PictureOfTheDay(
    date = date,
    year = date.substring(0, 4).toInt(),
    month = date.substring(5, 7).toInt(),
    day = date.substring(8, 10).toInt(),
    title = title,
    explanation = "Explanation",
    mediaType = MediaType.IMAGE,
    imageUrl = "https://example.com/$date.jpg",
    pageUrl = "https://example.com/$date",
    credit = "NASA",
    altText = "",
)

internal fun article(id: Int) = NewsArticle(
    id = id,
    title = "Article $id",
    summary = "Summary $id",
    url = "https://example.com/news/$id",
    imageUrl = null,
    newsSite = "Example",
    publishedAt = "2026-11-22T00:00:00Z",
)

internal class FakeApodRemote(var latest: PictureOfTheDay? = null) : ApodRemoteDataSource {
    val byDate = mutableMapOf<LocalDate, PictureOfTheDay>()
    var failWith: Exception? = null
    var calls = 0

    override suspend fun fetchLatest(): PictureOfTheDay? {
        calls++
        failWith?.let { throw it }
        return latest
    }

    override suspend fun fetchByDate(date: LocalDate): PictureOfTheDay? {
        calls++
        failWith?.let { throw it }
        return byDate[date]
    }
}

internal class FakeNewsRemote(var articles: List<NewsArticle> = emptyList()) : NewsRemoteDataSource {
    var failWith: Exception? = null
    var calls = 0

    override suspend fun fetchLatest(limit: Int): List<NewsArticle> {
        calls++
        failWith?.let { throw it }
        return articles.take(limit)
    }
}

internal class FakeApodRepository(private val pictures: Map<LocalDate?, PictureOfTheDay> = emptyMap()) : ApodRepository {
    var failWith: Exception? = null
    val requested = mutableListOf<LocalDate?>()

    override suspend fun getPicture(date: LocalDate?): PictureOfTheDay? {
        requested += date
        failWith?.let { throw it }
        return pictures[date]
    }
}

internal class FakeNewsRepository(private val articles: List<NewsArticle> = emptyList()) : NewsRepository {
    var failWith: Exception? = null
    val requestedLimits = mutableListOf<Int>()

    override suspend fun getLatest(limit: Int, forceRefresh: Boolean): List<NewsArticle> {
        requestedLimits += limit
        failWith?.let { throw it }
        return articles.take(limit)
    }
}
