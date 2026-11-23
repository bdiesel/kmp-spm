@file:OptIn(ExperimentalJsExport::class)

package com.example.spacesdk

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

// @JsExport only affects the JavaScript build (TypeScript definitions);
// Android and Swift see these types exactly as before.

/** What an Astronomy Picture of the Day entry shows. */
@JsExport
public enum class MediaType { IMAGE, VIDEO, OTHER }

/** NASA's Astronomy Picture of the Day. Text fields are plain text. */
@JsExport
public data class PictureOfTheDay(
    /** Publication date as `YYYY-MM-DD`. */
    val date: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val title: String,
    val explanation: String,
    val mediaType: MediaType,
    /** Full-size image, or null when the entry has none (e.g. a video). */
    val imageUrl: String?,
    /** The entry's page on nasa.gov. */
    val pageUrl: String,
    /** Image credit; may be empty. */
    val credit: String,
    /** Accessibility description; may be empty. */
    val altText: String,
)

/** A spaceflight news article. */
@JsExport
public data class NewsArticle(
    val id: Int,
    val title: String,
    val summary: String,
    val url: String,
    val imageUrl: String?,
    /** Publisher, e.g. "NASA" or "Spaceflight Now". */
    val newsSite: String,
    /** ISO-8601 timestamp, e.g. `2026-11-22T05:05:00Z`. */
    val publishedAt: String,
)

/** Why a request failed. */
@JsExport
public enum class SpaceError {
    /** The service couldn't be reached (offline, timeout, DNS…). */
    NETWORK,

    /** The service answered with an error. */
    SERVER,

    /** There's no picture for the requested date. */
    NOT_FOUND,

    /** The date doesn't exist or is before the first picture (1995-06-16). */
    INVALID_DATE,
}

/** Result of a picture-of-the-day request. */
@JsExport
public sealed class PictureResult {
    public data class Success(val picture: PictureOfTheDay) : PictureResult()
    public data class Failure(val error: SpaceError) : PictureResult()
}

/**
 * Result of a news request. (JavaScript gets its own `NewsResult` with a plain
 * array of articles; see the web package.)
 */
public sealed class NewsResult {
    public data class Success(val articles: List<NewsArticle>) : NewsResult()
    public data class Failure(val error: SpaceError) : NewsResult()
}
