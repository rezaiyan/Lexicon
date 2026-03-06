# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Lexicon is a Kotlin Multiplatform (KMP) vocabulary learning app targeting Android and iOS with a shared codebase. It uses Jetpack Compose Multiplatform for UI and follows Clean Architecture + Event Sink MVVM.

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

### Current (flat modules — migrating)

```
composeApp        -> App entry point, DI graph (Koin), platform hooks, Gradle config
presentation      -> Compose screens, ViewModels, navigation, UI state
domain            -> Use cases + domain models (pure Kotlin, no framework deps)
data              -> Repository implementations, Room DB, Ktor remote data sources
core              -> Ktor HTTP client, Try<T>, BaseViewModel, UiState, OnEvents
platforms         -> Platform-specific bridges (Firebase, notifications, secure storage)
design-system     -> Reusable Compose components and theming
resources         -> Compose Multiplatform strings and assets
utils             -> Shared helper functions
test              -> Shared test utilities
build-logic       -> Custom Gradle convention plugin (lexicon.compose-app)
iosApp            -> Swift iOS entry point
```

### Target (feature-based vertical slices)

```
:app -> :feature:auth, :feature:study, :feature:words, :feature:profile, :feature:import
         -> :domain
         -> :core:common, :core:network, :core:database, :core:design-system, :core:testing
         -> :platforms, :resources
```

Data flows unidirectionally: **View -> ViewModel -> UseCase -> Repository -> Local/Remote data source**, with state exposed back via Compose `mutableStateOf` snapshot state.

## Key Technical Details

- **Kotlin 2.2.21**, Compose Multiplatform 1.9.2, AGP 8.12.3, JVM target 11
- **Android SDK**: minSdk 24, compileSdk/targetSdk 36
- **DI**: Koin — main module at `composeApp/src/commonMain/kotlin/di/AppModule.kt`
- **Database**: Room 2.8.2 (multiplatform) with KSP code generation, schema v5 with migrations
- **Networking**: Ktor 3.3.1 with auth interceptor, error interceptor, and automatic token refresh/retry on 401/403
- **Auth**: KMPAuth + Firebase Auth (Google OAuth, Apple Sign-In)
- **Subscriptions**: RevenueCat KMP
- **Navigation**: `androidx.navigation:navigation-compose` with bottom tab layout
- **Dependency versions**: centralized in `gradle/libs.versions.toml`
- **App versioning**: single source of truth in `versioning.properties`
- **Architecture vision**: `doc/architecture-vision.html` — full current-vs-proposed analysis

## Architecture Patterns

### ViewModel — BaseViewModel<State, Effect> + Event Sink

All new ViewModels must extend `BaseViewModel<S, F>`:
- **Single atomic state**: one `data class` per screen backed by `mutableStateOf`
- **Event sink**: public methods are the API — no sealed Event/Intent classes
- **State mutation**: `updateState { copy(...) }` only
- **Effects**: `emitEffect()` for one-shot side effects (navigation, snackbar)
- **Try integration**: `.reduce(onSuccess, onFailure)` folds results into state
- **Flow errors**: `.catch {}` operator — never try-catch

Screen reads state via `viewModel.state()` (Compose-native) — not `collectAsStateWithLifecycle()`.
Effects handled via `OnEvents(viewModel.effects)`.
VM methods passed as references to content composables: `viewModel::doAction`.

### Use Cases — UseCase<P, R> / FlowUseCase<P, R>

All new use cases implement one of two `fun interface` contracts:
- `UseCase<P, R>`: `suspend operator fun invoke(params: P): Try<R>`
- `FlowUseCase<P, R>`: `operator fun invoke(params: P): Flow<R>`
- Suspend use cases always return `Try<T>` — never bare types
- Flow use cases return `Flow<T>` — never `Flow<Try<T>>`
- Must be stateless — no mutable fields

### Repositories — Try<T> / Flow<T>

Two rules for repository contracts:
- **Suspend -> `Try<T>`**: all suspend methods return `Try<T>`, never throw
- **Streaming -> `Flow<T>`**: all reactive methods return `Flow<T>`
- Interfaces in `domain/`, implementations in `data/`
- Data source interfaces in `domain/`, implementations in `data/`
- Mappers are extension functions: `Dto.toDomain()`, `Entity.toDomain()`

### Other Patterns (unchanged)
- **Platform abstractions**: `expect`/`actual` declarations in `platforms` module
- **Spaced Repetition**: 7-bucket system in `ReviewWordUseCase`
- **Auth-required flow**: authentication required before accessing the app
- **HTTP pipeline**: AuthInterceptor -> RefreshAndRetry -> ErrorInterceptor (keep as-is)

## Testing

### Test Pyramid (target)

```
  ViewModel Tests (Turbine)        <- state transitions, effects, event sink
  Repository Tests (fake DS)       <- mapping, local+remote coordination
  DataSource Tests (MockEngine)    <- HTTP serialization, error mapping
  Domain / Use Case Tests          <- business logic in isolation
  Instrumented / Integration       <- Room DB, SRS regression
```

- **Common tests**: `composeApp/src/commonTest/kotlin` — kotlin-test + coroutines-test + Turbine
- **Android unit tests**: `composeApp/src/androidTest/kotlin` — JUnit 4
- **Android instrumented tests**: `composeApp/src/androidInstrumentedTest/kotlin` — Room DB, SRS regression
- **iOS tests**: currently disabled; CI verifies framework linking
- **Fakes over mocks**: use manual fakes for all test dependencies
- **Shared fakes**: reusable test utilities in `:core:testing`

## CI/CD

GitHub Actions with two workflows:
- **build.yml**: common compilation -> Android APK -> iOS framework (macOS runner)
- **test.yml**: common tests -> Android unit tests -> iOS framework build

All environment secrets are injected via `.github/actions/init-config/action.yml` which generates `local.properties` and `google-services.json` from CI secrets.

## Conventions

- Follow Clean Architecture boundaries: domain module stays pure Kotlin with no platform dependencies
- Use Koin for all dependency injection; register new components in `AppModule.kt`
- All new code follows target patterns — BaseViewModel, UseCase<P,R>, Try<T> contracts
- Old code migrates gradually (see `doc/architecture-vision.html` for roadmap)
- PRs should follow conventional commit style and maintain SOLID principles
- Add tests for all new code (ViewModel + UseCase at minimum)

## Workflow Best Practices

- **Plan first**: Use plan mode for any task touching 3+ files or requiring architectural decisions
- **Commit often**: Commit as soon as a logical unit of work is complete
- **Compact proactively**: Run `/compact` at ~50% context usage to stay in the effective zone
- **Custom commands**: Use `/test`, `/build`, `/review`, `/new-feature` for common workflows
- **Custom agents**: `architecture-reviewer` (boundary + contract checks), `test-writer` (generate tests), `kmp-navigator` (trace code flows), `migrator` (migrate old patterns), `screen-redesigner` (redesign screens), `e2e-feature` (full-stack features)
- **Break large tasks**: Keep subtasks under 50% context window — delegate to subagents for independent work
- **Migration**: Use `migrator` agent for systematic migration of old patterns to new ones

## Kotlin code style

- **No `!!` (double-bang)**: Do not use the non-null assertion operator. Handle nullability explicitly with safe calls (`?.`), Elvis (`?:`), `let`/`also`/`takeIf`, or proper types so values are non-null where needed.
- **Handle nullability properly**: Prefer nullable types and explicit handling over force-unwrapping. Use `requireNotNull`/`checkNotNull` only when the contract guarantees non-null at that point, and document why.
- **No try-catch for control flow**: Do not use `try`/`catch` for normal error handling. For asynchronous code, use the **Flow `catch` operator** (e.g. `flow { ... }.catch { e -> emit(...) }`) or `Try<T>`/sealed outcomes where appropriate.
- **Avoid unnecessary `runCatching`**: Do not use `runCatching` where it is not necessary. Prefer direct returns, `Try<T>`-returning APIs, or Flow-based error handling instead of wrapping every call in `runCatching`.
