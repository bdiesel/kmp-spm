@file:OptIn(ExperimentalJsExport::class)

package com.example.sdk

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/** A user known to the SDK. Immutable; the data layer owns changes. */
@JsExport
public data class User(val id: String, val name: String)

/**
 * Outcome of a user lookup. Sealed so Swift sees an enum it can `switch` over
 * exhaustively, and TypeScript sees a closed set of classes. Errors are values,
 * not exceptions, so they survive the trip across language boundaries.
 */
@JsExport
public sealed class UserResult {
    public data class Found(val user: User) : UserResult()
    public data class NotFound(val id: String) : UserResult()
    public data class Failure(val message: String) : UserResult()
}
