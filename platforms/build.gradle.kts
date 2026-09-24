import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("lexicon.kmp.library")
}

kotlin {
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
                includeDirs(project.file("src/nativeInterop/cinterop"))
            }
        }

        binaries.configureEach {
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

    targets.withType<KotlinNativeTarget>().configureEach {
        compilations.configureEach {
            compilerOptions.configure {
                freeCompilerArgs.addAll(listOf(
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi"
                ))
            }
        }
        configureSherpaOnnxCinterop()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(project(":domain"))
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
        }

        androidMain.dependencies {
            implementation(project(":core"))
            implementation(libs.androidx.security.crypto)
            implementation(libs.google.firebase.analytics)
            implementation(libs.google.firebase.config)
            implementation(libs.google.firebase.crashlytics)
            implementation(libs.google.firebase.messaging)
            implementation(libs.google.firebase.perf)
            implementation(files("libs/sherpa-onnx-1.12.26.aar"))
            implementation(libs.commons.compress)
            implementation(libs.work.runtime)
        }

        iosMain.dependencies {
            implementation(project(":core"))
        }

        wasmJsMain.dependencies {
            implementation(project(":core"))
        }
    }
}
