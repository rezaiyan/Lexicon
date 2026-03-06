plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
            freeCompilerArgs.addAll(listOf(
                "-opt-in=kotlin.time.ExperimentalTime"
            ))
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.addAll(listOf(
                    "-opt-in=kotlin.time.ExperimentalTime"
                ))
            }
        }
        iosTarget.binaries.framework {
            baseName = "presentation"
            isStatic = true
        }
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }

    applyDefaultHierarchyTemplate()

    sourceSets.all {
        languageSettings {
            optIn("kotlin.time.ExperimentalTime")
        }
    }

    sourceSets {
        val mobileMain by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(mobileMain)
        iosMain.get().dependsOn(mobileMain)

        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(project(":design-system"))
            implementation(project(":utils"))
            implementation(project(":core"))
            implementation(project(":platforms"))
            implementation(project(":feature:auth"))
            implementation(project(":feature:subscription"))
            implementation(project(":feature:leaderboard"))
            implementation(project(":feature:profile"))
            implementation(project(":feature:onboarding"))
            implementation(project(":feature:study"))
            implementation(project(":feature:settings"))
            implementation(project(":feature:words"))
            implementation(project(":feature:import"))
            // Removed: presentation should only depend on domain, not data
            implementation(project(":resources"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)
            api(libs.navigation.compose)
            api(libs.lifecycle.runtime.compose)
            api(libs.lifecycle.viewmodel)
            api(libs.lifecycle.viewmodel.compose)
            api(libs.koin.core)
            api(libs.koin.compose)
            api(libs.koin.compose.viewmodel)
            implementation(libs.coil.compose)
            implementation(libs.kotlinx.datetime)
            implementation(libs.emoji.compose.m3)
            implementation(libs.compottie)
            implementation(libs.compottie.network)
            implementation(compose.material3AdaptiveNavigationSuite)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.kotlinxCoroutinesSwing.get()}")
        }

        mobileMain.dependencies {
            implementation(libs.kmpauth.google)
            implementation(libs.kmpauth.firebase)
            implementation(libs.kmpauth.uihelper)
            implementation(libs.gitlive.firebase.auth)
            implementation(libs.purchases.kmp.core)
        }

        androidMain.dependencies {
            implementation(compose.components.uiToolingPreview)
        }

        iosMain.dependencies {
        }
    }
}

android {
    namespace = "com.alirezaiyan.vokab.presentation"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    dependencies {
        debugImplementation(compose.uiTooling)
    }
}
