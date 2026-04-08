---
name: lexicon-project
description: Lexicon KMP operational snapshot — tech stack versions, build commands, key paths. Architectural contracts live in /CLAUDE.md.
---

# Project: Lexicon

**Last Updated:** 2026-04-08
**Canonical architecture rules:** `/CLAUDE.md` (module layout, layer contracts, anti-patterns)
**Patterns:** `.claude/skills/` (viewmodel-patterns, screen-patterns, usecase-patterns, repository-patterns, error-handling, testing-patterns, di-patterns, state-machines, navigation-overlays, design-system, domain-model-patterns, analytics-feature)

## Overview

Kotlin Multiplatform vocabulary learning app targeting Android + iOS. Compose Multiplatform UI, Clean Architecture + Event Sink MVVM.

## Technology Stack

Versions pinned in `gradle/libs.versions.toml` — always check there before bumping.

| Category | Tool | Version |
|----------|------|---------|
| Language | Kotlin | 2.3.10 |
| Build | AGP / Gradle KTS | 8.12.3 |
| UI | Compose Multiplatform | 1.10.2 |
| DI | Koin | 4.1.1 |
| Networking | Ktor | 3.4.1 |
| Database | SQLDelight | 2.3.1 |
| Navigation | androidx.navigation-compose | 2.9.2 |
| Auth | KMPAuth + Firebase Auth | 2.4.0-alpha05 / 24.0.1 |
| Subscriptions | RevenueCat KMP | 2.2.10+17.19.1 |
| Serialization | kotlinx.serialization | 1.10.0 |
| Testing | kotlin-test + Turbine | 1.2.1 |
| Lint | detekt | 1.23.8 |
| Coverage | kover | 0.9.7 |

**Android:** minSdk 24, compileSdk/targetSdk 36.

## Directory Structure (current flat layout)

```
composeApp/      App entry, DI wiring
presentation/    Compose screens, ViewModels, navigation
domain/          Use cases, domain models, repo interfaces (pure Kotlin)
data/            Repo impls, SQLDelight, Ktor data sources
core/            HTTP client, Try<T>, BaseViewModel, UiState, OnEvents
design-system/   Reusable Compose components + theme tokens
platforms/       expect/actual bridges (Firebase, notifications, secure storage)
resources/       Compose Multiplatform strings and assets
utils/           Shared helpers
feature/         (in-progress) target feature modules
test/            Shared test utilities
build-logic/     Convention plugins
iosApp/          Swift iOS entry
docs/            Architecture vision, plans
maestro/         E2E flows
fastlane/        Android/iOS release automation
```

**Migration target** (see `/CLAUDE.md` → Target): `:app` → `:feature:{auth,study,words,profile,import}` → `:domain` → `:core:{common,network,database,design-system,testing}` → `:platforms`, `:resources`.

## Development Commands

| Task | Command |
|------|---------|
| Android debug APK | `./gradlew composeApp:assembleDebug` |
| iOS simulator framework | `./gradlew composeApp:linkDebugFrameworkIosSimulatorArm64` |
| Compile common code | `./gradlew composeApp:compileKotlinMetadata` |
| All common tests | `./gradlew composeApp:cleanAllTests composeApp:allTests` |
| Android unit tests | `./gradlew composeApp:testDebugUnitTest` |
| Lint (detekt) | `./gradlew detekt` |
| Coverage report | `./gradlew koverHtmlReport` |
| Version bump | `./scripts/bump-version.sh --hotfix\|--minor\|--major` |
| Sync iOS config | `./scripts/sync-ios-config.sh` |

## Setup

Copy `local.defaults.properties` → `local.properties` and fill in backend URL, OAuth client IDs, RevenueCat keys, signing config. For iOS, also run `./scripts/sync-ios-config.sh`.

## Key Files

- **Config:** `gradle/libs.versions.toml`, `versioning.properties`, `local.properties`, `detekt.yml`
- **DI entry:** `composeApp/src/commonMain/kotlin/di/AppModule.kt`
- **Architecture vision:** `doc/architecture-vision.html`
- **Plans:** `docs/plans/` — spec workflow plan files
- **App context (READ FIRST):** `.claude/app-context.md` — full feature map, domain rules, "what to know before touching any feature"
- **Critical risks:** `.claude/critical-risks.md`
- **Infra (local-only, gitignored):** `.claude/infra.local.md`

## CI/CD

GitHub Actions: `build.yml` (compile → APK → iOS framework), `test.yml` (tests → iOS build). Secrets via `.github/actions/init-config/action.yml`.
