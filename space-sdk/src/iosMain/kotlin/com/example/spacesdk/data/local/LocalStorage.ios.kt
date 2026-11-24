package com.example.spacesdk.data.local

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.spacesdk.data.local.db.SpaceDatabase
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.Foundation.NSFileManager

// A SQLite file in the app's sandbox (Library/Application Support/databases),
// or in the App Group's container so a widget extension can read it too.
internal actual fun sqlDriverFactory(appGroup: String?): (() -> SqlDriver)? {
    val directory = appGroup?.let(::appGroupContainer)?.let { "$it/Library/Application Support/databases" }
    return { nativeSqlDriver(DATABASE_NAME, directory) }
}

/** Opens [name] in [directory], or in the default sandbox location when null. */
internal fun nativeSqlDriver(name: String, directory: String?): SqlDriver {
    directory?.let { SystemFileSystem.createDirectories(Path(it)) }
    return NativeSqliteDriver(
        schema = SpaceDatabase.Schema.synchronous(),
        name = name,
        onConfiguration = { config ->
            if (directory == null) config
            else config.copy(extendedConfig = config.extendedConfig.copy(basePath = directory))
        },
    )
}

/**
 * The App Group's shared folder, or null if this app isn't entitled to it. Null
 * means the SDK falls back to the app's own sandbox, so a wrong ID never crashes.
 */
internal fun appGroupContainer(appGroup: String): String? =
    NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier(appGroup)?.path
