import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

// :space-api — HTTP clients generated from OpenAPI specs in ./openapi.
// Kotlin Multiplatform (Android + iOS) using Ktor and kotlinx.serialization.
// Consumed by :space-sdk as an `implementation` dependency, so generated
// types never appear in the SDK's public (Swift/Kotlin) API.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.openapi.generator)
}

/** One generated client per spec. Output goes to build/generated/openapi/<name>. */
fun registerClient(name: String, spec: String, pkg: String) =
    tasks.register<GenerateTask>("generate${name.replaceFirstChar(Char::uppercase)}Client") {
        group = "openapi"
        description = "Generates the $name Kotlin Multiplatform client from openapi/$spec"
        generatorName.set("kotlin")
        library.set("multiplatform") // Ktor + kotlinx.serialization
        inputSpec.set(layout.projectDirectory.file("openapi/$spec").asFile.path)
        outputDir.set(layout.buildDirectory.dir("generated/openapi/$name").get().asFile.path)
        packageName.set(pkg)
        generateApiTests.set(false)
        generateModelTests.set(false)
        generateApiDocumentation.set(false)
        generateModelDocumentation.set(false)
        configOptions.putAll(
            mapOf(
                "dateLibrary" to "kotlinx-datetime",
                "omitGradleWrapper" to "true",
            ),
        )
    }

// Our own spec, written from NASA's APOD feed guide (NASA publishes none).
val apod = registerClient("apod", "apod.yaml", "com.example.spaceapi.apod")

// Published spec, downloaded from https://api.spaceflightnewsapi.net/v4/schema/?format=json
// and checked in so builds are reproducible. Re-download to pick up API changes.
val spaceflightNews = registerClient(
    "spaceflightNews", "spaceflight-news.json", "com.example.spaceapi.spaceflightnews",
)

kotlin {
    android {
        namespace = "com.example.spaceapi"
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

    sourceSets {
        commonMain {
            // Generated sources; the map {} also makes compilation depend on generation.
            listOf(apod, spaceflightNews).forEach { client ->
                kotlin.srcDir(client.map { "${it.outputDir.get()}/src/commonMain/kotlin" })
            }
            dependencies {
                api(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
            }
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}
