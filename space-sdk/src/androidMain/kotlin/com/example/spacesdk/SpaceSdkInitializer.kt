package com.example.spacesdk

import android.content.Context
import androidx.startup.Initializer
import com.example.spacesdk.data.local.AppContext

/**
 * Runs automatically at app startup (androidx.startup) and remembers the
 * application Context, so `SpaceSdk()` can store its cache without the app
 * passing one in. Apps don't call this. Internal to Kotlin, but public in
 * bytecode, which is all androidx.startup's reflection needs.
 */
internal class SpaceSdkInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        AppContext.value = context.applicationContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
