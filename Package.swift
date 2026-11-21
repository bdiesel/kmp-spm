// swift-tools-version:5.9
import PackageDescription

// Local Swift Package exposing the Kotlin/Native XCFramework to Xcode/SPM.
//
// The iosApp scheme rebuilds the XCFramework automatically before every build
// (scheme pre-action). To build it by hand from the project root:
//   ./gradlew :composeApp:assembleSharedDebugXCFramework
//
// For a release/distribution build, run assembleSharedReleaseXCFramework and
// change `debug` to `release` in the path below.

let package = Package(
    name: "Shared",
    platforms: [.iOS(.v15)],
    products: [
        .library(name: "Shared", targets: ["Shared"]),
    ],
    targets: [
        .binaryTarget(
            name: "Shared",
            path: "composeApp/build/XCFrameworks/debug/Shared.xcframework"
        ),
    ]
)
