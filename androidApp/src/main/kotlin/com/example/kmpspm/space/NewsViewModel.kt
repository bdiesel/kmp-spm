package com.example.kmpspm.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spacesdk.NewsArticle
import com.example.spacesdk.NewsResult
import com.example.spacesdk.SpaceSdk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the News screen shows. Owned by the app, not the SDK. */
data class NewsUiState(
    val articles: List<NewsArticle> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

/** Screen logic for News: calls SpaceSdk's use cases, exposes UI state. */
class NewsViewModel(private val sdk: SpaceSdk = SpaceSdkProvider.sdk) : ViewModel() {
    private val _state = MutableStateFlow(NewsUiState())
    val state: StateFlow<NewsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { show(sdk.news.latest(count = 20)) }
    }

    /** Pull-to-refresh / retry. Keeps the current list visible while loading. */
    fun refresh() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch { show(sdk.news.refresh(count = 20)) }
    }

    private fun show(result: NewsResult) {
        _state.update {
            when (result) {
                is NewsResult.Success -> NewsUiState(articles = result.articles, isLoading = false)
                is NewsResult.Failure -> it.copy(isLoading = false, error = result.error.toMessage())
            }
        }
    }
}
