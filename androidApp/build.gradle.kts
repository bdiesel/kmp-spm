// Android application. Hosts the shared Compose UI from :composeApp.
// Kotlin compilation is built into AGP 9, so no Kotlin plugin is applied here.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.example.kmpspm"
    compileSdk {
        version = release(libs.versions.android.compileSdk.get().toInt()) {
            minorApiLevel = libs.versions.android.compileSdkMinor.get().toInt()
        }
    }

    defaultConfig {
        applicationId = "com.example.kmpspm"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            // R8: shrink, optimize and obfuscate code, and drop unused resources.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

// By default the sample builds against the SDK's source. With
// -Pspacesdk.usePublished=true it uses the published artifact instead (mavenLocal,
// then GitHub Packages), exactly as an outside app would.
val usePublishedSdk = providers.gradleProperty("spacesdk.usePublished").map(String::toBoolean).getOrElse(false)
val publishedSdk = providers.gradleProperty("spacesdk.group").get() + ":space-sdk:" +
    providers.gradleProperty("spacesdk.version").get()

dependencies {
    implementation(project(":composeApp"))
    // SpaceSdk as a regular Kotlin library: suspend functions, Flow and state holders directly.
    if (usePublishedSdk) implementation(publishedSdk) else implementation(project(":space-sdk"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
