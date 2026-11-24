package com.example.spacesdk.data.local

import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

// Library/Caches, which iOS may clear when space runs low.
internal actual fun imageCacheDirectory(): String? =
    (NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).firstOrNull() as? String)
        ?.let { "$it/spacesdk-images" }

internal actual fun fileUrl(path: String): String = NSURL.fileURLWithPath(path).absoluteString ?: "file://$path"
