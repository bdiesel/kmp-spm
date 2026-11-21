# kmp-spm

A small Kotlin Multiplatform + Compose Multiplatform demo that shares one Compose UI
across **Android**, **iOS**, and **Web (Wasm)**. On iOS, the shared Kotlin code is
delivered to Xcode as a **Swift Package Manager (SPM)** binary package, so the
project is named `kmp-spm`.

The app itself is a counter screen: it greets you with the current platform name
(for example `Hello from Android 35`, with the device's API level, or
`Hello from Web (Mozilla/5.0)`) and has a button that increments a count. The
point of the project is the build wiring, not the app.

## Architecture

One Compose UI in `commonMain` is compiled for three platforms. Each platform
adds only a thin entry point. On iOS, the Kotlin output goes through a Swift
Package before the app sees it.

```mermaid
flowchart TB
    subgraph common["composeApp · commonMain"]
        App["App()<br/>shared Compose UI"]
        Plat["expect fun platform()"]
    end

    subgraph android["Android · androidMain"]
        MA["MainActivity<br/>setContent { App() }"] --> APK["APK"]
    end

    subgraph web["Web · wasmJsMain"]
        WM["Main.kt<br/>ComposeViewport { App() }"] --> WASM["composeApp.wasm<br/>+ index.html"]
    end

    subgraph ios["iOS"]
        MVC["iosMain · MainViewController()<br/>ComposeUIViewController { App() }"]
        XCF["Shared.xcframework<br/>(Kotlin/Native, built by Gradle)"]
        PKG["Package.swift<br/>binaryTarget 'Shared'"]
        XP["iosApp.xcodeproj<br/>local package dependency"]
        CV["ContentView.swift<br/>import Shared · UIViewControllerRepresentable"]
        MVC --> XCF --> PKG --> XP --> CV
    end

    App --> MA
    App --> WM
    App --> MVC
    Plat -. "actual per platform" .-> android
    Plat -. "actual per platform" .-> web
    Plat -. "actual per platform" .-> ios
```

### What happens when you run the iOS app

Xcode never calls Kotlin directly. The scheme rebuilds the XCFramework first,
then the normal Xcode build links it through SPM:

```mermaid
sequenceDiagram
    actor Dev as You
    participant IDE as Android Studio / Xcode
    participant Pre as Scheme pre-action
    participant G as Gradle
    participant X as Xcode build
    participant Dev2 as Simulator / iPhone

    Dev->>IDE: Run iosApp
    IDE->>Pre: start iosApp scheme build
    Pre->>G: ./gradlew :composeApp:assembleSharedDebugXCFramework
    G-->>Pre: Shared.xcframework + gradle.log
    Pre->>Pre: write build/xcode-gradle/status (ok or failed)
    IDE->>X: build iosApp target
    X->>X: "Check Gradle XCFramework build" phase reads status
    alt Gradle failed
        X-->>IDE: build error + tail of gradle.log
    else Gradle succeeded
        X->>X: resolve local package Shared, compile Swift, link
        X-->>IDE: iosApp.app
        IDE->>Dev2: install and launch
    end
```

## Project layout

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
- **iOS through SPM:** the app links the Kotlin code as a prebuilt binary package:
  1. Gradle builds `composeApp/build/XCFrameworks/debug/Shared.xcframework`.
  2. The root `Package.swift` exposes that XCFramework as a binary target named `Shared`.
  3. `iosApp/iosApp.xcodeproj` depends on the repo root as a local package and
     does `import Shared`.
  4. `ContentView.swift` wraps the Kotlin `MainViewController()` in a
     `UIViewControllerRepresentable`, which is the `ComposeUIViewController { App() }`.

  The `iosApp` scheme has a pre-action that runs step 1 before every build, so
  Kotlin changes are picked up automatically. Xcode ignores pre-action failures,
  so the script records the result in `build/xcode-gradle/status`. The
  "Check Gradle XCFramework build" build phase then fails the build with the
  Gradle error if it failed. The full Gradle log is in `build/xcode-gradle/gradle.log`.

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

Requirements: a Mac with Xcode and its iOS platform component installed
(**Xcode ▸ Settings ▸ Components**, or `xcodebuild -downloadPlatform iOS`).

**First time only:** build the XCFramework once so Xcode can resolve the
`Shared` package when it opens the project:

```sh
./gradlew :composeApp:assembleSharedDebugXCFramework
```

After that, the scheme rebuilds it automatically.

**From Android Studio:** install the **Kotlin Multiplatform** plugin
(**Settings ▸ Plugins ▸ Marketplace**) and restart. Studio detects
`iosApp/iosApp.xcodeproj` and offers an `iosApp` run configuration. Pick a
simulator or a connected iPhone and click Run.

**From Xcode:** open `iosApp/iosApp.xcodeproj`, select the `iosApp` scheme and a
destination, and run.

**From the command line:**

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```

To run on a physical iPhone, choose your Apple ID team under **Signing &
Capabilities** in Xcode first. The checked-in project has no team set.

For a release build, run `assembleSharedReleaseXCFramework` and change `debug`
to `release` in `Package.swift`. The scheme pre-action always builds debug.

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
- **SPM caches binary targets aggressively.** If Xcode seems to run stale Kotlin
  code, use **File ▸ Packages ▸ Reset Package Caches**.
- **Framework size.** A debug Compose iOS framework is large (tens of MB). Release
  builds are much smaller.
- **Configuration cache is on.** If a plugin misbehaves, try
  `--no-configuration-cache`.
