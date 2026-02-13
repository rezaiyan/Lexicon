package com.vokab.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

class ComposeAppConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

        plugins.withId("org.jetbrains.kotlin.multiplatform") {
            extensions.configure<KotlinMultiplatformExtension> {
                configureComposeAppSourceSets(versionCatalog)
            }
        }
    }

    private fun KotlinMultiplatformExtension.configureComposeAppSourceSets(
        libs: VersionCatalog
    ) {
        sourceSets.named("commonMain") {
            dependencies {
                apiBundle(libs, "composeAppCommonMainApi")
                implementationBundle(libs, "composeAppCommonMainImplementation")
            }
        }

        sourceSets.matching { it.name == "mobileMain" }.all {
            dependencies {
                implementationBundle(libs, "composeAppMobileMainImplementation")
            }
        }

        sourceSets.matching { it.name == "androidMain" }.all {
            dependencies {
                implementationBundle(libs, "composeAppAndroidMainImplementation")
            }
        }

        sourceSets.matching { it.name == "iosMain" }.all {
            dependencies {
                implementationBundle(libs, "composeAppIosMainImplementation")
            }
        }

        sourceSets.matching { it.name == "wasmJsMain" }.all {
            dependencies {
                implementationBundle(libs, "composeAppWasmJsMainImplementation")
            }
        }

        sourceSets.named("commonTest") {
            dependencies {
                implementationBundle(libs, "composeAppCommonTestImplementation")
            }
        }

        sourceSets.matching { it.name == "androidInstrumentedTest" }.all {
            dependencies {
                implementationBundle(libs, "composeAppAndroidInstrumentedTestImplementation")
            }
        }
    }
}

private fun KotlinDependencyHandler.apiBundle(
    libs: VersionCatalog,
    alias: String
) {
    libs.findBundle(alias).ifPresent { bundle ->
        bundle.get().forEach { dependency ->
            api(dependency)
        }
    }
}

private fun KotlinDependencyHandler.implementationBundle(
    libs: VersionCatalog,
    alias: String
) {
    libs.findBundle(alias).ifPresent { bundle ->
        bundle.get().forEach { dependency ->
            implementation(dependency)
        }
    }
}
