import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

private val localProperties: Properties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

private fun getConfigValue(key: String): String? =
    System.getenv(key.uppercase().replace('.', '_'))?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty(key)

private fun readableKeyName(key: String): String = "$key (env ${key.uppercase().replace('.', '_')})"

private fun requireConfigValue(key: String): String =
    getConfigValue(key)?.takeIf { it.isNotBlank() }
        ?: error("Missing '${readableKeyName(key)}'. Provide it via environment variable or local.properties.")

private fun String.toQuotedLiteral(): String = "\"${replace("\"", "\\\"")}\""

private fun parseHost(url: String): String =
    url.removePrefix("https://")
        .removePrefix("http://")
        .substringBefore('/')

private val backendHost: String = getConfigValue("vokab.backend.host")
    ?: getConfigValue("vokab.backend.url")?.let(::parseHost)
    ?: error("Missing '${readableKeyName("vokab.backend.host")}' or '${readableKeyName("vokab.backend.url")}'. Provide one of them.")
private val googleServerClientId: String = requireConfigValue("GOOGLE_SERVER_CLIENT_ID")
private val revenuecatAndroidKey: String = requireConfigValue("REVENUECAT_ANDROID_KEY")
private val revenuecatIosKey: String = requireConfigValue("REVENUECAT_IOS_KEY")
private val firebaseWebApiKey: String = getConfigValue("FIREBASE_WEB_API_KEY") ?: ""
private val firebaseProjectId: String = getConfigValue("FIREBASE_PROJECT_ID") ?: ""

val generatedWasmConfigDir = layout.buildDirectory.dir("generated/wasmJsConfig")

val generateWasmJsBuildConfig by tasks.registering {
    group = "configuration"
    description = "Generates BuildConfig for wasmJs from local.properties"

    val outputDir = generatedWasmConfigDir
    outputs.dir(outputDir)

    doLast {
        val dir = outputDir.get().asFile.resolve("config")
        dir.mkdirs()
        dir.resolve("WasmBuildConfig.kt").writeText(
            """
            |package config
            |
            |internal object WasmBuildConfig {
            |    const val VOKAB_BACKEND_HOST: String = ${backendHost.toQuotedLiteral()}
            |    const val GOOGLE_SERVER_CLIENT_ID: String = ${googleServerClientId.toQuotedLiteral()}
            |    const val REVENUECAT_ANDROID_KEY: String = ${revenuecatAndroidKey.toQuotedLiteral()}
            |    const val REVENUECAT_IOS_KEY: String = ${revenuecatIosKey.toQuotedLiteral()}
            |    const val FIREBASE_WEB_API_KEY: String = ${firebaseWebApiKey.toQuotedLiteral()}
            |    const val FIREBASE_PROJECT_ID: String = ${firebaseProjectId.toQuotedLiteral()}
            |}
            """.trimMargin()
        )
    }
}

kotlin {
    androidTarget {
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            freeCompilerArgs.addAll(listOf(
                "-opt-in=kotlin.time.ExperimentalTime"
            ))
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.addAll(listOf(
                        "-opt-in=kotlin.time.ExperimentalTime"
                    ))
                }
            }
        }
        iosTarget.binaries.framework {
            baseName = "core"
            isStatic = true
        }
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets.all {
        languageSettings {
            optIn("kotlin.time.ExperimentalTime")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.koin.core)
            implementation(libs.androidx.activity.compose)
        }

        iosMain.dependencies {
        }

        val wasmJsMain by getting {
            kotlin.srcDir(generatedWasmConfigDir)
        }
    }
    jvmToolchain(11)
}

tasks.matching { it.name.startsWith("compileKotlinWasmJs") }.configureEach {
    dependsOn(generateWasmJsBuildConfig)
}

android {
    namespace = "com.alirezaiyan.vokab.core"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        buildConfigField("String", "VOKAB_BACKEND_HOST", backendHost.toQuotedLiteral())
        buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", googleServerClientId.toQuotedLiteral())
        buildConfigField("String", "REVENUECAT_ANDROID_KEY", revenuecatAndroidKey.toQuotedLiteral())
        buildConfigField("String", "REVENUECAT_IOS_KEY", revenuecatIosKey.toQuotedLiteral())
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
