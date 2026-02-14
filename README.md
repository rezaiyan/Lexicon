# Lexicon

A Kotlin Multiplatform vocabulary learning app targeting **Android**, **iOS**, and **Web (Kotlin/Wasm)**.

Built with Compose Multiplatform, Clean Architecture, and MVVM.

## Module Architecture

```
composeApp        App entry point, DI graph (Koin), platform hooks
presentation      Compose screens, ViewModels, navigation, UI state
domain            Use cases + domain models (pure Kotlin)
data              Repository implementations, Room DB, Ktor remote data sources
core              Shared configuration, platform abstractions
platforms         Platform-specific bridges (analytics, notifications, secure storage)
design-system     Reusable Compose components and theming
resources         Compose Multiplatform strings and assets
utils             Shared helper functions
test              Shared test utilities
build-logic       Custom Gradle convention plugin
iosApp            Swift iOS entry point
```

## Build & Run

```bash
# Android debug APK
./gradlew composeApp:assembleDebug

# iOS framework (simulator)
./gradlew composeApp:linkDebugFrameworkIosSimulatorArm64

# Web (Kotlin/Wasm dev server)
./gradlew composeApp:wasmJsBrowserDevelopmentRun

# Compile common (shared) Kotlin code
./gradlew composeApp:compileKotlinMetadata
```

## Testing

```bash
# Run all common unit tests
./gradlew composeApp:cleanAllTests composeApp:allTests

# Android unit tests only
./gradlew composeApp:testDebugUnitTest

# Web browser tests
./gradlew composeApp:wasmJsBrowserTest
```

## Setup

1. Copy `local.defaults.properties` to `local.properties`
2. Fill in backend URL, Google OAuth client IDs, RevenueCat keys
3. For iOS builds, run `./scripts/sync-ios-config.sh`

## Key Technical Details

- **Kotlin** 2.2.20, Compose Multiplatform 1.9.2
- **Targets:** Android (minSdk 24), iOS, Web (wasmJs)
- **DI:** Koin
- **Database:** Room 2.8.2 (multiplatform, Android + iOS only)
- **Networking:** Ktor 3.3.1 with auth interceptor and token refresh
- **Auth:** KMPAuth + Firebase Auth (Google OAuth, Apple Sign-In)
- **Subscriptions:** RevenueCat KMP
- **Navigation:** androidx.navigation with bottom tabs (Profile, Study, Settings)

## App Flow

```
Splash -> [authenticated?] -> Main App (tabs)
       -> [not authenticated] -> Auth Gate (Google/Apple sign-in)
                               -> Onboarding (language, level)
                               -> Vocabulary Preview (import suggestions)
                               -> Main App
```

## Version Management

```bash
./scripts/bump-version.sh --hotfix   # patch bump
./scripts/bump-version.sh --minor    # minor bump
./scripts/bump-version.sh --major    # major bump
```

