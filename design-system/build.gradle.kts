plugins {
    id("lexicon.kmp.library")
    id("lexicon.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(project(":resources"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(libs.compottie)
            implementation(libs.compottie.network)
        }
    }
}

compose.resources {
    publicResClass = true
}
