rootProject.name = "kmp-spm"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        // Published SpaceSdk, used by the samples with -Pspacesdk.usePublished=true.
        // Content filters keep these repos from being queried for anything else.
        mavenLocal {
            content { includeGroup("io.github.bdiesel.spacesdk") }
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/bdiesel/kmp-spm")
            // GitHub Packages needs a token even for reads (scope read:packages).
            credentials {
                username = providers.gradleProperty("gpr.user").orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                password = providers.gradleProperty("gpr.key").orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
            }
            content { includeGroup("io.github.bdiesel.spacesdk") }
        }
    }
}

include(":composeApp")
include(":androidApp")
include(":space-sdk")
include(":space-api")
