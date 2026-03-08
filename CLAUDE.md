# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

Lexicon is a Kotlin Multiplatform (KMP) vocabulary learning app targeting Android and iOS. Jetpack Compose Multiplatform for UI, Clean Architecture + Event Sink MVVM.

## Build & Run Commands

```bash
./gradlew composeApp:assembleDebug                          # Android debug APK
./gradlew composeApp:linkDebugFrameworkIosSimulatorArm64     # iOS framework (simulator)
./gradlew composeApp:compileKotlinMetadata                   # Compile common code
./gradlew composeApp:cleanAllTests composeApp:allTests       # All common tests
./gradlew composeApp:testDebugUnitTest                       # Android unit tests only
./scripts/bump-version.sh --hotfix|--minor|--major           # Version bump
```

## Setup

Copy `local.defaults.properties` to `local.properties` and fill in backend URL, OAuth client IDs, RevenueCat keys, and signing config. For iOS: also run `./scripts/sync-ios-config.sh`.

## Module Architecture

### Current (flat — migrating to feature-based)

```
composeApp        -> App entry, DI (Koin), platform hooks
presentation      -> Compose screens, ViewModels, navigation
domain            -> Use cases, domain models, repository interfaces (pure Kotlin)
data              -> Repository impls, SQLDelight DB, Ktor data sources
core              -> HTTP client, Try<T>, BaseViewModel, UiState, OnEvents
platforms         -> expect/actual bridges (Firebase, notifications, secure storage)
design-system     -> Reusable Compose components and theming
resources         -> Compose Multiplatform strings and assets
utils             -> Shared helpers
test              -> Shared test utilities
build-logic       -> Convention plugin
iosApp            -> Swift iOS entry
```

Data flow: **View -> ViewModel -> UseCase -> Repository -> DataSource**
State flows back via Compose `mutableStateOf` snapshot state.

### Target

```
:app -> :feature:{auth,study,words,profile,import}
     -> :domain
     -> :core:{common,network,database,design-system,testing}
     -> :platforms, :resources
```

## Key Technical Details

- Dependency versions centralized in `gradle/libs.versions.toml`
- **Android SDK**: minSdk 24, compileSdk/targetSdk 36
- **DI**: Koin — `composeApp/src/commonMain/kotlin/di/AppModule.kt`
- **Database**: SQLDelight (multiplatform)
- **Networking**: Ktor with auth interceptor + error interceptor + auto token refresh/retry on 401/403
- **Auth**: KMPAuth + Firebase Auth (Google OAuth, Apple Sign-In)
- **Subscriptions**: RevenueCat KMP
- **Navigation**: `androidx.navigation:navigation-compose` with bottom tabs
- **App versioning**: `versioning.properties`
- **Architecture vision**: `doc/architecture-vision.html`

## Architecture Rules

Detailed patterns live in `.claude/skills/` — load the relevant skill for implementation details. These are the guardrails:

| Layer | Contract | Skill |
|---|---|---|
| **ViewModel** | `BaseViewModel<State, Effect>`, single data class state, event sink (public methods), `updateState`/`emitEffect`/`.reduce()` | `viewmodel-patterns` |
| **Screen** | `viewModel.state()` (Compose-native), `OnEvents`, `LexiconColumn`, content composable with data+lambdas | `screen-patterns` |
| **UseCase** | `UseCase<P,R>` -> `Try<T>`, `FlowUseCase<P,R>` -> `Flow<T>`, stateless | `usecase-patterns` |
| **Repository** | suspend -> `Try<T>`, stream -> `Flow<T>`, interface in domain, impl in data | `repository-patterns` |
| **Data Source** | Interface in domain, impl in data, mappers as extension functions | `repository-patterns` |
| **Navigation** | Type-safe `@Serializable` routes, `OverlayHost` for dialogs/sheets | `navigation-overlays` |
| **Design System** | Check existing components first, use `Theme.*` tokens, never hardcode | `design-system` |
| **Testing** | Turbine for VM, fakes over mocks, MockEngine for DataSource | `testing-patterns` |

### Module Boundaries (STRICT)

- `domain` must NEVER import from: `data`, `presentation`, `platforms`, `core`, `composeApp`
- `domain` must NEVER reference: SQLDelight, Ktor, Koin, Compose, Android, iOS APIs
- `presentation` must NEVER import from: `data` (only through `domain`)
- `design-system` must NEVER import from: `domain`, `data`, `presentation`

### Anti-patterns (NEVER use in new code)

- `!!` (non-null assertion) — use safe calls, Elvis, `requireNotNull` with justification
- `try-catch` for control flow — use Flow `.catch {}` operator or `Try<T>`
- Unnecessary `runCatching` — prefer `Try<T>`-returning APIs
- Sealed Event/Intent classes — use event sink pattern (public ViewModel methods)
- `collectAsStateWithLifecycle()` — use `viewModel.state()` (Compose-native)
- `LaunchedEffect` for effects — use `OnEvents(viewModel.effects)`
- Fragmented StateFlows — single `data class` state per screen
- Stateful use cases — no mutable fields in use cases
- Bare-throwing suspend methods — always return `Try<T>`

## Testing

Test pyramid: ViewModel (Turbine) -> Repository (fakes) -> DataSource (MockEngine) -> UseCase

- Common tests: `composeApp/src/commonTest/kotlin/` — kotlin-test + coroutines-test + Turbine
- Android tests: `composeApp/src/androidTest/kotlin/` — JUnit 4
- **Fakes over mocks** — manual fakes for all dependencies
- **Shared fakes**: reusable test utilities in `:core:testing`
- Tests required for all new code: ViewModel + UseCase at minimum

## CI/CD

GitHub Actions: `build.yml` (compile -> APK -> iOS framework) and `test.yml` (tests -> iOS build).
Secrets via `.github/actions/init-config/action.yml`.

## Conventions

- All new code follows target patterns — never perpetuate legacy patterns
- Old code migrates gradually (see `doc/architecture-vision.html`)
- Register new DI components in `AppModule.kt`
- Conventional commit messages, SOLID principles

## Workflow

- **Plan first**: Enter plan mode for tasks touching 3+ files or requiring architectural decisions
- **Commit often**: One logical unit per commit
- **Use skills**: They auto-trigger based on task context and contain the canonical patterns
- **Use agents**: `architecture-reviewer` (boundary checks), `test-writer` (generate tests), `kmp-navigator` (trace flows), `migrator` (migrate patterns), `screen-redesigner` (redesign screens), `e2e-feature` (full-stack features)
- **Break large tasks**: Delegate independent work to subagents
- **Custom commands**: `/test`, `/build`, `/review`, `/new-feature`
