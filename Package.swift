// swift-tools-version:5.9
import PackageDescription

// Local Swift Package exposing the Kotlin/Native XCFramework to Xcode/SPM.
//
// Build the XCFramework first from the project root:
//   ./gradlew :composeApp:assembleSharedReleaseXCFramework
// (or `assembleSharedDebugXCFramework` during dev)
//
// Then in Xcode: File ▸ Add Package Dependencies… ▸ Add Local… ▸ pick this folder.

let package = Package(
    name: "Shared",
    platforms: [.iOS(.v14)],
    products: [
        .library(name: "Shared", targets: ["Shared"]),
    ],
    targets: [
        .binaryTarget(
            name: "Shared",
            path: "composeApp/build/XCFrameworks/release/Shared.xcframework"
        ),
    ]
)
