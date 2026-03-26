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
            // verifyMigrations is intentionally disabled.
            //
            // SQLDelight's verifyMigrations replays all .sqm files from an empty DB
            // starting with 1.sqm. However, 1.sqm is a v1→v2 migration that ALTER TABLEs
            // SettingsEntity — a table that doesn't exist in an empty DB.
            //
            // Root cause: when the project started, the initial schema was created directly
            // from Lexicon.sq (version 1) without a corresponding migration file.
            // Migration files 1.sqm–7.sqm cover v1→v2 through v7→v8.
            //
            // To enable verifyMigrations properly:
            //   1. Rename 1.sqm→2.sqm, 2.sqm→3.sqm … 7.sqm→8.sqm
            //   2. Add a new 1.sqm that creates the original v1 tables
            //      (WordEntity + SettingsEntity without tts/tag columns)
            //   3. Update the old 8.sqm (was 7.sqm) to use CREATE TABLE IF NOT EXISTS
            //      so existing users at user_version=7 don't crash on duplicate table
            // This is a coordinated migration surgery — track as a separate task.
        }
    }
}
