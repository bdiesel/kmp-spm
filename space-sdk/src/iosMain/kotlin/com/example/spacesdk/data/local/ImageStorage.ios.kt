package com.example.spacesdk.data.local

import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

// Library/Caches, which iOS may clear when space runs low: the app's own, or
// the App Group container's so a widget extension shares the images.
internal actual fun imageCacheDirectory(appGroup: String?): String? {
    val caches = appGroup?.let(::appGroupContainer)?.let { "$it/Library/Caches" }
        ?: NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).firstOrNull() as? String
    return caches?.let { "$it/spacesdk-images" }
}

internal actual fun fileUrl(path: String): String = NSURL.fileURLWithPath(path).absoluteString ?: "file://$path"
