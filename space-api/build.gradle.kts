import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

// :space-api — HTTP clients generated from OpenAPI specs in ./openapi.
// Kotlin Multiplatform (Android + iOS) using Ktor and kotlinx.serialization.
//
// Not published on its own. :space-sdk compiles these generated sources into
// its own artifact (see the `openApiSources` configuration below), so SpaceSdk
// ships as ONE package. This module still compiles them too, to run the
// generated-client tests in isolation.
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
                // Generated classes are `internal`: hidden from SDK consumers at
                // the language level, not just by dependency scope.
                "nonPublicApi" to "true",
            ),
        )
        // Start from an empty folder, so files from earlier runs (e.g. a renamed
        // API class) don't linger and get compiled.
        cleanupOutput.set(true)
        // A few multiplatform templates (auth/*, Base64ByteArray, OctetByteArray)
        // ignore nonPublicApi; make every remaining top-level declaration internal.
        doLast {
            val topLevel = Regex(
                "^(?=(?:open |data |sealed |abstract |enum |inline |value )*(?:class|interface|object|fun|val|var|typealias) )",
                RegexOption.MULTILINE,
            )
            File(outputDir.get().toString()).walkTopDown().filter { it.extension == "kt" }.forEach { file ->
                val text = file.readText()
                val fixed = text.replace(topLevel, "internal ")
                if (fixed != text) file.writeText(fixed)
            }
        }
    }

// Our own spec, written from NASA's APOD feed guide (NASA publishes none).
val apod = registerClient("apod", "apod.yaml", "com.example.spaceapi.apod")

// Published spec, downloaded from https://api.spaceflightnewsapi.net/v4/schema/?format=json
// and checked in so builds are reproducible. Re-download to pick up API changes.
val spaceflightNews = registerClient(
    "spaceflightNews", "spaceflight-news.json", "com.example.spaceapi.spaceflightnews",
)

val clients = listOf(apod, spaceflightNews)
fun GenerateTask.kotlinSources(): File = File("${outputDir.get()}/src/commonMain/kotlin")

// AGP derives sibling folders (e.g. baselineProfiles) from each source dir and
// reads them without inheriting the dependency on the generators; declare it.
tasks.matching { it.name.endsWith("ArtProfile") }.configureEach {
    dependsOn(clients)
}

// Hands the generated source folders to :space-sdk (Gradle's supported way to
// share task outputs between projects; the folders carry their task dependency).
val openApiSources = configurations.create("openApiSources") {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("openapi-generated-sources"))
    }
}
clients.forEach { client ->
    artifacts.add(openApiSources.name, client.map { it.kotlinSources() }) {
        type = "directory"
        builtBy(client)
    }
}

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
            clients.forEach { client -> kotlin.srcDir(client.map { it.kotlinSources() }) }
            dependencies {
                implementation(libs.ktor.client.core)
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
