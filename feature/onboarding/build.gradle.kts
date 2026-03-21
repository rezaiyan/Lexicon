plugins {
    id("lexicon.kmp.feature-ui")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":utils"))
            implementation(project(":platforms"))
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
