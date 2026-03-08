plugins {
    id("lexicon.kmp.library")
    id("lexicon.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.components.resources)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "lexicon.resources.generated.resources"
}
