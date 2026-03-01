import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
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

    fun KotlinNativeTarget.configureSherpaOnnxCinterop() {
        val archDir = when (name) {
            "iosArm64" -> "ios-arm64"
            "iosSimulatorArm64" -> "ios-arm64_x86_64-simulator"
            else -> error("Unsupported target: $name")
        }

        val sherpaRoot = project.file("libs/build-ios/sherpa-onnx.xcframework/$archDir")
        val onnxruntimeRoot = project.file("libs/build-ios/ios-onnxruntime/onnxruntime.xcframework/$archDir")

        compilations.getByName("main") {
            cinterops.create("sherpa_onnx") {
                definitionFile.set(project.file("src/nativeInterop/cinterop/sherpa_onnx.def"))
                includeDirs(sherpaRoot.resolve("Headers"))
            }
            cinterops.create("bz2") {
                definitionFile.set(project.file("src/nativeInterop/cinterop/bz2.def"))
                // Use our custom bz2_api.h header (system bzlib.h uses BZ_API() macro wrappers that confuse cinterop)
                includeDirs(project.file("src/nativeInterop/cinterop"))
            }
        }

        binaries.all {
            linkerOpts(
                "-L${sherpaRoot.absolutePath}", "-lsherpa-onnx",
                "-L${onnxruntimeRoot.absolutePath}", "-lonnxruntime",
                "-framework", "Foundation",
                "-framework", "Accelerate",
                "-framework", "CoreML",
                "-lbz2",
                "-lc++",
            )
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.addAll(listOf(
                    "-opt-in=kotlin.time.ExperimentalTime",
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi"
                ))
            }
        }
        iosTarget.binaries.framework {
            baseName = "platforms"
            isStatic = true
        }
        iosTarget.configureSherpaOnnxCinterop()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }

    sourceSets.all {
        languageSettings {
            optIn("kotlin.time.ExperimentalTime")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(libs.koin.core)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.kotlinxCoroutinesSwing.get()}")
        }

        androidMain.dependencies {
            implementation(project(":core"))
            implementation(libs.androidx.security.crypto)
            implementation(libs.google.firebase.analytics)
            implementation(libs.google.firebase.crashlytics)
            implementation(libs.google.firebase.messaging)
            implementation(files("libs/sherpa-onnx-1.12.26.aar"))
            implementation(libs.commons.compress)
        }

        iosMain.dependencies {
            implementation(project(":core"))
        }

        wasmJsMain.dependencies {
            implementation(project(":core"))
        }
    }
}

android {
    namespace = "com.alirezaiyan.vokab.platforms"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
