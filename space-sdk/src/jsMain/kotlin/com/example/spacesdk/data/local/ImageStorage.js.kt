package com.example.spacesdk.data.local

// Not yet: browsers have no folder to write files to. The web cache will use
// the browser's Cache Storage, a separate step. Until then displayUrl returns
// the original URL and the browser's own HTTP cache applies.
internal actual fun imageCacheDirectory(appGroup: String?): String? = null

internal actual fun fileUrl(path: String): String = error("No file system in the browser")
