// Experimental consumable SDK for iOS (via Swift export) and Web (via JS export).
// Logic only — no Compose — so consumers get a small, native-feeling API.
//
// Layering (see README "SDK architecture"):
//   com.example.sdk          public API: KmpSdk entry point, models, UI state holders
//   com.example.sdk.domain   internal use cases
//   com.example.sdk.data     internal repositories + data sources
plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

version = "0.1.0"

kotlin {
    explicitApi()

    iosArm64()
    iosSimulatorArm64()

    // Swift export (Alpha in Kotlin 2.4.x): Kotlin declarations become a real
    // Swift module instead of an Objective-C framework.
    swiftExport {
        moduleName = "KmpSdk"
        flattenPackage = "com.example.sdk"
    }

    // Kotlin/JS library with TypeScript definitions, packaged as an npm module.
    js {
        outputModuleName.set("kmp-sdk")
        browser()
        nodejs() // lets the common tests run headless with `jsNodeTest`
        binaries.library()
        useEsModules()
        generateTypeScriptDefinitions()
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
