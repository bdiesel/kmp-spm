package com.example.spacesdk

/** A user known to the SDK. Immutable; the data layer owns changes. */
public data class User(val id: String, val name: String)

/**
 * Outcome of a user lookup. Sealed so Swift sees an enum it can `switch` over
 * exhaustively. Errors are values, not exceptions, so they survive the trip
 * across the language boundary.
 */
public sealed class UserResult {
    public data class Found(val user: User) : UserResult()
    public data class NotFound(val id: String) : UserResult()
    public data class Failure(val message: String) : UserResult()
}
