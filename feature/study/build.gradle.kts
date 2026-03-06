plugins {
    id("lexicon.kmp.feature")
    id("lexicon.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":design-system"))
            implementation(project(":platforms"))
            implementation(project(":resources"))
            implementation(project(":utils"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)
            api(libs.lifecycle.runtime.compose)
            api(libs.koin.compose)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
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
