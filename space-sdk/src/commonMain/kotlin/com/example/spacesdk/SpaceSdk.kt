package com.example.spacesdk

import com.example.spacesdk.data.ApodRepository
import com.example.spacesdk.data.DefaultApodRepository
import com.example.spacesdk.data.DefaultNewsRepository
import com.example.spacesdk.data.ImagePrefetcher
import com.example.spacesdk.data.NewsRepository
import com.example.spacesdk.data.local.DEFAULT_MAX_CACHED_IMAGES
import com.example.spacesdk.data.local.ImageCache
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
 * sdk.images.displayUrl(picture.imageUrl)
 * ```
 *
 * No API key is needed. Create one instance and share it: it owns the network
 * clients and the offline cache. On Android and iOS, pictures, news and their
 * images are stored on the device, so they're available offline across launches.
 */
public class SpaceSdk internal constructor(
    apodRepository: ApodRepository,
    newsRepository: NewsRepository,
    imageCache: ImageCache? = null,
) {
    /** Keeps the 50 most recently used images, plus today's picture. */
    public constructor() : this(DEFAULT_MAX_CACHED_IMAGES)

    /** Keeps the [maxCachedImages] most recently used images, plus today's picture. 0 caches none. */
    public constructor(maxCachedImages: Int) : this(maxCachedImages, appGroup = null)

    /**
     * Like `SpaceSdk(maxCachedImages)`, sharing storage with app extensions.
     *
     * iOS: [appGroup] is an App Group ID (`group.…`). The SDK keeps its offline
     * data and images in that group's container, so an app and its widget
     * extension share them. Use the same ID in the app and every extension, and
     * add it to each target's App Groups entitlement. If the app isn't entitled
     * to it, the SDK uses the app's own storage.
     *
     * Android and web: [appGroup] is ignored. Android widgets run in the app's
     * process and already share its storage.
     */
    public constructor(maxCachedImages: Int, appGroup: String?) :
        this(LocalStorage.create(maxCachedImages, appGroup))

    private constructor(storage: LocalStorage) : this(
        DefaultApodRepository(
            GeneratedApodRemoteDataSource(apodApi()),
            storage.apod,
            storage.images ?: ImagePrefetcher.None,
        ),
        DefaultNewsRepository(
            GeneratedNewsRemoteDataSource(spaceflightNewsApi()),
            storage.news,
            storage.images ?: ImagePrefetcher.None,
        ),
        storage.images,
    )

    /** Astronomy Picture of the Day. */
    public val pictureOfTheDay: PictureOfTheDayUseCases = PictureOfTheDayUseCases(apodRepository)

    /** Spaceflight news. */
    public val news: NewsUseCases = NewsUseCases(newsRepository)

    /** Cached images for pictures and articles. */
    public val images: ImageUseCases = ImageUseCases(imageCache)

}
