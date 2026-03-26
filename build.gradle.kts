plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.firebaseCrashlytics) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

kover {
    merge {
        allProjects()
    }
    reports {
        filters {
            excludes {
                classes(
                    "*_Factory",
                    "*_MembersInjector",
                    "*BuildConfig*",
                    "*ComposableSingletons*",
                )
                packages(
                    "di",
                    "theme",
                    "notification",
                    "com.alirezaiyan.vokab",
                )
                annotatedBy(
                    "androidx.compose.runtime.Composable",
                )
            }
        }
        verify {
            rule("Baseline coverage") {
                minBound(5)
            }
        }
    }
}

// Force Kotlin stdlib to match compiler version — prevents Kotlin/Wasm stdlib mismatch
// when transitive dependencies (e.g. SQLDelight) pull in a newer stdlib.
val kotlinVersion = libs.versions.kotlin.get()
subprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-stdlib")) {
                useVersion(kotlinVersion)
            }
        }
    }
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

// KAN-18: Enable SARIF output for GitHub Code Scanning annotations
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        sarif.required.set(true)
    }
}

/**
 * Enforces module dependency boundary rules from CLAUDE.md.
 * Run with: ./gradlew checkModuleBoundaries
 * Hooked into the `check` task so violations fail CI automatically.
 *
 * Scans build.gradle.kts files as text to find project(":...") references
 * in non-test source sets. This approach is Gradle-version agnostic.
 */
tasks.register("checkModuleBoundaries") {
    group = "verification"
    description = "Fail the build if module dependency boundary rules are violated."
    // findProject() accesses the Project graph at execution time — not compatible with
    // Gradle's configuration cache, which forbids Project references in task state.
    notCompatibleWithConfigurationCache("Uses findProject() at execution time")
    doLast {
        // Rules: module path -> forbidden dependency paths
        // :domain depends on :core for Try<T> — that is intentional and allowed.
        val rules = mapOf(
            ":domain" to listOf(":data", ":presentation", ":platforms", ":composeApp"),
            ":presentation" to listOf(":data"),
            ":design-system" to listOf(":domain", ":data", ":presentation"),
        )

        // Matches project(":foo") and project(path = ":foo")
        val projectRef = Regex("""project\(\s*(?:path\s*=\s*)?"(:[^"]+)"\s*\)""")

        val violations = mutableListOf<String>()

        rules.forEach { (modulePath, forbidden) ->
            val proj = findProject(modulePath) ?: return@forEach
            val buildFile = proj.buildFile
            if (!buildFile.exists()) return@forEach

            // Walk lines, skip content inside *Test* or *test* named blocks.
            var depth = 0
            var testBlockDepth = -1   // brace depth at which we entered a test block; -1 = not in one

            for (line in buildFile.readLines()) {
                val openCount  = line.count { it == '{' }
                val closeCount = line.count { it == '}' }

                // Detect entry into a test source-set block (e.g. commonTest.dependencies {)
                if (testBlockDepth < 0 && openCount > 0 &&
                    line.contains(Regex("""\b\w*[Tt]est\w*\s*[\.\{]"""))) {
                    testBlockDepth = depth  // remember depth before the open brace
                }

                depth += openCount - closeCount

                // Exit test block when we return to (or below) the depth we entered it
                if (testBlockDepth >= 0 && depth <= testBlockDepth) {
                    testBlockDepth = -1
                    continue
                }

                if (testBlockDepth >= 0) continue  // skip lines inside test blocks

                projectRef.findAll(line).forEach { match ->
                    val dep = match.groupValues[1]
                    if (forbidden.any { dep == it || dep.startsWith("$it:") }) {
                        violations += "  [$modulePath] → [$dep] is forbidden"
                    }
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw org.gradle.api.GradleException(buildString {
                appendLine("Module boundary violations detected:")
                violations.forEach { appendLine(it) }
                append("See CLAUDE.md \u00a7 Module Boundaries for rules.")
            })
        }
        logger.lifecycle("checkModuleBoundaries: no violations found")
    }
}

tasks.register<Exec>("bumpVersion") {
    group = "lexicon"
    description = "Bump/sync version (versioning.properties + iOS Config.xcconfig). Use script with --hotfix|--minor|--major for bump."
    commandLine("bash", "scripts/bump-version.sh")
    workingDir = rootDir
}
