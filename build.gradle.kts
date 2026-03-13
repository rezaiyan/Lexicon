plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.firebaseCrashlytics) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

kover {
    merge {
        allProjects()
    }
    reports {
        filters {
            excludes {
                classes(
                    "*_Factory",
                    "*_MembersInjector",
                    "*BuildConfig*",
                    "*ComposableSingletons*",
                )
                packages(
                    "di",
                    "theme",
                    "notification",
                    "com.alirezaiyan.vokab",
                )
                annotatedBy(
                    "androidx.compose.runtime.Composable",
                )
            }
        }
        verify {
            rule("Baseline coverage") {
                minBound(5)
            }
        }
    }
}

// Force Kotlin stdlib to match compiler version — prevents Kotlin/Wasm stdlib mismatch
// when transitive dependencies (e.g. SQLDelight) pull in a newer stdlib.
val kotlinVersion = libs.versions.kotlin.get()
subprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-stdlib")) {
                useVersion(kotlinVersion)
            }
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/detekt.yml"))
    baseline = file("$rootDir/detekt-baseline.xml")
    source.setFrom(
        files(
            fileTree("$rootDir") {
                include("**/src/*/kotlin/**/*.kt")
                exclude("**/build/**")
            }
        )
    )
}

// KAN-18: Enable SARIF output for GitHub Code Scanning annotations
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        sarif.required.set(true)
    }
}

tasks.register<Exec>("bumpVersion") {
    group = "lexicon"
    description = "Bump/sync version (versioning.properties + iOS Config.xcconfig). Use script with --hotfix|--minor|--major for bump."
    commandLine("bash", "scripts/bump-version.sh")
    workingDir = rootDir
}
