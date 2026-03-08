package com.vokab.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpFeatureConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("lexicon.kmp.library")

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.named("commonMain") {
                dependencies {
                    implementation(project(":domain"))
                    implementation(project(":core"))
                    api(libs.findLibrary("lifecycle-viewmodel").get())
                    api(libs.findLibrary("koin-core").get())
                    api(libs.findLibrary("koin-compose-viewmodel").get())
                    implementation(libs.findLibrary("kotlinx-coroutines-core").get())
                }
            }
        }
    }
}
