package com.example.spacesdk.domain

import com.example.spacesdk.NewsResult
import com.example.spacesdk.PictureOfTheDayResult
import com.example.spacesdk.data.ApodRepository
import com.example.spacesdk.data.NewsRepository
import com.example.spacesdk.data.source.SpaceApiException
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate

/** APOD's first entry. Earlier dates can never exist. */
internal val FIRST_APOD_DATE = LocalDate(1995, 6, 16)

/**
 * Gets a picture of the day. Validates the date and turns every outcome,
 * including failures, into a [PictureOfTheDayResult].
 */
internal class GetPictureOfTheDayUseCase(private val repository: ApodRepository) {

    /** [date] is `YYYY-MM-DD`, or null for the most recent picture. */
    suspend operator fun invoke(date: String?): PictureOfTheDayResult {
        val day = date?.trim()?.let { text ->
            val parsed = runCatching { LocalDate.parse(text) }.getOrNull()
                ?: return PictureOfTheDayResult.Failure("Use a date like 2014-09-18")
            if (parsed < FIRST_APOD_DATE) {
                return PictureOfTheDayResult.Failure("APOD starts on $FIRST_APOD_DATE")
            }
            parsed
        }
        return try {
            repository.getPicture(day)
                ?.let { PictureOfTheDayResult.Success(it) }
                ?: PictureOfTheDayResult.NotFound(day?.toString() ?: "latest")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PictureOfTheDayResult.Failure(e.toUserMessage())
        }
    }
}

/** Gets the latest news. Clamps [limit] to 1–50 and never throws. */
internal class GetLatestNewsUseCase(private val repository: NewsRepository) {

    suspend operator fun invoke(limit: Int, forceRefresh: Boolean): NewsResult = try {
        NewsResult.Success(repository.getLatest(limit.coerceIn(1, 50), forceRefresh))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        NewsResult.Failure(e.toUserMessage())
    }
}

/**
 * Known service errors keep their (short) message. Anything else is a transport
 * failure whose platform message is long and technical (e.g. a raw NSError), so
 * consumers get a friendly sentence instead.
 */
private fun Exception.toUserMessage(): String = when (this) {
    is SpaceApiException -> "Couldn't load: $message"
    else -> "Couldn't reach the server. Check your connection and try again."
}
