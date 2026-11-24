package com.example.spacesdk.data.local

import android.net.Uri
import java.io.File

// The app's cache folder, which Android may clear when space runs low.
internal actual fun imageCacheDirectory(appGroup: String?): String? =
    AppContext.value?.cacheDir?.let { File(it, "spacesdk-images").path }

internal actual fun fileUrl(path: String): String = Uri.fromFile(File(path)).toString()
