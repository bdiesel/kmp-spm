package com.example.spacesdk.data.source

import com.example.spaceapi.apod.apis.ApodApi
import com.example.spaceapi.spaceflightnews.apis.ArticlesApi
import com.example.spacesdk.NewsArticle
import com.example.spacesdk.PictureOfTheDay
import io.ktor.client.engine.HttpClientEngine
import kotlinx.datetime.LocalDate

/** Thrown by data sources when a service answers with an error status. */
internal class SpaceApiException(message: String) : Exception(message)

// Base URLs are set here, not taken from the specs: the published Spaceflight
// News spec has no `servers` entry, so the generator defaults to http://localhost.
internal const val APOD_BASE_URL = "https://science.nasa.gov/wp-json/wp/v2"
internal const val SPACEFLIGHT_NEWS_BASE_URL = "https://api.spaceflightnewsapi.net"

/** Generated APOD client pointed at production. [engine] is for tests (MockEngine). */
internal fun apodApi(engine: HttpClientEngine? = null) =
    ApodApi(baseUrl = APOD_BASE_URL, httpClientEngine = engine)

/** Generated Spaceflight News client pointed at production. [engine] is for tests. */
internal fun spaceflightNewsApi(engine: HttpClientEngine? = null) =
    ArticlesApi(baseUrl = SPACEFLIGHT_NEWS_BASE_URL, httpClientEngine = engine)

/** NASA APOD over the network. Works with exactly one source of data. */
internal interface ApodRemoteDataSource {
    /** The most recent picture, or null if the service returned none. */
    suspend fun fetchLatest(): PictureOfTheDay?

    /** The picture for [date], or null if there is none. */
    suspend fun fetchByDate(date: LocalDate): PictureOfTheDay?
}

/** Spaceflight News over the network. */
internal interface NewsRemoteDataSource {
    suspend fun fetchLatest(limit: Int): List<NewsArticle>
}

/** [ApodRemoteDataSource] backed by the OpenAPI-generated [ApodApi]. */
internal class GeneratedApodRemoteDataSource(private val api: ApodApi) : ApodRemoteDataSource {

    override suspend fun fetchLatest(): PictureOfTheDay? {
        val response = api.listApod(page = 1, perPage = 1)
        if (!response.success) throw SpaceApiException("NASA APOD returned HTTP ${response.status}")
        return response.body().firstOrNull()?.toPictureOfTheDay()
    }

    override suspend fun fetchByDate(date: LocalDate): PictureOfTheDay? {
        val response = api.getApodByDate(date.toApodDateCode())
        // WordPress answers 400 ("invalid parameter") or 404 for dates without a post.
        if (response.status == 400 || response.status == 404) return null
        if (!response.success) throw SpaceApiException("NASA APOD returned HTTP ${response.status}")
        return response.body().toPictureOfTheDay()
    }
}

/** [NewsRemoteDataSource] backed by the OpenAPI-generated [ArticlesApi]. */
internal class GeneratedNewsRemoteDataSource(private val api: ArticlesApi) : NewsRemoteDataSource {

    override suspend fun fetchLatest(limit: Int): List<NewsArticle> {
        val response = api.articlesList(limit = limit)
        if (!response.success) throw SpaceApiException("Spaceflight News returned HTTP ${response.status}")
        return response.body().results.map { it.toNewsArticle() }
    }
}
