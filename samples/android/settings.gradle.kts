// A standalone Android app that consumes SpaceSdk the way any outside app
// would: from GitHub Packages, by version. It has no access to the SDK source.
rootProject.name = "SpaceSdkSample"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Unreleased SDK changes: run `./gradlew :space-sdk:publishToMavenLocal` in
        // the repo root, then build this app with -Pspacesdk.local.
        if (providers.gradleProperty("spacesdk.local").isPresent) {
            mavenLocal { content { includeGroup("io.github.bdiesel.spacesdk") } }
        }
        google()
        mavenCentral()
        // SpaceSdk is published to GitHub Packages, which needs a token even for
        // reads (scope read:packages). Put gpr.user / gpr.key in
        // ~/.gradle/gradle.properties, or set GITHUB_ACTOR / GITHUB_TOKEN.
        maven {
            name = "SpaceSdk"
            url = uri("https://maven.pkg.github.com/bdiesel/kmp-spm")
            credentials {
                username = providers.gradleProperty("gpr.user").orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                password = providers.gradleProperty("gpr.key").orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
            }
            content { includeGroup("io.github.bdiesel.spacesdk") }
        }
    }
}

include(":app")
