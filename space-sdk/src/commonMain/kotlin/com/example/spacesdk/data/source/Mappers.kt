package com.example.spacesdk.data.source

import com.example.spaceapi.apod.models.Apod
import com.example.spaceapi.spaceflightnews.models.Article
import com.example.spacesdk.MediaType
import com.example.spacesdk.NewsArticle
import com.example.spacesdk.PictureOfTheDay
import kotlinx.datetime.LocalDate

// Generated API models -> public SDK models. Generated types stop here.

internal fun Apod.toPictureOfTheDay() = PictureOfTheDay(
    date = date.toString(),
    title = title.htmlToPlainText(),
    explanation = explanation.htmlToPlainText().removePrefix("Explanation:").trim(),
    mediaType = when (mediaType.lowercase()) {
        "image" -> MediaType.IMAGE
        "video" -> MediaType.VIDEO
        else -> MediaType.OTHER
    },
    imageUrl = hdurl?.takeIf { it.isNotBlank() },
    pageUrl = permalink?.takeIf { it.isNotBlank() } ?: url,
    credit = credit.orEmpty().htmlToPlainText(),
    altText = alt.orEmpty().htmlToPlainText(),
)

internal fun Article.toNewsArticle() = NewsArticle(
    id = id,
    title = title.htmlToPlainText(),
    summary = summary.htmlToPlainText(),
    url = url,
    imageUrl = imageUrl.takeIf { it.isNotBlank() },
    newsSite = newsSite,
    publishedAt = publishedAt.toString(),
)

/** APOD's legacy request date code: 2014-09-18 -> "140918". */
internal fun LocalDate.toApodDateCode(): String =
    (year % 100).pad2() + month.ordinal.plus(1).pad2() + day.pad2()

private fun Int.pad2() = toString().padStart(2, '0')

private val lineBreak = Regex("(?i)<br\\s*/?>|</p>")
private val tag = Regex("<[^>]+>")
private val numericEntity = Regex("&#(x?)([0-9a-fA-F]+);")
private val whitespace = Regex("[ \\t\\u00A0]+")
private val namedEntities = mapOf(
    "&amp;" to "&", "&lt;" to "<", "&gt;" to ">", "&quot;" to "\"", "&apos;" to "'",
    "&nbsp;" to " ", "&ndash;" to "–", "&mdash;" to "—", "&hellip;" to "…",
    "&lsquo;" to "‘", "&rsquo;" to "’", "&ldquo;" to "“", "&rdquo;" to "”",
)

/** Strips tags and decodes entities so Swift and Compose get plain text. */
internal fun String.htmlToPlainText(): String {
    var text = replace(lineBreak, "\n").replace(tag, "")
    text = numericEntity.replace(text) { m ->
        val code = m.groupValues[2].toIntOrNull(if (m.groupValues[1].isEmpty()) 10 else 16)
        code?.let { Char(it).toString() } ?: m.value
    }
    namedEntities.forEach { (entity, char) -> text = text.replace(entity, char) }
    return text.lines().joinToString("\n") { it.replace(whitespace, " ").trim() }.trim()
}
