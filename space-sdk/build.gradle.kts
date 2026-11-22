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
}

version = "0.1.0"

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
        commonMain.dependencies {
            // `implementation` for Swift export: only the coroutine types our API uses
            // (Flow) are exported. As `api`, Swift export would bridge all of
            // kotlinx-coroutines as its own Swift module (and fails to compile it in 2.5.0-RC).
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            // Android apps see Flow in our public API, so they need coroutines on their classpath.
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
