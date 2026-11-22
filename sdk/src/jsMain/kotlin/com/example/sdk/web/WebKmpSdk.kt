@file:OptIn(ExperimentalJsExport::class)

package com.example.sdk.web

import com.example.sdk.KmpSdk
import com.example.sdk.UserLookupStateHolder
import com.example.sdk.UserLookupUiState
import com.example.sdk.UserResult
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import kotlin.js.Promise

/**
 * JavaScript entry point. JS can't call `suspend` functions or collect a
 * `Flow`, so this adapts them to Promises and subscribe callbacks.
 */
@JsExport
public class WebKmpSdk {
    private val sdk = KmpSdk()
    private val scope = MainScope()

    public fun getUser(id: String): Promise<UserResult> =
        scope.promise { sdk.getUser(id) }

    public fun createUserLookup(): WebUserLookup = WebUserLookup(sdk.createUserLookup())

    /** Cancels pending Promises. */
    public fun close() {
        scope.cancel()
    }
}

/** JavaScript view of [UserLookupStateHolder]. */
@JsExport
public class WebUserLookup internal constructor(private val holder: UserLookupStateHolder) {
    private val scope = MainScope()

    public val currentState: UserLookupUiState get() = holder.currentState

    /** Calls [onState] with the current state and every change; returns an unsubscribe function. */
    public fun subscribe(onState: (UserLookupUiState) -> Unit): () -> Unit {
        val job = scope.launch { holder.state.collect { onState(it) } }
        return { job.cancel() }
    }

    public fun lookUp(id: String) {
        holder.lookUp(id)
    }

    public fun close() {
        scope.cancel()
        holder.close()
    }
}
