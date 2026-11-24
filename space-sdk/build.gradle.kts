import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl

// SpaceSdk: a logic-only Kotlin Multiplatform SDK for Android and iOS.
//
//   Android  -> regular Kotlin library (AAR), full Kotlin API.
//   iOS      -> Swift export (Beta in Kotlin 2.5): a prebuilt, self-contained
//               Swift package (`exportDebugSwiftPackage` / `exportReleaseSwiftPackage`)
//               that apps add as a package dependency, with no Gradle in their build.
//
// Layering (see README "SpaceSdk architecture"):
//   com.example.spacesdk          public API: SpaceSdk entry point, models, UI state holders
//   com.example.spacesdk.domain   internal use cases
//   com.example.spacesdk.data     internal repositories + data sources
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization) // the generated models are @Serializable
    alias(libs.plugins.sqldelight)           // the offline cache (src/commonMain/sqldelight)
    `maven-publish`
}

// The cache database. Queries are generated as suspend-friendly (async) code so
// that a web driver, which can only run asynchronously, can share them later.
sqldelight {
    databases {
        create("SpaceDatabase") {
            packageName.set("com.example.spacesdk.data.local.db")
            generateAsync.set(true)
        }
    }
}

// The SQLDelight plugin adds its runtime (and async-extensions) to commonMain's
// *api*, with no opt-out. Swift export bridges api dependencies into the Swift
// package, and the bridge it generates for SQLDelight doesn't compile (two
// `Query(...)` overloads become one Swift declaration). They're implementation
// details, declared as `implementation` in commonMain below; drop them from api.
configurations.named("commonMainApi") {
    withDependencies { removeIf { it.group == "app.cash.sqldelight" } }
}

// The SDK's SQLite driver needs the system libsqlite3, but the Package.swift that
// Swift export writes doesn't link it (SQLDelight's linkSqlite only affects
// Kotlin-built frameworks). Add it to the target that wraps the Kotlin binary, so
// every app gets it from the package. Fails loudly if the file's layout changes.
listOf("Debug", "Release").forEach { variant ->
    val packageFile = layout.projectDirectory.file("SwiftPackage/$variant/Package.swift").asFile
    tasks.matching { it.name == "export${variant}SwiftPackage" }.configureEach {
        doLast {
            val text = packageFile.readText()
            val kotlinBinary = "dependencies: [\"SpaceSdkKotlin\"]"
            check(kotlinBinary in text) { "$packageFile changed layout: can't add libsqlite3" }
            if ("linkedLibrary(\"sqlite3\")" !in text) {
                packageFile.writeText(
                    text.replace(kotlinBinary, "$kotlinBinary,\n            linkerSettings: [.linkedLibrary(\"sqlite3\")]"),
                )
            }
        }
    }
}

// SQLDelight has no option to generate internal code, so its classes (SpaceDatabase,
// the *Queries, the row types) would be public API: visible to Android apps and
// exported to Swift. Right after generating, make each top-level declaration
// internal. Members stay `public` (effectively internal inside an internal type),
// which keeps overrides of SQLDelight's own public interfaces valid.
tasks.withType<app.cash.sqldelight.gradle.SqlDelightTask>().configureEach {
    val generated = outputDirectory
    doLast {
        val topLevelPublic = Regex("^public ", RegexOption.MULTILINE)
        generated.get().asFile.walk().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            file.writeText(file.readText().replace(topLevelPublic, "internal "))
        }
    }
}

// The OpenAPI-generated clients from :space-api are compiled INTO this module,
// so SpaceSdk publishes as one artifact. They are generated as `internal`.
val openApiSources = configurations.create("openApiSources") {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("openapi-generated-sources"))
    }
}
dependencies {
    openApiSources(project(":space-api"))
}

// AGP derives sibling folders (e.g. baselineProfiles) from each source dir;
// make those tasks wait for the generated sources too.
tasks.matching { it.name.endsWith("ArtProfile") }.configureEach {
    dependsOn(openApiSources)
}

group = providers.gradleProperty("spacesdk.group").get()
version = providers.gradleProperty("spacesdk.version").get()

// Android consumers: implementation("io.github.bdiesel.spacesdk:space-sdk:<version>")
// ./gradlew :space-sdk:publishToMavenLocal                      -> ~/.m2 (local testing)
// ./gradlew publishAllPublicationsToGitHubPackagesRepository    -> GitHub Packages
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/${providers.gradleProperty("spacesdk.githubRepo").get()}")
            credentials {
                username = providers.gradleProperty("gpr.user").orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                password = providers.gradleProperty("gpr.key").orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
            }
        }
    }
}

kotlin {
    explicitApi()

    android {
        namespace = "com.example.spacesdk"
        compileSdk {
            version = release(libs.versions.android.compileSdk.get().toInt()) {
                minorApiLevel = libs.versions.android.compileSdkMinor.get().toInt()
            }
        }
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    // Web: an ES-module npm package with TypeScript definitions
    // (build/dist/js/productionLibrary). jsMain adds the Promise-based facade.
    js {
        outputModuleName.set("space-sdk")
        browser()
        nodejs() // lets the common tests run headless with jsNodeTest
        binaries.library()
        useEsModules()
        generateTypeScriptDefinitions()
    }

    export {
        @OptIn(ExperimentalSwiftExportDsl::class)
        swift {
            moduleName = "SpaceSdk"
            // Drop the package prefix so Swift sees SpaceSdk.UserResult, not a long path.
            rootPackage = "com.example.spacesdk"

            // Registers :space-sdk:embedSwiftExportForXcode (Gradle runs inside the app build).
            xcodeIntegration { }

            // Registers :space-sdk:exportDebugSwiftPackage / exportReleaseSwiftPackage:
            // a standalone Swift package in space-sdk/SwiftPackage/{Debug,Release}.
            swiftPackageIntegration {
                outputDirectory = layout.projectDirectory.dir("SwiftPackage")
            }
        }
    }

    sourceSets {
        commonMain {
            // Generated sources from :space-api (internal); the configuration
            // carries the dependency on the generator tasks.
            kotlin.srcDir(openApiSources)
            dependencies {
                // `implementation` for Swift export: only the coroutine types our API uses
                // (Flow) are exported. As `api`, Swift export would bridge all of
                // kotlinx-coroutines as its own Swift module (and fails to compile it in 2.5.0-RC).
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                // What the generated clients need. `implementation`: never part of the API.
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.kotlinx.serialization.json)
                // Offline cache. `implementation`: never part of the API.
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.async.extensions)
                implementation(libs.sqldelight.coroutines.extensions) // queries as Flows (live data)
                implementation(libs.kotlinx.io.core) // image cache files (Android, iOS)
            }
        }
        androidMain.dependencies {
            // Android apps see Flow in our public API, so they need coroutines on their classpath.
            api(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android.driver)
            // Captures the application Context at startup, so SpaceSdk() needs no arguments.
            implementation(libs.androidx.startup)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
        jsMain.dependencies {
            implementation(libs.ktor.client.js) // fetch() in the browser
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}
