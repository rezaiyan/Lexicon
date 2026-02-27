# Architecture Overview

## Tech Stack
- **Kotlin 2.2.21**, Compose Multiplatform 1.9.2, AGP 8.12.3
- **Targets**: Android (minSdk 24, targetSdk 36), iOS, WasmJs (web)
- **DI**: Koin 4.1.1
- **Database**: SQLDelight 2.1.0 (multiplatform)
- **Networking**: Ktor 3.3.1
- **Auth**: KMPAuth 2.4.0-alpha05 + Firebase Auth (GitLive 2.4.0)
- **Subscriptions**: RevenueCat KMP 2.2.10+17.19.1
- **Serialization**: kotlinx-serialization-json 1.9.0

## Module Dependency Graph

```
iosApp (Swift)
    └── composeApp
         ├── presentation
         │    ├── domain
         │    ├── design-system
         │    ├── resources
         │    └── utils
         ├── data
         │    ├── domain
         │    ├── core
         │    └── platforms
         ├── core
         ├── platforms
         │    └── domain (for interfaces)
         └── domain (pure Kotlin)
```

## Module Responsibilities

| Module | Purpose | Dependencies |
|--------|---------|-------------|
| `composeApp` | App entry, DI graph (Koin), platform hooks | All modules |
| `presentation` | Screens, ViewModels, navigation, UI state | domain, design-system, resources, utils |
| `domain` | Use cases, models, repository interfaces | None (pure Kotlin) |
| `data` | Repository impls, SQLDelight DB, Ktor APIs | domain, core, platforms |
| `core` | App config, expect/actual utilities | None |
| `platforms` | Platform bridges (Firebase, notifications, storage) | domain (for interfaces) |
| `design-system` | Theme, colors, typography, spacing | None |
| `resources` | Compose Multiplatform strings & assets | None |
| `utils` | Helpers (Language enum, image utils, file picker) | None |
| `build-logic` | Gradle convention plugin | None |

## Data Flow (Unidirectional)

```
View (Composable)
  → ViewModel (processes events/intents)
    → UseCase (business logic)
      → Repository Interface (domain)
        → Repository Impl (data)
          → Local DataSource (SQLDelight) / Remote DataSource (Ktor)
            ← Data flows back via StateFlow/Flow
```

## Key Architectural Patterns

1. **Clean Architecture**: Strict module boundaries. Domain has zero framework dependencies.
2. **MVVM + Intent**: ViewModels expose `StateFlow<UiState>` and process sealed event classes.
3. **Use Case Pattern**: Each business operation is a standalone class (40+ use cases).
4. **Repository Pattern**: Interfaces in `domain`, implementations in `data`.
5. **Try<T> Monad**: Custom Result type in `domain/common/Try.kt` for error handling.
6. **Expect/Actual**: Platform abstractions for Firebase, notifications, storage, TTS.
7. **Overlay System**: Dialogs and bottom sheets managed via `OverlayHost` CompositionLocal.
8. **Flow-Based Reactivity**: All state uses Kotlin Flow (never LiveData).

## App Entry Points

- **Android**: `LexiconApplication` (Application) -> `MainActivity` -> `LexiconApp()` composable
- **iOS**: `MainViewController.kt` -> Swift `ContentView` -> Compose `LexiconApp()`
- **Web**: `composeApp/src/webMain/kotlin/com/alirezaiyan/vokab/main.kt`

## App Navigation Flow

```
Splash → [Onboarding → VocabularyPreview →] AuthGate → Ready (Bottom Nav)
                                                          ├── Profile
                                                          ├── Study (default)
                                                          │    ├── Review (bottom sheet)
                                                          │    └── Import (bottom sheet)
                                                          └── Settings
                                                               ├── WordManager
                                                               └── Subscription
```

## Versioning
- Source of truth: `versioning.properties` (versionCode=28, versionName=1.12.0)
- Bump script: `./scripts/bump-version.sh --hotfix|--minor|--major`
