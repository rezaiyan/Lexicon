package com.vokab.gradle

import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.library")

        extensions.configure<KotlinMultiplatformExtension> {
            androidTarget {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }

            listOf(
                iosArm64(),
                iosSimulatorArm64()
            ).forEach { iosTarget ->
                iosTarget.binaries.framework {
                    baseName = target.path.removePrefix(":").replace(":", "-")
                    isStatic = true
                }
            }

            @OptIn(ExperimentalWasmDsl::class)
            wasmJs { browser() }

            applyDefaultHierarchyTemplate()

            sourceSets.configureEach {
                languageSettings {
                    optIn("kotlin.time.ExperimentalTime")
                    optIn("kotlin.io.encoding.ExperimentalEncodingApi")
                    optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
                    optIn("androidx.compose.material3.ExperimentalMaterial3Api")
                    optIn(
                        "androidx.compose.material3.adaptive.navigationsuite" +
                            ".ExperimentalMaterial3AdaptiveNavigationSuiteApi",
                    )
                    optIn("androidx.compose.ui.ExperimentalComposeUiApi")
                    optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                    optIn("kotlinx.cinterop.ExperimentalForeignApi")
                    optIn("kotlinx.cinterop.BetaInteropApi")
                    optIn("kotlin.js.ExperimentalWasmJsInterop")
                    optIn("androidx.compose.foundation.ExperimentalFoundationApi")
                    optIn("androidx.compose.animation.ExperimentalAnimationApi")
                    optIn("org.jetbrains.compose.resources.InternalResourceApi")
                }
            }
        }

        extensions.configure<LibraryExtension> {
            val modulePath = target.path.removePrefix(":").replace(":", ".").replace("-", "")
            namespace = "com.alirezaiyan.vokab.$modulePath"
                .split(".")
                .joinToString(".") { segment ->
                    if (segment in JAVA_KEYWORDS) "${segment}_" else segment
                }
            compileSdk = libs.findVersion("android-compileSdk").get().toString().toInt()
            defaultConfig {
                minSdk = libs.findVersion("android-minSdk").get().toString().toInt()
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
        }
    }

    companion object {
        private val JAVA_KEYWORDS = setOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new",
            "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while"
        )
    }
}
