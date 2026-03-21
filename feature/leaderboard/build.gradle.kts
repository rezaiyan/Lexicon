plugins {
    id("lexicon.kmp.feature-ui")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":platforms"))
            implementation(libs.coil.compose)
        }
    }
}
