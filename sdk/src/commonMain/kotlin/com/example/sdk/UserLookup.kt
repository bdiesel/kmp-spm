@file:OptIn(ExperimentalJsExport::class)

package com.example.sdk

import com.example.sdk.domain.GetUserUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/** Everything a "look up a user" screen needs to render. */
@JsExport
public sealed class UserLookupUiState {
    public data object Idle : UserLookupUiState()
    public data class Loading(val id: String) : UserLookupUiState()
    public data class Success(val user: User) : UserLookupUiState()
    public data class NotFound(val id: String) : UserLookupUiState()
    public data class Error(val message: String) : UserLookupUiState()
}

/**
 * Shared UI-layer state holder (the KMP equivalent of a screen ViewModel).
 *
 * Unidirectional data flow: the native UI (SwiftUI, web, Compose) renders
 * [state] and sends events back with [lookUp]. This class is the single owner
 * of that state. Call [close] when the screen goes away.
 */
public class UserLookupStateHolder internal constructor(
    private val getUser: GetUserUseCase,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<UserLookupUiState>(UserLookupUiState.Idle)
    private var lookupJob: Job? = null

    /** Current state, for UIs that need a synchronous first frame. */
    public val currentState: UserLookupUiState get() = _state.value

    /** Emits the current state, then every change. */
    public val state: Flow<UserLookupUiState> = _state.asStateFlow()

    /** Event: look up [id]. A new lookup cancels the one in flight. */
    public fun lookUp(id: String) {
        lookupJob?.cancel()
        lookupJob = scope.launch {
            _state.value = UserLookupUiState.Loading(id)
            _state.value = when (val result = getUser(id)) {
                is UserResult.Found -> UserLookupUiState.Success(result.user)
                is UserResult.NotFound -> UserLookupUiState.NotFound(result.id)
                is UserResult.Failure -> UserLookupUiState.Error(result.message)
            }
        }
    }

    /** Cancels in-flight work. The holder can't be used afterwards. */
    public fun close() {
        scope.cancel()
    }
}
