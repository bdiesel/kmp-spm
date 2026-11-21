# kmp-spm

A small Kotlin Multiplatform + Compose Multiplatform demo that shares one Compose UI
across **Android**, **iOS**, and **Web (Wasm)**. On iOS, the shared Kotlin code is
delivered to Xcode as a **Swift Package Manager (SPM)** binary package, so the
project is named `kmp-spm`.

The app itself is a counter screen: it greets you with the current platform name
(for example `Hello from Android 35`, with the device's API level, or
`Hello from Web (Mozilla/5.0)`) and has a button that increments a count. The point of the project is the build
wiring, not the app.

## How it fits together

```
.
├── composeApp/                       # The single KMP module
│   └── src/
│       ├── commonMain/               # Shared Compose UI (App.kt) + expect fun platform()
│       ├── androidMain/              # MainActivity, AndroidManifest, actual platform()
│       ├── iosMain/                  # MainViewController() bridge + actual platform()
│       └── wasmJsMain/               # Browser entry point (Main.kt) + index.html
├── iosApp/iosApp/                    # SwiftUI shell that hosts the Compose view
├── Package.swift                     # Local SPM package wrapping the XCFramework
└── gradle/libs.versions.toml         # Every dependency and plugin version
```

- **Shared UI:** `App()` in `commonMain` is the whole UI. Each platform only
  provides an entry point and an `actual fun platform()`.
- **Android:** `composeApp` is also the Android application module.
  `MainActivity` calls `setContent { App() }`.
- **Web:** the `wasmJs` target compiles `App()` to WebAssembly and serves it from
  `index.html`.
- **iOS through SPM:** Xcode never runs Gradle. Instead:
  1. Gradle builds `composeApp/build/XCFrameworks/release/Shared.xcframework`.
  2. The root `Package.swift` exposes that XCFramework as a binary target named `Shared`.
  3. The Xcode app adds the repo root as a local package and does `import Shared`.
  4. `ContentView.swift` wraps the Kotlin `MainViewController()` in a
     `UIViewControllerRepresentable`, which is the `ComposeUIViewController { App() }`.

## Toolchain

| Component | Version |
| --- | --- |
| Gradle (wrapper) | 9.9.2 |
| Gradle daemon JDK | JetBrains JDK 21, auto-provisioned via the foojay resolver |
| Kotlin | 2.4.21 |
| Android Gradle Plugin | 9.5.2 |
| Compose Multiplatform | 1.12.2 (Material3 1.9.0) |
| Android compileSdk / targetSdk / minSdk | 37 / 37 / 24 |
| iOS targets | `iosArm64` (devices), `iosSimulatorArm64` (Apple Silicon simulators) |

All versions live in `gradle/libs.versions.toml`. You don't need a system-wide
Gradle or a specific JDK: the wrapper and the daemon toolchain settings
(`gradle/gradle-daemon-jvm.properties`) download what they need.

## Getting started

### Android

```sh
./gradlew :composeApp:installDebug
```

Or open the project in Android Studio and run the `composeApp` configuration.

### Web (Wasm)

```sh
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

Then open the URL it prints.

### iOS

**1. Build the XCFramework** (re-run whenever shared Kotlin code changes):

```sh
./gradlew :composeApp:assembleSharedReleaseXCFramework
# faster for day-to-day work:
./gradlew :composeApp:assembleSharedDebugXCFramework
```

`Package.swift` points at the `release/` output. If you build the debug variant,
change the path in `Package.swift` to `debug/`.

**2. Create the Xcode project (one-time).** The Swift sources are checked in at
`iosApp/iosApp/`, but there is no `.xcodeproj` yet. In Xcode:

1. **File ▸ New ▸ Project ▸ iOS App.** Name it `iosApp` and save it inside `iosApp/`.
2. Delete Xcode's generated `ContentView.swift` and `iosAppApp.swift`, then add
   the two files already in `iosApp/iosApp/`.
3. **File ▸ Add Package Dependencies… ▸ Add Local…** and select the repo root.
   Add the `Shared` product to the `iosApp` target.
4. Build and run on an Apple Silicon simulator or a device.

If `import Shared` doesn't resolve, the XCFramework from step 1 is missing. SPM
reads the binary target from disk, so it must be built first.

## Known limitations

- **AGP 9 compatibility flags.** `composeApp` applies both
  `com.android.application` and `kotlin.multiplatform`, which AGP 9 no longer
  supports by default. `gradle.properties` sets `android.builtInKotlin=false` and
  `android.newDsl=false` to keep this layout working, and the build prints
  deprecation warnings about them. The long-term fix is to move the Android app
  into its own `:androidApp` module and switch `composeApp` to the
  `com.android.kotlin.multiplatform.library` plugin
  ([guide](https://kotl.in/gradle/agp-new-kmp)). After that, both flags can be removed.
- **No Intel-Mac iOS simulator.** Compose Multiplatform 1.12 no longer publishes
  `iosX64`, so the target was removed. Use an Apple Silicon Mac or a real device.
- **Kotlin plugin warning about Gradle 10.** Kotlin 2.4.x triggers a Gradle 10
  incompatibility warning (`KotlinNativeBundleArtifactsTypes`). It is fixed in
  Kotlin 2.5.0.
- **SPM caches binary targets aggressively.** After rebuilding the XCFramework,
  you may need **File ▸ Packages ▸ Reset Package Caches** in Xcode.
- **Framework size.** A debug Compose iOS framework is large (tens of MB). Release
  builds are much smaller.
- **Configuration cache is on.** If a plugin misbehaves, try
  `--no-configuration-cache`.
