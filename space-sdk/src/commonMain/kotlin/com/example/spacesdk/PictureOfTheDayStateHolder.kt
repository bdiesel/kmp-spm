package com.example.spacesdk

import com.example.spacesdk.domain.GetPictureOfTheDayUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Everything a picture-of-the-day screen needs to render. */
public sealed class PictureOfTheDayUiState {
    public data object Loading : PictureOfTheDayUiState()
    public data class Success(val picture: PictureOfTheDay) : PictureOfTheDayUiState()
    public data class NotFound(val date: String) : PictureOfTheDayUiState()
    public data class Error(val message: String) : PictureOfTheDayUiState()
}

/**
 * Shared UI-layer state holder for a picture-of-the-day screen (the KMP
 * equivalent of a ViewModel). Starts loading today's picture immediately.
 *
 * Unidirectional data flow: the native UI renders [state] and sends events
 * back with [loadLatest] and [load]. Call [close] when the screen goes away.
 */
public class PictureOfTheDayStateHolder internal constructor(
    private val getPicture: GetPictureOfTheDayUseCase,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<PictureOfTheDayUiState>(PictureOfTheDayUiState.Loading)
    private var job: Job? = null

    /** Current state, for UIs that need a synchronous first frame. */
    public val currentState: PictureOfTheDayUiState get() = _state.value

    /** Emits the current state, then every change. */
    public val state: Flow<PictureOfTheDayUiState> = _state.asStateFlow()

    init {
        loadLatest()
    }

    /** Event: show the most recent picture. */
    public fun loadLatest() {
        run(date = null)
    }

    /** Event: show the picture for [date] (`YYYY-MM-DD`). */
    public fun load(date: String) {
        run(date)
    }

    /** Cancels in-flight work. The holder can't be used afterwards. */
    public fun close() {
        scope.cancel()
    }

    private fun run(date: String?) {
        job?.cancel()
        job = scope.launch {
            _state.value = PictureOfTheDayUiState.Loading
            _state.value = when (val result = getPicture(date)) {
                is PictureOfTheDayResult.Success -> PictureOfTheDayUiState.Success(result.picture)
                is PictureOfTheDayResult.NotFound -> PictureOfTheDayUiState.NotFound(result.date)
                is PictureOfTheDayResult.Failure -> PictureOfTheDayUiState.Error(result.message)
            }
        }
    }
}
