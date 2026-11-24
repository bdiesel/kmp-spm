package com.example.spacesdk

import com.example.spacesdk.data.ApodRepository
import com.example.spacesdk.data.DefaultApodRepository
import com.example.spacesdk.data.DefaultNewsRepository
import com.example.spacesdk.data.NewsRepository
import com.example.spacesdk.data.local.LocalStorage
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
 * clients and the offline cache. On Android and iOS the cache is a SQLite
 * database, so pictures and news are available offline across app launches.
 */
public class SpaceSdk internal constructor(
    apodRepository: ApodRepository,
    newsRepository: NewsRepository,
) {
    public constructor() : this(LocalStorage.create())

    private constructor(storage: LocalStorage) : this(
        DefaultApodRepository(GeneratedApodRemoteDataSource(apodApi()), storage.apod),
        DefaultNewsRepository(GeneratedNewsRemoteDataSource(spaceflightNewsApi()), storage.news),
    )

    /** Astronomy Picture of the Day. */
    public val pictureOfTheDay: PictureOfTheDayUseCases = PictureOfTheDayUseCases(apodRepository)

    /** Spaceflight news. */
    public val news: NewsUseCases = NewsUseCases(newsRepository)
}
