@file:OptIn(ExperimentalJsExport::class)

package com.example.spacesdk.web

import com.example.spacesdk.NewsArticle
import com.example.spacesdk.PictureResult
import com.example.spacesdk.SpaceError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.promise
import kotlin.js.Promise
import com.example.spacesdk.ImageUseCases as CoreImageUseCases
import com.example.spacesdk.NewsResult as CoreNewsResult
import com.example.spacesdk.NewsUseCases as CoreNewsUseCases
import com.example.spacesdk.PictureOfTheDayUseCases as CorePictureOfTheDayUseCases
import com.example.spacesdk.SpaceSdk as CoreSpaceSdk

// The JavaScript face of SpaceSdk: the same use cases, with Promises instead of
// suspend functions and arrays instead of Kotlin lists. Everything else
// (caching, validation, the generated client) is the shared Kotlin code.

/**
 * SpaceSdk for the web.
 *
 * ```ts
 * const sdk = new SpaceSdk();
 * const result = await sdk.pictureOfTheDay.today();
 * ```
 */
@JsExport
public class SpaceSdk {
    private val core = CoreSpaceSdk()
    private val scope = MainScope()

    /** Astronomy Picture of the Day. */
    public val pictureOfTheDay: PictureOfTheDayUseCases = PictureOfTheDayUseCases(core.pictureOfTheDay, scope)

    /** Spaceflight news. */
    public val news: NewsUseCases = NewsUseCases(core.news, scope)

    /** Images for pictures and articles. */
    public val images: ImageUseCases = ImageUseCases(core.images, scope)
}

/** Image use cases. Promises never reject. */
@JsExport
public class ImageUseCases internal constructor(
    private val useCases: CoreImageUseCases,
    private val scope: CoroutineScope,
) {
    /** A URL to show [imageUrl] with, e.g. in `<img src>`. On the web, currently [imageUrl] itself. */
    public fun displayUrl(imageUrl: String): Promise<String> = scope.promise { useCases.displayUrl(imageUrl) }
}

/** Astronomy Picture of the Day use cases. Promises never reject; failures are a `PictureResult.Failure`. */
@JsExport
public class PictureOfTheDayUseCases internal constructor(
    private val useCases: CorePictureOfTheDayUseCases,
    private val scope: CoroutineScope,
) {
    /** The most recent picture. */
    public fun today(): Promise<PictureResult> = scope.promise { useCases.today() }

    /** The picture published on the given date (month 1–12, 1995-06-16 or later). */
    public fun forDate(year: Int, month: Int, day: Int): Promise<PictureResult> =
        scope.promise { useCases.forDate(year, month, day) }
}

/** Spaceflight news use cases. Promises never reject; failures are a `NewsResult.Failure`. */
@JsExport
public class NewsUseCases internal constructor(
    private val useCases: CoreNewsUseCases,
    private val scope: CoroutineScope,
) {
    /** The [count] most recent articles (1–50). Uses cached articles when available. */
    public fun latest(count: Int): Promise<NewsResult> = scope.promise { useCases.latest(count).toWeb() }

    /** Like [latest], but always fetches fresh articles. */
    public fun refresh(count: Int): Promise<NewsResult> = scope.promise { useCases.refresh(count).toWeb() }
}

/** Result of a news request, with a plain array of articles. */
@JsExport
public sealed class NewsResult {
    public class Success(public val articles: Array<NewsArticle>) : NewsResult()
    public class Failure(public val error: SpaceError) : NewsResult()
}

private fun CoreNewsResult.toWeb(): NewsResult = when (this) {
    is CoreNewsResult.Success -> NewsResult.Success(articles.toTypedArray())
    is CoreNewsResult.Failure -> NewsResult.Failure(error)
}
