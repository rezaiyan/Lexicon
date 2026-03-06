plugins {
    id("lexicon.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(libs.kotlin.test)
        }
    }
}
