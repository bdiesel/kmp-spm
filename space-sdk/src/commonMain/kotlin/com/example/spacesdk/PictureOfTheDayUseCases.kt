package com.example.spacesdk

import com.example.spacesdk.data.ApodRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
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
 * Live data: [observeToday] streams the stored picture and [refresh] updates
 * it from the network, so every observer sees the new picture.
 *
 * ```
 * sdk.pictureOfTheDay.observeToday().collect { picture -> show(picture) }
 * sdk.pictureOfTheDay.refresh()   // on screen start, pull-to-refresh…
 * ```
 *
 * Results are stored on the device. Never throws (except cancellation);
 * failures come back as [PictureResult.Failure] or [RefreshResult.Failure].
 */
public class PictureOfTheDayUseCases internal constructor(
    private val repository: ApodRepository,
) {
    /** The most recent picture. Falls back to the last one loaded when offline. */
    public suspend fun today(): PictureResult = load(date = null)

    /**
     * The stored most recent picture (null until one has been loaded), and
     * again each time it changes. Never fetches: call [refresh] for that.
     */
    public fun observeToday(): Flow<PictureOfTheDay?> = repository.observeLatest()

    /** Fetches the most recent picture; [observeToday] then emits it. */
    public suspend fun refresh(): RefreshResult = try {
        if (repository.refreshLatest()) RefreshResult.Success else RefreshResult.Failure(SpaceError.NOT_FOUND)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        RefreshResult.Failure(e.toSpaceError())
    }

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
