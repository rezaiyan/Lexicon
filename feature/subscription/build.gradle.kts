plugins {
    id("lexicon.kmp.feature-ui")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":platforms"))
            implementation(libs.kotlinx.datetime)
            implementation(libs.lifecycle.runtime.compose)
        }

        androidMain.dependencies {
            implementation(compose.components.uiToolingPreview)
        }
    }
}

android {
    dependencies {
        debugImplementation(compose.uiTooling)
    }
}
