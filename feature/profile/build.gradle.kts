plugins {
    id("lexicon.kmp.feature-ui")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":platforms"))
            implementation(project(":utils"))
            implementation(libs.coil.compose)
            implementation(libs.kotlinx.datetime)
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
