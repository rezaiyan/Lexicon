plugins {
    id("lexicon.kmp.feature-ui")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":platforms"))
            implementation(project(":utils"))
            api(libs.lifecycle.runtime.compose)
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
