package com.example.spacesdk

import com.example.spacesdk.data.ApodRepository
import com.example.spacesdk.data.DefaultApodRepository
import com.example.spacesdk.data.DefaultNewsRepository
import com.example.spacesdk.data.NewsRepository
import com.example.spacesdk.data.source.GeneratedApodRemoteDataSource
import com.example.spacesdk.data.source.GeneratedNewsRemoteDataSource
import com.example.spacesdk.data.source.apodApi
import com.example.spacesdk.data.source.spaceflightNewsApi

/**
 * SpaceSdk: NASA's Astronomy Picture of the Day and spaceflight news.
 *
 * ```
 * val sdk = SpaceSdk()                         // one instance per app
 * sdk.pictureOfTheDay.today()
 * sdk.pictureOfTheDay.forDate(2014, 9, 18)
 * sdk.news.latest(count = 20)
 * ```
 *
 * No API key is needed. Create one instance and share it: it owns the network
 * clients and caches.
 */
public class SpaceSdk internal constructor(
    apodRepository: ApodRepository,
    newsRepository: NewsRepository,
) {
    public constructor() : this(
        DefaultApodRepository(GeneratedApodRemoteDataSource(apodApi())),
        DefaultNewsRepository(GeneratedNewsRemoteDataSource(spaceflightNewsApi())),
    )

    /** Astronomy Picture of the Day. */
    public val pictureOfTheDay: PictureOfTheDayUseCases = PictureOfTheDayUseCases(apodRepository)

    /** Spaceflight news. */
    public val news: NewsUseCases = NewsUseCases(newsRepository)
}
