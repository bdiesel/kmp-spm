package com.example.spacesdk

import com.example.spacesdk.data.ApodRepository
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate

/** The first Astronomy Picture of the Day. */
internal val FIRST_APOD_DATE = LocalDate(1995, 6, 16)

/**
 * Use cases for NASA's Astronomy Picture of the Day.
 *
 * ```
 * when (val result = sdk.pictureOfTheDay.today()) {
 *     is PictureResult.Success -> show(result.picture)
 *     is PictureResult.Failure -> showError(result.error)
 * }
 * ```
 *
 * Results are cached for the lifetime of the [SpaceSdk]. Never throws
 * (except cancellation); failures come back as [PictureResult.Failure].
 */
public class PictureOfTheDayUseCases internal constructor(
    private val repository: ApodRepository,
) {
    /** The most recent picture. Falls back to the last one loaded when offline. */
    public suspend fun today(): PictureResult = load(date = null)

    /** The picture published on the given date (month 1–12, 1995-06-16 or later). */
    public suspend fun forDate(year: Int, month: Int, day: Int): PictureResult {
        val date = runCatching { LocalDate(year, month, day) }.getOrNull()
        if (date == null || date < FIRST_APOD_DATE) return PictureResult.Failure(SpaceError.INVALID_DATE)
        return load(date)
    }

    private suspend fun load(date: LocalDate?): PictureResult = try {
        repository.getPicture(date)
            ?.let { PictureResult.Success(it) }
            ?: PictureResult.Failure(SpaceError.NOT_FOUND)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        PictureResult.Failure(e.toSpaceError())
    }
}
