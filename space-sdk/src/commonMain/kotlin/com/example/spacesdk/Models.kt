package com.example.spacesdk

/** What an APOD entry shows. A plain enum, so Swift gets a native enum. */
public enum class MediaType { IMAGE, VIDEO, OTHER }

/**
 * NASA's Astronomy Picture of the Day.
 *
 * Text fields are plain text (the SDK strips the HTML the API returns).
 * Dates are ISO-8601 strings (`2026-11-22`) so the type crosses into Swift
 * without exporting a date library.
 */
public data class PictureOfTheDay(
    val date: String,
    val title: String,
    val explanation: String,
    val mediaType: MediaType,
    /** Full-size image, or null when the entry has none. */
    val imageUrl: String?,
    /** The APOD web page for this entry. */
    val pageUrl: String,
    /** Image credit, may be empty. */
    val credit: String,
    /** Accessibility description, may be empty. */
    val altText: String,
)

/** A spaceflight news article from the Spaceflight News API. */
public data class NewsArticle(
    val id: Int,
    val title: String,
    val summary: String,
    val url: String,
    val imageUrl: String?,
    val newsSite: String,
    /** ISO-8601 timestamp, e.g. `2026-11-22T05:05:00Z`. */
    val publishedAt: String,
)

/** Outcome of a picture-of-the-day request. Errors are values, never exceptions. */
public sealed class PictureOfTheDayResult {
    public data class Success(val picture: PictureOfTheDay) : PictureOfTheDayResult()
    public data class NotFound(val date: String) : PictureOfTheDayResult()
    public data class Failure(val message: String) : PictureOfTheDayResult()
}

/** Outcome of a news request. Errors are values, never exceptions. */
public sealed class NewsResult {
    public data class Success(val articles: List<NewsArticle>) : NewsResult()
    public data class Failure(val message: String) : NewsResult()
}
