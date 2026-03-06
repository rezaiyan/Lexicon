plugins {
    id("lexicon.kmp.feature")
    id("lexicon.kmp.compose")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":design-system"))
            implementation(project(":resources"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)
            api(libs.koin.compose)
            implementation(libs.navigation.compose)
            implementation(libs.coil.compose)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
