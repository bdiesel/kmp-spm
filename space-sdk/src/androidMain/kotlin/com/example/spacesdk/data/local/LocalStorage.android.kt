package com.example.spacesdk.data.local

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.spacesdk.data.local.db.SpaceDatabase

/** The application Context, captured at app startup by SpaceSdkInitializer. */
internal object AppContext {
    @Volatile
    var value: Context? = null
}

// A SQLite file in the app's databases directory. If the Context wasn't
// captured (an app that disabled androidx.startup), the cache stays in memory.
// No App Groups on Android: widgets run in the app's process and share this file.
internal actual fun sqlDriverFactory(appGroup: String?): (() -> SqlDriver)? =
    AppContext.value?.let { context ->
        { AndroidSqliteDriver(SpaceDatabase.Schema.synchronous(), context, DATABASE_NAME) }
    }
