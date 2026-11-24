package com.example.spacesdk

import com.example.spacesdk.data.local.ImageCache

/**
 * Images for pictures and articles, cached by the SDK so they show offline.
 *
 * ```
 * AsyncImage(model = sdk.images.displayUrl(picture.imageUrl), ...)
 * ```
 *
 * The SDK keeps the most recently used images (`SpaceSdk(maxCachedImages)`,
 * 50 by default), plus today's picture, and removes the rest.
 */
public class ImageUseCases internal constructor(
    private val cache: ImageCache?,
) {
    /**
     * A URL to display [imageUrl] with: a local copy (downloaded now if
     * needed), or [imageUrl] itself when it can't be cached, e.g. offline
     * and never downloaded. Never fails, so it can go straight to an image view.
     */
    public suspend fun displayUrl(imageUrl: String): String = cache?.localUrl(imageUrl) ?: imageUrl
}
