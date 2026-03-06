plugins {
    id("lexicon.kmp.library")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
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
            api(project(":platforms"))
            implementation(project(":core"))
            implementation(project(":utils"))
            implementation(libs.sqldelight.coroutines)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
        }

        mobileMain.dependencies {
            implementation(libs.purchases.kmp.core)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.androidx.security.crypto)
            implementation(libs.sqldelight.android.driver)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }

        val wasmJsMain by getting {
            dependencies {
                implementation(libs.sqldelight.web.worker.driver)
                implementation(libs.ktor.client.js)
                implementation(libs.kotlinx.browser)
                implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.0.2"))
                implementation(npm("sql.js", "1.8.0"))
                implementation(devNpm("copy-webpack-plugin", "9.1.0"))
            }
        }
    }
}

sqldelight {
    databases {
        create("LexiconDatabase") {
            packageName.set("data.core.database")
            generateAsync.set(true)
        }
    }
}
