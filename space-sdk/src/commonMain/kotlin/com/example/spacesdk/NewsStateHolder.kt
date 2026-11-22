package com.example.spacesdk

import com.example.spacesdk.domain.GetLatestNewsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Everything a news list screen needs to render. */
public sealed class NewsUiState {
    public data object Loading : NewsUiState()
    public data class Success(val articles: List<NewsArticle>) : NewsUiState()
    public data class Error(val message: String) : NewsUiState()
}

/**
 * Shared UI-layer state holder for a news list. Starts loading immediately.
 * Render [state]; call [refresh] for pull-to-refresh; call [close] when done.
 */
public class NewsStateHolder internal constructor(
    private val getNews: GetLatestNewsUseCase,
    private val limit: Int,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    private var job: Job? = null

    public val currentState: NewsUiState get() = _state.value

    public val state: Flow<NewsUiState> = _state.asStateFlow()

    init {
        load(forceRefresh = false)
    }

    /** Event: fetch the latest articles, bypassing the cache. */
    public fun refresh() {
        load(forceRefresh = true)
    }

    public fun close() {
        scope.cancel()
    }

    private fun load(forceRefresh: Boolean) {
        job?.cancel()
        job = scope.launch {
            // Keep showing the current list while refreshing.
            if (_state.value !is NewsUiState.Success) _state.value = NewsUiState.Loading
            _state.value = when (val result = getNews(limit, forceRefresh)) {
                is NewsResult.Success -> NewsUiState.Success(result.articles)
                is NewsResult.Failure -> NewsUiState.Error(result.message)
            }
        }
    }
}
