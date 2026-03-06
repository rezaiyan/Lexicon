plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin", libs.versions.kotlin.get()))
    implementation("com.android.tools.build:gradle:${libs.versions.agp.get()}")
    implementation("org.jetbrains.compose:compose-gradle-plugin:${libs.versions.compose.plugin.get()}")
    implementation("org.jetbrains.kotlin:compose-compiler-gradle-plugin:${libs.versions.kotlin.get()}")
}

gradlePlugin {
    plugins {
        register("composeAppConvention") {
            id = "lexicon.compose-app"
            implementationClass = "com.vokab.gradle.ComposeAppConventionPlugin"
        }
        register("kmpLibrary") {
            id = "lexicon.kmp.library"
            implementationClass = "com.vokab.gradle.KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "lexicon.kmp.compose"
            implementationClass = "com.vokab.gradle.KmpComposeConventionPlugin"
        }
        register("kmpFeature") {
            id = "lexicon.kmp.feature"
            implementationClass = "com.vokab.gradle.KmpFeatureConventionPlugin"
        }
        register("kmpFeatureUi") {
            id = "lexicon.kmp.feature-ui"
            implementationClass = "com.vokab.gradle.KmpFeatureUiConventionPlugin"
        }
    }
}
