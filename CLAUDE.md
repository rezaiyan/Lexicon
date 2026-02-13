# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Vokab is a Kotlin Multiplatform (KMP) vocabulary learning app targeting Android and iOS with a shared codebase. It uses Jetpack Compose Multiplatform for UI and follows Clean Architecture + MVVM.

## Build & Run Commands

```bash
# Build Android debug APK
./gradlew composeApp:assembleDebug

# Build iOS framework (simulator)
./gradlew composeApp:linkDebugFrameworkIosSimulatorArm64

# Compile common (shared) Kotlin code
./gradlew composeApp:compileKotlinMetadata

# Run all common unit tests
./gradlew composeApp:cleanAllTests composeApp:allTests

# Run Android unit tests only
./gradlew composeApp:testDebugUnitTest

# Version bumping (updates versioning.properties + iOS Config.xcconfig)
./scripts/bump-version.sh --hotfix   # patch bump
./scripts/bump-version.sh --minor    # minor bump
./scripts/bump-version.sh --major    # major bump
```

## Setup

Copy `local.defaults.properties` to `local.properties` and fill in backend URL, Google OAuth client IDs, RevenueCat keys, and optional signing config. For iOS builds, also run `./scripts/sync-ios-config.sh`.

## Module Architecture

```
composeApp        → App entry point, DI graph (Koin), platform hooks, Gradle config
presentation      → Compose screens, ViewModels, navigation, UI state
domain            → Use cases + domain models (pure Kotlin, no framework deps)
data              → Repository implementations, Room DB, Ktor remote data sources
core              → Ktor HTTP client setup, shared configuration
platforms         → Platform-specific bridges (Firebase, notifications, secure storage)
design-system     → Reusable Compose components and theming
resources         → Compose Multiplatform strings and assets
utils             → Shared helper functions
test              → Shared test utilities
build-logic       → Custom Gradle convention plugin (vokab.compose-app)
iosApp            → Swift iOS entry point
```

Data flows unidirectionally: **View → ViewModel → UseCase → Repository → Local/Remote data source**, with state exposed back via `StateFlow`/`SharedFlow`.

## Key Technical Details

- **Kotlin 2.2.20**, Compose Multiplatform 1.9.2, AGP 8.12.3, JVM target 11
- **Android SDK**: minSdk 24, compileSdk/targetSdk 36
- **DI**: Koin — main module at `composeApp/src/commonMain/kotlin/di/AppModule.kt` (~500 lines registering all data sources, repositories, use cases, and ViewModels)
- **Database**: Room 2.8.2 (multiplatform) with KSP code generation, schema v5 with migrations
- **Networking**: Ktor 3.3.1 with auth interceptor, error interceptor, and automatic token refresh/retry on 401/403
- **Auth**: KMPAuth + Firebase Auth (Google OAuth, Apple Sign-In)
- **Subscriptions**: RevenueCat KMP
- **Navigation**: `androidx.navigation:navigation-compose` with bottom tab layout (Study, Collections, WordManager, Settings, Profile)
- **Dependency versions**: centralized in `gradle/libs.versions.toml`
- **App versioning**: single source of truth in `versioning.properties`

## Architecture Patterns

- **Use Case pattern**: each business operation is a standalone use case class in the `domain` module (40+ use cases)
- **Repository pattern**: interfaces in `domain`, implementations in `data`
- **Platform abstractions**: `expect`/`actual` declarations in `platforms` module for Firebase analytics, notifications, secure storage
- **Intent-based ViewModel pattern**: some ViewModels (e.g., AuthViewModel) process UI events as intents
- **Spaced Repetition**: 7-bucket system implemented in `ReviewWordUseCase` with configurable forgot penalty and success threshold
- **Auth-required flow**: authentication is required before accessing the app; no anonymous mode

## Testing

- **Common tests**: `composeApp/src/commonTest/kotlin` — kotlin-test + coroutines-test
- **Android unit tests**: `composeApp/src/androidTest/kotlin` — JUnit 4
- **Android instrumented tests**: `composeApp/src/androidInstrumentedTest/kotlin` — Room DB tests, review scenario tests, spaced repetition regression tests
- **iOS tests**: currently disabled (GoogleSignIn framework dependency); CI verifies framework linking instead

## CI/CD

GitHub Actions with two workflows:
- **build.yml**: common compilation → Android APK → iOS framework (macOS runner)
- **test.yml**: common tests → Android unit tests → iOS framework build

All environment secrets are injected via `.github/actions/init-config/action.yml` which generates `local.properties` and `google-services.json` from CI secrets.

## Conventions

- Follow Clean Architecture boundaries: domain module stays pure Kotlin with no platform dependencies
- Use Koin for all dependency injection; register new components in `AppModule.kt`
- Use Kotlin Flow (not LiveData) for reactive state
- PRs should follow conventional commit style and maintain SOLID principles
- Add tests for new use cases

## Kotlin code style

- **No `!!` (double-bang)**: Do not use the non-null assertion operator. Handle nullability explicitly with safe calls (`?.`), Elvis (`?:`), `let`/`also`/`takeIf`, or proper types so values are non-null where needed.
- **Handle nullability properly**: Prefer nullable types and explicit handling over force-unwrapping. Use `requireNotNull`/`checkNotNull` only when the contract guarantees non-null at that point, and document why.
- **No try-catch for control flow**: Do not use `try`/`catch` for normal error handling. For asynchronous code, use the **Flow `catch` operator** (e.g. `flow { ... }.catch { e -> emit(...) }`) or `Result`/sealed outcomes where appropriate.
- **Avoid unnecessary `runCatching`**: Do not use `runCatching` where it is not necessary. Prefer direct returns, `Result`-returning APIs, or Flow-based error handling instead of wrapping every call in `runCatching`.
