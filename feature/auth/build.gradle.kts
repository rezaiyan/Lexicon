plugins {
    id("lexicon.kmp.feature")
    id("lexicon.kmp.compose")
}

kotlin {
    sourceSets {
        val mobileMain by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(mobileMain)
        iosMain.get().dependsOn(mobileMain)

        commonMain.dependencies {
            implementation(project(":design-system"))
            implementation(project(":platforms"))
            implementation(project(":resources"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            api(libs.koin.compose)
            implementation(libs.kotlinx.coroutines.core)
        }

        mobileMain.dependencies {
            implementation(libs.kmpauth.google)
            implementation(libs.kmpauth.firebase)
            implementation(libs.kmpauth.uihelper)
            implementation(libs.gitlive.firebase.auth)
        }
    }
}
