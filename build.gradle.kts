plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.firebaseCrashlytics) apply false
    alias(libs.plugins.detekt)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/detekt.yml"))
    baseline = file("$rootDir/detekt-baseline.xml")
    source.setFrom(
        files(
            fileTree("$rootDir") {
                include("**/src/*/kotlin/**/*.kt")
                exclude("**/build/**")
            }
        )
    )
}

tasks.register<Exec>("bumpVersion") {
    group = "lexicon"
    description = "Bump/sync version (versioning.properties + iOS Config.xcconfig). Use script with --hotfix|--minor|--major for bump."
    commandLine("bash", "scripts/bump-version.sh")
    workingDir = rootDir
}
