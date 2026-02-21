
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
    id("lexicon.compose-app")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

fun getConfigValue(key: String): String? =
    System.getenv(key.uppercase().replace('.', '_'))?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty(key)

fun formattedKeyName(key: String): String = "$key (env ${key.uppercase().replace('.', '_')})"

val versioningProperties = Properties().apply {
    val versioningFile = rootProject.file("versioning.properties")
    check(versioningFile.exists()) {
        "Missing versioning.properties at ${versioningFile.absolutePath}"
    }
    versioningFile.inputStream().use { load(it) }
}

val applicationIdValue = requireNotNull(
    versioningProperties.getProperty("applicationId")
) { "applicationId is required in versioning.properties" }

val versionCodeValue = requireNotNull(
    versioningProperties.getProperty("versionCode")
) { "versionCode is required in versioning.properties" }.toInt()

val versionNameValue = requireNotNull(
    versioningProperties.getProperty("versionName")
) { "versionName is required in versioning.properties" }


kotlin {
    androidTarget {
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            freeCompilerArgs.addAll(listOf(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-opt-in=kotlin.time.ExperimentalTime",
                "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
                "-opt-in=kotlin.io.encoding.ExperimentalEncodingApi"
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
                        "-opt-in=kotlin.RequiresOptIn",
                        "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                        "-opt-in=kotlin.time.ExperimentalTime",
                        "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                        "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
                        "-opt-in=kotlin.io.encoding.ExperimentalEncodingApi"
                    ))
                }
            }
        }
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true

            // Set bundle ID explicitly for iOS framework
            binaryOption("bundleId", applicationIdValue)

            linkerOpts.add("-lsqlite3")

            export(libs.lifecycle.viewmodel)
            export(libs.lifecycle.runtime.compose)
            export(libs.lifecycle.viewmodel.compose)
            export(libs.navigation.compose)
            export(libs.koin.core)
            export(libs.koin.compose)
            export(libs.koin.compose.viewmodel)

            transitiveExport = true
        }
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                devServer = devServer?.copy(
                    open = mapOf("app" to mapOf("name" to "Google Chrome"))
                )
            }
        }
        binaries.executable()
    }

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

        androidMain.dependencies {
            implementation(compose.preview)
        }
        commonMain.dependencies {
            implementation(project(":presentation"))
            implementation(project(":domain"))
            implementation(project(":data"))
            implementation(project(":platforms"))
            implementation(project(":design-system"))
            implementation(project(":utils"))
            implementation(project(":core"))
            implementation(project(":resources"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.components.resources)
        }

        commonTest.dependencies {
            implementation(kotlin("test-common"))
            implementation(kotlin("test-annotations-common"))
        }
    }
    jvmToolchain(11)
}

android {
    namespace = "com.alirezaiyan.vokab"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = applicationIdValue
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = versionCodeValue
        versionName = versionNameValue

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        create("release") {
            val storeFileProp = getConfigValue("RELEASE_STORE_FILE")?.trim()?.takeIf { it.isNotBlank() }
            val storePass = getConfigValue("RELEASE_STORE_PASSWORD")?.takeIf { it.isNotBlank() }
            val keyAliasProp = getConfigValue("RELEASE_KEY_ALIAS")?.trim()?.takeIf { it.isNotBlank() }
            val keyPass = getConfigValue("RELEASE_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
            if (storeFileProp != null && storePass != null && keyAliasProp != null && keyPass != null) {
                storeFile = rootProject.file(storeFileProp)
                storePassword = storePass
                keyAlias = keyAliasProp
                keyPassword = keyPass
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    dependencies {
        debugImplementation(compose.uiTooling)
    }
}

val generateIosConfig by tasks.registering {
    group = "configuration"
    description = "Generates Config.private.xcconfig and google-services.json from local.properties"

    val generatedConfig = rootProject.layout.projectDirectory.file("iosApp/Configuration/Config.private.xcconfig")
    val googleServicesTemplate = rootProject.layout.projectDirectory.file("composeApp/google-services.template.json")
    val googleServicesOutput = rootProject.layout.projectDirectory.file("composeApp/google-services.json")

    inputs.file(googleServicesTemplate)
    outputs.file(generatedConfig)
    outputs.file(googleServicesOutput)

    doLast {
        check(googleServicesTemplate.asFile.exists()) {
            "Missing google-services template at ${googleServicesTemplate.asFile.absolutePath}"
        }

        val xcconfigMappings = listOf(
            "GID_CLIENT_ID" to "GOOGLE_IOS_CLIENT_ID",
            "GID_SERVER_CLIENT_ID" to "GOOGLE_SERVER_CLIENT_ID",
            "GID_URL_SCHEME" to "GOOGLE_IOS_URL_SCHEME",
            "VOKAB_BACKEND_HOST" to "vokab.backend.host",
            "REVENUECAT_ANDROID_KEY" to "REVENUECAT_ANDROID_KEY",
            "REVENUECAT_IOS_KEY" to "REVENUECAT_IOS_KEY"
        )

        val googleServicesMappings = mapOf(
            "\${FIREBASE_ANDROID_API_KEY}" to "FIREBASE_ANDROID_API_KEY",
            "\${GOOGLE_SERVER_CLIENT_ID}" to "GOOGLE_SERVER_CLIENT_ID",
            "\${GOOGLE_IOS_CLIENT_ID}" to "GOOGLE_IOS_CLIENT_ID",
            "\${FIREBASE_PROJECT_NUMBER}" to "FIREBASE_PROJECT_NUMBER",
            "\${FIREBASE_PROJECT_ID}" to "FIREBASE_PROJECT_ID",
            "\${FIREBASE_STORAGE_BUCKET}" to "FIREBASE_STORAGE_BUCKET",
            "\${FIREBASE_MOBILE_SDK_APP_ID}" to "FIREBASE_MOBILE_SDK_APP_ID",
            "\${ANDROID_PACKAGE_NAME}" to "ANDROID_PACKAGE_NAME",
            "\${GOOGLE_ANDROID_CLIENT_ID_RELEASE}" to "GOOGLE_ANDROID_CLIENT_ID_RELEASE",
            "\${ANDROID_CERT_HASH_RELEASE}" to "ANDROID_CERT_HASH_RELEASE",
            "\${GOOGLE_ANDROID_CLIENT_ID_DEBUG}" to "GOOGLE_ANDROID_CLIENT_ID_DEBUG",
            "\${ANDROID_CERT_HASH_DEBUG}" to "ANDROID_CERT_HASH_DEBUG",
            "\${IOS_BUNDLE_ID}" to "IOS_BUNDLE_ID",
            "\${IOS_APP_STORE_ID}" to "IOS_APP_STORE_ID"
        )

        fun parseHost(url: String): String =
            url.removePrefix("https://")
                .removePrefix("http://")
                .substringBefore('/')

        val backendHost = getConfigValue("vokab.backend.host")?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: getConfigValue("vokab.backend.url")?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let(::parseHost)
            ?: ""

        val requiredKeys = buildSet {
            xcconfigMappings.filterNot { it.first == "VOKAB_BACKEND_HOST" }
                .forEach { add(it.second) }
            googleServicesMappings.values.forEach { add(it) }
        }

        val configValues = requiredKeys.associateWith { key ->
            getConfigValue(key)?.trim().orEmpty()
        }
        val missingKeys = configValues.filterValues { it.isBlank() }.keys
        check(missingKeys.isEmpty() && backendHost.isNotBlank()) {
            "Missing required configuration keys: ${missingKeys.joinToString { formattedKeyName(it) }}. " +
                "Also require either '${formattedKeyName("vokab.backend.host")}' or '${formattedKeyName("vokab.backend.url")}'. " +
                "Provide environment variables or entries in local.properties."
        }

        val content = buildString {
            appendLine("// Generated by composeApp:generateIosConfig")
            appendLine("// Do not commit this file.")
            xcconfigMappings.forEach { (xcKey, propertyKey) ->
                val value = if (xcKey == "VOKAB_BACKEND_HOST") backendHost else configValues.getValue(propertyKey)
                appendLine("$xcKey = $value")
            }
        }

        val outputFile: File = generatedConfig.asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(content)

        fun escapeJson(input: String): String = buildString {
            input.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
        }

        var googleServicesContent = googleServicesTemplate.asFile.readText()
        googleServicesMappings.forEach { (placeholder, propertyKey) ->
            val value = configValues.getValue(propertyKey)
            googleServicesContent = googleServicesContent.replace(placeholder, escapeJson(value))
        }

        val googleServicesFile = googleServicesOutput.asFile
        googleServicesFile.parentFile.mkdirs()
        googleServicesFile.writeText(googleServicesContent)
    }
}

val iosConfigTriggerFragments = listOf(
    "embedAndSignAppleFrameworkForXcode",
    "syncFramework",
    "assembleXCFramework"
)

tasks.configureEach {
    if (iosConfigTriggerFragments.any { fragment ->
            name.contains(fragment, ignoreCase = true)
        }) {
        dependsOn(generateIosConfig)
    }
    if (name.startsWith("process") && name.endsWith("GoogleServices")) {
        dependsOn(generateIosConfig)
    }
}

// Disable iOS test tasks to avoid GoogleSignIn framework linking issues in CI
tasks.configureEach {
    if (name.contains("Test", ignoreCase = true) &&
        name.contains("Ios", ignoreCase = true) &&
        (name.contains("link", ignoreCase = true) || name.contains("compile", ignoreCase = true))) {
        enabled = false
    }
}

// Allow custom index.html to override the auto-generated one for wasmJs
tasks.named("wasmJsProcessResources", Copy::class) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
