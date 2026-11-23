package com.example.kmpspm.space

import com.example.spacesdk.SpaceError
import com.example.spacesdk.SpaceSdk

/** One SDK instance for the whole app: it owns the network clients and caches. */
object SpaceSdkProvider {
    val sdk: SpaceSdk by lazy { SpaceSdk() }
}

/** The app decides how SDK errors read to its users. */
fun SpaceError.toMessage(): String = when (this) {
    SpaceError.NETWORK -> "Couldn't reach the server. Check your connection and try again."
    SpaceError.SERVER -> "The service had a problem. Try again later."
    SpaceError.NOT_FOUND -> "There's no picture for that day."
    SpaceError.INVALID_DATE -> "Pick a date from June 16, 1995 onward."
}
