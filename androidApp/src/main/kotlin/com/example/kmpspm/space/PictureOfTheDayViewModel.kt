package com.example.kmpspm.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spacesdk.PictureOfTheDay
import com.example.spacesdk.PictureResult
import com.example.spacesdk.SpaceSdk
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/** What the Picture of the Day screen shows. Owned by the app, not the SDK. */
sealed interface PictureUiState {
    data object Loading : PictureUiState
    data class Shown(val picture: PictureOfTheDay) : PictureUiState
    data class Failed(val message: String) : PictureUiState
}

/** Screen logic for Picture of the Day: calls SpaceSdk's use cases, exposes UI state. */
class PictureOfTheDayViewModel(private val sdk: SpaceSdk = SpaceSdkProvider.sdk) : ViewModel() {
    private val _state = MutableStateFlow<PictureUiState>(PictureUiState.Loading)
    val state: StateFlow<PictureUiState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        showLatest()
    }

    fun showLatest() = load { sdk.pictureOfTheDay.today() }

    fun showPreviousDay() {
        val shown = (state.value as? PictureUiState.Shown)?.picture ?: return
        val previous = Calendar.getInstance().apply {
            set(shown.year, shown.month - 1, shown.day)
            add(Calendar.DAY_OF_MONTH, -1)
        }
        load {
            sdk.pictureOfTheDay.forDate(
                year = previous.get(Calendar.YEAR),
                month = previous.get(Calendar.MONTH) + 1,
                day = previous.get(Calendar.DAY_OF_MONTH),
            )
        }
    }

    private fun load(request: suspend () -> PictureResult) {
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = PictureUiState.Loading
            _state.value = when (val result = request()) {
                is PictureResult.Success -> PictureUiState.Shown(result.picture)
                is PictureResult.Failure -> PictureUiState.Failed(result.error.toMessage())
            }
        }
    }
}
