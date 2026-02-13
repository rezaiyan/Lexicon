plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
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
        iosTarget.binaries.framework {
            baseName = "data"
            isStatic = true
        }
        iosTarget.compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.addAll(listOf(
                    "-opt-in=kotlin.time.ExperimentalTime"
                ))
            }
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
            api(project(":platforms"))
            implementation(project(":core"))
            implementation(project(":utils"))
            implementation(libs.sqldelight.coroutines)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.kotlinxCoroutinesSwing.get()}")
        }

        mobileMain.dependencies {
            implementation(libs.purchases.kmp.core)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.androidx.security.crypto)
            implementation(libs.sqldelight.android.driver)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }

        val wasmJsMain by getting {
            dependencies {
                implementation(libs.sqldelight.web.worker.driver)
                implementation(libs.ktor.client.js)
                implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
                implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.0.2"))
                implementation(npm("sql.js", "1.8.0"))
                implementation(devNpm("copy-webpack-plugin", "9.1.0"))
            }
        }
    }
}

android {
    namespace = "com.alirezaiyan.vokab.data"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

sqldelight {
    databases {
        create("LexiconDatabase") {
            packageName.set("data.core.database")
            generateAsync.set(true)
        }
    }
}
