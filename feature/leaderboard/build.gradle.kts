plugins {
    id("lexicon.kmp.feature-ui")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.coil.compose)
        }
    }
}
