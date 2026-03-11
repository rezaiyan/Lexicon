# Lexicon

A Kotlin Multiplatform vocabulary learning app targeting **Android**, **iOS**, and **Web (Kotlin/Wasm)**.

Built with Compose Multiplatform, Clean Architecture, and MVVM.

## Features

<!--
  To generate these demo recordings, run:
    ./maestro/record_showcase.sh

  GIFs will be saved to docs/demos/ and can be committed to the repo.
  See maestro/flows/showcase/ for the individual flow definitions.
-->

### Study Dashboard
Track your learning progress with an interactive dashboard — vocabulary stats, progress ring, distribution bar, and learning stage breakdown.

<p align="center">
  <img src="docs/demos/study_dashboard_showcase.gif" alt="Study Dashboard" width="300"/>
</p>

### Flashcard Review
Master vocabulary through spaced-repetition flashcards — tap to flip, rate your recall (Got it! / Nope), and track performance with completion stats.

<p align="center">
  <img src="docs/demos/flashcard_review_showcase.gif" alt="Flashcard Review" width="300"/>
</p>

### Import Words
Build your vocabulary multiple ways — type words manually with translations and descriptions, or import from a `.txt` file.

<p align="center">
  <img src="docs/demos/import_words_showcase.gif" alt="Import Words" width="300"/>
</p>

### AI-Powered Import
Let AI generate vocabulary for you — choose your target language, proficiency level, and topics, then preview and import curated word packs.

<p align="center">
  <img src="docs/demos/ai_import_showcase.gif" alt="AI Import Wizard" width="300"/>
</p>

### Word Manager
Browse, search, and manage your entire vocabulary — view detailed word cards with learning progress, edit words, and track mastery levels.

<p align="center">
  <img src="docs/demos/word_manager_showcase.gif" alt="Word Manager" width="300"/>
</p>

### Profile & Settings
View your learning streak and weekly activity, compete on the leaderboard, switch between light/dark themes, and manage your subscription.

<p align="center">
  <img src="docs/demos/profile_settings_showcase.gif" alt="Profile & Settings" width="300"/>
</p>

### Onboarding
Beautiful first-launch experience — personalized language selection, proficiency level, and AI-generated starter vocabulary.

<p align="center">
  <img src="docs/demos/onboarding_showcase.gif" alt="Onboarding" width="300"/>
</p>

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

