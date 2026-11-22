package com.example.spacesdk

import com.example.spacesdk.data.ApodRepository
import com.example.spacesdk.data.DefaultApodRepository
import com.example.spacesdk.data.DefaultNewsRepository
import com.example.spacesdk.data.NewsRepository
import com.example.spacesdk.data.source.GeneratedApodRemoteDataSource
import com.example.spacesdk.data.source.GeneratedNewsRemoteDataSource
import com.example.spacesdk.data.source.apodApi
import com.example.spacesdk.data.source.spaceflightNewsApi
import com.example.spacesdk.domain.GetLatestNewsUseCase
import com.example.spacesdk.domain.GetPictureOfTheDayUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * SDK entry point and composition root.
 *
 * Wires the OpenAPI-generated clients (internal, from :space-api) into the
 * data, domain and UI-state layers by hand (constructor injection). Create one
 * instance and share it for the app's lifetime: it owns the HTTP clients and
 * caches. No API key is needed for either service.
 */
public class SpaceSdk internal constructor(
    apodRepository: ApodRepository,
    newsRepository: NewsRepository,
) {

    public constructor() : this(
        DefaultApodRepository(GeneratedApodRemoteDataSource(apodApi())),
        DefaultNewsRepository(GeneratedNewsRemoteDataSource(spaceflightNewsApi())),
    )

    private val getPictureUseCase = GetPictureOfTheDayUseCase(apodRepository)
    private val getNewsUseCase = GetLatestNewsUseCase(newsRepository)

    /** Today's (most recent) Astronomy Picture of the Day. Never throws. */
    public suspend fun getPictureOfTheDay(): PictureOfTheDayResult = getPictureUseCase(date = null)

    /** The picture for [date] (`YYYY-MM-DD`, 1995-06-16 or later). Never throws. */
    public suspend fun getPictureOfTheDay(date: String): PictureOfTheDayResult = getPictureUseCase(date)

    /** The [limit] most recent spaceflight news articles (1–50). Never throws. */
    public suspend fun getLatestNews(limit: Int): NewsResult = getNewsUseCase(limit, forceRefresh = false)

    /** Creates a state holder for a picture-of-the-day screen. */
    public fun createPictureOfTheDay(): PictureOfTheDayStateHolder =
        PictureOfTheDayStateHolder(getPictureUseCase, newScope())

    /** Creates a state holder for a news list showing [limit] articles. */
    public fun createNews(limit: Int): NewsStateHolder =
        NewsStateHolder(getNewsUseCase, limit, newScope())

    private fun newScope() = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
