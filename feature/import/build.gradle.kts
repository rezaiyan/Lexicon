plugins {
    id("lexicon.kmp.feature")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":platforms"))
            implementation(project(":utils"))
        }
    }
}

android {
    namespace = "com.alirezaiyan.vokab.feature.wordimport"
}
