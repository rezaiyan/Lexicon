plugins {
    id("lexicon.kmp.library")
    id("lexicon.kmp.compose")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        val mobileMain by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(mobileMain)
        iosMain.get().dependsOn(mobileMain)

        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(project(":design-system"))
            implementation(project(":utils"))
            implementation(project(":core"))
            implementation(project(":platforms"))
            implementation(project(":feature:auth"))
            implementation(project(":feature:subscription"))
            implementation(project(":feature:profile"))
            implementation(project(":feature:onboarding"))
            implementation(project(":feature:study"))
            implementation(project(":feature:settings"))
            implementation(project(":feature:words"))
            implementation(project(":feature:import"))
            implementation(project(":resources"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)
            api(libs.navigation.compose)
            api(libs.lifecycle.runtime.compose)
            api(libs.lifecycle.viewmodel)
            api(libs.lifecycle.viewmodel.compose)
            api(libs.koin.core)
            api(libs.koin.compose)
            api(libs.koin.compose.viewmodel)
            implementation(libs.coil.compose)
            implementation(libs.kotlinx.datetime)
            implementation(libs.emoji.compose.m3)
            implementation(libs.compottie)
            implementation(libs.compottie.network)
            implementation(compose.material3AdaptiveNavigationSuite)
            implementation(libs.kotlinx.coroutines.core)
        }

        mobileMain.dependencies {
            implementation(libs.kmpauth.google)
            implementation(libs.kmpauth.firebase)
            implementation(libs.kmpauth.uihelper)
            implementation(libs.gitlive.firebase.auth)
            implementation(libs.purchases.kmp.core)
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
