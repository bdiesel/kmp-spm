# kmp-spm

A demo Kotlin Multiplatform + Compose Multiplatform project that runs on
**Android**, **iOS** (via Swift Package Manager), and **Web** (Wasm/JS).

## Layout

```
.
├── composeApp/                       # KMP module — Android app + iOS framework + Wasm web
│   └── src/
│       ├── commonMain/               # Shared Compose UI + business logic
│       ├── androidMain/              # Android entry point (MainActivity)
│       ├── iosMain/                  # ComposeUIViewController bridge for iOS
│       └── wasmJsMain/               # Wasm/JS entry point + index.html
├── iosApp/iosApp/                    # SwiftUI shell that hosts the Compose view
├── Package.swift                     # Local SPM package wrapping the XCFramework
└── gradle/, settings.gradle.kts, …   # Standard Gradle scaffolding
```

The iOS app does **not** depend on the Gradle/Kotlin output directly. Instead:

1. Gradle builds `composeApp/build/XCFrameworks/release/Shared.xcframework`.
2. The root `Package.swift` exposes that XCFramework as a Swift Package called `Shared`.
3. Xcode pulls in the SPM package as a local dependency.

## One-time bootstrap

### 1. Generate the Gradle wrapper

The wrapper JAR isn't committed yet. Pick one:

```sh
# Easiest — uses Android Studio's bundled Gradle
open -a "Android Studio" .
# Then File ▸ Sync Project with Gradle Files; AS will create gradlew + the wrapper JAR.
```

or from the command line:

```sh
brew install gradle
gradle wrapper --gradle-version 8.10.2 --distribution-type bin
```

### 2. Build the iOS XCFramework

```sh
./gradlew :composeApp:assembleSharedReleaseXCFramework
# or for faster iterative dev:
./gradlew :composeApp:assembleSharedDebugXCFramework
```

This produces `composeApp/build/XCFrameworks/release/Shared.xcframework` (or `debug/`).
If you use the debug variant, edit `Package.swift` to point at `debug/` instead.

### 3. Create the iOS Xcode project

The Swift sources live at `iosApp/iosApp/`, but the `.xcodeproj` isn't generated yet.

In Xcode:

1. **File ▸ New ▸ Project ▸ iOS App** — name it `iosApp`, save inside `iosApp/`, replacing the existing folder if asked. (Or save elsewhere and move the two `.swift` files in.)
2. Delete Xcode's generated `ContentView.swift` and `iosAppApp.swift`; add the two files already in `iosApp/iosApp/` instead.
3. **File ▸ Add Package Dependencies… ▸ Add Local…** — select the repo root (`/Users/brian/Dev/kmp-spm`). Add the `Shared` product to the `iosApp` target.
4. Build & run on a simulator.

> If the `Shared` import fails to resolve, make sure step 2 (`assembleShared…XCFramework`) ran first — SPM resolves the binary target by reading the file on disk.

## Running each platform

| Platform | Command |
| --- | --- |
| Android | `./gradlew :composeApp:installDebug` (or run from Android Studio) |
| iOS | Build & run `iosApp` from Xcode |
| Web (Wasm) | `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` then open the printed URL |

## Tweaking versions

All versions live in `gradle/libs.versions.toml`. Bumping Kotlin, AGP, or Compose
Multiplatform from a single place reduces compatibility surprises.

## Known sharp edges

- **XCFramework path** in `Package.swift` is hardcoded to `release/`. Switch to
  `debug/` for dev iteration (smaller, faster builds), but remember to rebuild
  whenever shared Kotlin code changes — SPM caches binary targets aggressively.
- **Compose iOS framework size**: a hello-world XCFramework is ~40–60 MB in
  debug. Release ARM64-only is much smaller.
- **Configuration cache** is on. If a plugin misbehaves, disable temporarily
  with `--no-configuration-cache`.
