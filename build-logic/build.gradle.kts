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
}

gradlePlugin {
    plugins {
        register("composeAppConvention") {
            id = "vokab.compose-app"
            implementationClass = "com.vokab.gradle.ComposeAppConventionPlugin"
        }
    }
}
