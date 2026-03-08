import java.util.Properties

plugins {
    id("lexicon.kmp.library")
    id("lexicon.kmp.compose")
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

    // Store values as task inputs for configuration cache compatibility
    // (avoids capturing the build script object in doLast)
    inputs.property("backendHost", backendHost)
    inputs.property("googleServerClientId", googleServerClientId)
    inputs.property("revenuecatAndroidKey", revenuecatAndroidKey)
    inputs.property("revenuecatIosKey", revenuecatIosKey)
    inputs.property("firebaseWebApiKey", firebaseWebApiKey)
    inputs.property("firebaseProjectId", firebaseProjectId)

    doLast {
        fun String.quoted(): String = "\"${replace("\"", "\\\"")}\""

        val props = inputs.properties
        val dir = outputDir.get().asFile.resolve("config")
        dir.mkdirs()
        dir.resolve("WasmBuildConfig.kt").writeText(
            """
            |package config
            |
            |internal object WasmBuildConfig {
            |    const val VOKAB_BACKEND_HOST: String = ${(props["backendHost"] as String).quoted()}
            |    const val GOOGLE_SERVER_CLIENT_ID: String = ${(props["googleServerClientId"] as String).quoted()}
            |    const val REVENUECAT_ANDROID_KEY: String = ${(props["revenuecatAndroidKey"] as String).quoted()}
            |    const val REVENUECAT_IOS_KEY: String = ${(props["revenuecatIosKey"] as String).quoted()}
            |    const val FIREBASE_WEB_API_KEY: String = ${(props["firebaseWebApiKey"] as String).quoted()}
            |    const val FIREBASE_PROJECT_ID: String = ${(props["firebaseProjectId"] as String).quoted()}
            |}
            """.trimMargin()
        )
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            api(libs.kotlinx.coroutines.core)
            api(libs.lifecycle.viewmodel)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.koin.core)
            implementation(libs.androidx.activity.compose)
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
    defaultConfig {
        buildConfigField("String", "VOKAB_BACKEND_HOST", backendHost.toQuotedLiteral())
        buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", googleServerClientId.toQuotedLiteral())
        buildConfigField("String", "REVENUECAT_ANDROID_KEY", revenuecatAndroidKey.toQuotedLiteral())
        buildConfigField("String", "REVENUECAT_IOS_KEY", revenuecatIosKey.toQuotedLiteral())
    }
    buildFeatures {
        buildConfig = true
    }
}
