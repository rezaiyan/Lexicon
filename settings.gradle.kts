rootProject.name = "Lexicon"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

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
    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
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
    }
}

include(":composeApp")
include(":core")
include(":utils")
include(":test")
include(":platforms")
include(":domain")
include(":data")
include(":design-system")
include(":presentation")
include(":resources")
include(":feature:auth")
include(":feature:subscription")
include(":feature:leaderboard")
include(":feature:profile")
include(":feature:onboarding")
include(":feature:study")
include(":feature:settings")
include(":feature:words")
include(":feature:import")
includeBuild("build-logic")
