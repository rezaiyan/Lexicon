# Lexicon — Architecture Reimagined

> Authored as a senior software engineer review. This is an opinionated, forward-looking redesign
> that builds on what already works and surgically removes what doesn't.
> Current state: **7/10**. Target state: **9.5/10**.

---

## Table of Contents

1. [Executive Assessment](#1-executive-assessment)
2. [Module Graph — Target](#2-module-graph--target)
3. [Layer-by-Layer Redesign](#3-layer-by-layer-redesign)
   - 3.1 [Core — The Foundation](#31-core--the-foundation)
   - 3.2 [Domain — The Contract](#32-domain--the-contract)
   - 3.3 [Data — The Plumbing](#33-data--the-plumbing)
   - 3.4 [Presentation — The Surface](#34-presentation--the-surface)
   - 3.5 [Features — The Vertical Slices](#35-features--the-vertical-slices)
   - 3.6 [Platforms — The Adapters](#36-platforms--the-adapters)
4. [Cross-Cutting Concerns](#4-cross-cutting-concerns)
   - 4.1 [Error Taxonomy](#41-error-taxonomy)
   - 4.2 [Analytics as a First-Class Layer](#42-analytics-as-a-first-class-layer)
   - 4.3 [Offline-First Sync Engine](#43-offline-first-sync-engine)
   - 4.4 [Caching Strategy](#44-caching-strategy)
   - 4.5 [Feature Flags & Entitlements](#45-feature-flags--entitlements)
5. [Testing Strategy](#5-testing-strategy)
6. [Build System & CI/CD](#6-build-system--cicd)
7. [Observability](#7-observability)
8. [Migration Playbook](#8-migration-playbook)
9. [What to Keep, What to Kill](#9-what-to-keep-what-to-kill)

---

## 1. Executive Assessment

### What's Working

The current codebase has a solid foundation. `BaseViewModel<S, F>`, `Try<T>`, `UseCase<P, R>`,
and the `OnEvents` composable are genuinely well-designed abstractions — consistent, testable,
and idiomatic. The fakes-over-mocks testing philosophy is correct. The domain module's zero-
dependency constraint is enforced and valuable. SQLDelight and Ktor are the right choices for a
KMP stack.

### What's Broken

| Problem | Impact | Root Cause |
|---|---|---|
| Dual architecture (`:presentation` + `:feature:*`) | Cognitive overhead, inconsistent navigation | Migration left half-done |
| Analytics calls wired directly into ViewModels | Tight coupling, hard to test, hard to audit | No analytics abstraction |
| All DI in flat `AppModule.kt` | Feature teams can't own their DI graph | No module-scoped DI isolation |
| Manual fakes duplicated across 20+ test files | Maintenance burden, drift risk | No shared fake infrastructure |
| No explicit error taxonomy | Generic `Throwable` propagated too far up | Missing domain error model |
| `FeatureAccess` re-fetched per request | Wasted network calls | No in-memory caching layer |
| Large ViewModels (AuthViewModel: 223 lines) | Low cohesion, hard to test in isolation | No ViewModel decomposition pattern |
| Sync logic scattered in repositories | Non-deterministic, hard to retry | No dedicated sync engine |
| No schema migration strategy visible | Silent data corruption on upgrades | SQLDelight schema lifecycle not owned |

### The North Star

> Every feature is a self-contained vertical slice. Features can be added, removed, or replaced
> without touching any other feature. The app is a thin host that wires them together.
> Every architectural decision optimises for: **testability → readability → performance**.

---

## 2. Module Graph — Target

```
                         ┌─────────────────────┐
                         │      :app            │  (was :composeApp)
                         │  DI composition root │
                         │  App entry point     │
                         └──────────┬──────────┘
                                    │ depends on
          ┌─────────────────────────┼──────────────────────────┐
          │                         │                          │
  ┌───────▼──────┐         ┌────────▼───────┐       ┌─────────▼────────┐
  │ :feature:auth│         │ :feature:study │  ...  │ :feature:insights │
  │              │         │               │       │                  │
  │ owns: screens│         │ owns: screens │       │ owns: screens    │
  │         VMs  │         │         VMs   │       │         VMs      │
  │         DI   │         │         DI    │       │         DI       │
  └───────┬──────┘         └────────┬──────┘       └─────────┬────────┘
          │                         │                         │
          └─────────────────────────┼─────────────────────────┘
                                    │ all features depend on
          ┌─────────────────────────┼──────────────────────────┐
          │                         │                          │
  ┌───────▼──────┐         ┌────────▼───────┐       ┌─────────▼────────┐
  │   :domain    │         │     :data      │       │   :analytics     │
  │              │         │               │       │                  │
  │ pure Kotlin  │         │ repos, DSes   │       │ event bus        │
  │ zero deps    │         │ Ktor, SQLDel  │       │ trackers         │
  │ UC contracts │         │ mappers       │       │ no domain dep    │
  └───────┬──────┘         └────────┬──────┘       └─────────┬────────┘
          │                         │                         │
          └─────────────────────────┼─────────────────────────┘
                                    │ all depend on
          ┌─────────────────────────┼──────────────────────────┐
          │                         │                          │
  ┌───────▼──────┐         ┌────────▼───────┐       ┌─────────▼────────┐
  │ :core:common │         │ :core:network  │       │ :core:database   │
  │              │         │               │       │                  │
  │ Try<T>       │         │ ApiClient     │       │ AppDatabase      │
  │ BaseViewModel│         │ interceptors  │       │ schema lifecycle  │
  │ UseCase      │         │ retry logic   │       │ migrations       │
  │ OnEvents     │         │               │       │                  │
  └──────────────┘         └───────────────┘       └──────────────────┘
          │                         │                          │
          └─────────────────────────┼──────────────────────────┘
                                    │ platform bridges
                    ┌───────────────▼───────────────┐
                    │          :platforms            │
                    │  expect/actual, no logic       │
                    │  Firebase, TTS, push, keychain │
                    └───────────────────────────────┘
          ┌─────────────────────────┐
          │    :core:design-system  │
          │  no domain, no data dep │
          │  Theme, LexiconColumn   │
          │  atomic components      │
          └─────────────────────────┘
          ┌─────────────────────────┐
          │    :core:testing        │
          │  shared fakes, builders │
          │  ViewModelTestBase      │
          │  FakeRegistry           │
          └─────────────────────────┘
```

### Strict Dependency Rules (enforced via Gradle module-graph plugin)

```
:domain          → (nothing)
:core:*          → (nothing except :core:common)
:data            → :domain, :core:common, :core:network, :core:database, :platforms
:analytics       → :core:common, :platforms  (NO :domain dependency — tracks strings/ids only)
:feature:*       → :domain, :data, :analytics, :core:*, :platforms, :core:design-system
:app             → :feature:*, :core:*, :data, :domain, :analytics
:core:testing    → :core:common, :domain (for fakes), :data (for fakes)
:core:design-system → (nothing from domain, data, features)
```

---

## 3. Layer-by-Layer Redesign

### 3.1 Core — The Foundation

#### 3.1.1 `Try<T>` — Keep, Extend Slightly

The current `Try<T>` is excellent. One addition: typed failures.

```kotlin
// CURRENT — valid but lossy
sealed class Try<out T> {
    data class Success<out T>(val value: T) : Try<T>()
    data class Failure(val throwable: Throwable) : Try<Nothing>()
}

// ADDITION — domain-level error classification
// Keep Try<T> as-is. Add a typed unwrap extension:
inline fun <T> Try<T>.onDomainError(
    block: (DomainError) -> Unit
): Try<T> = onFailure { if (it is DomainError) block(it) }
```

No change to Try<T> itself — just add `DomainError` (see §4.1).

#### 3.1.2 `BaseViewModel<S, F>` — Keep, Add Decomposition Seam

The current design is good. Add one seam for **handler delegation** — breaking large VMs
into cohesive sub-handlers without losing the single-state guarantee.

```kotlin
// PROPOSED ADDITION
// A handler owns a slice of state and a set of related methods.
// The VM composes multiple handlers.

abstract class StateHandler<S>(
    private val stateAccess: StateAccess<S>
) {
    protected fun updateState(reducer: S.() -> S) = stateAccess.updateState(reducer)
}

// Example: decompose AuthViewModel into handlers
class AuthLoginHandler(stateAccess: StateAccess<AuthState>, ...) : StateHandler<AuthState>(stateAccess) {
    suspend fun loginWithGoogle(idToken: String) { ... }
    suspend fun loginWithApple(token: String, nonce: String) { ... }
}

class AuthSessionHandler(stateAccess: StateAccess<AuthState>, ...) : StateHandler<AuthState>(stateAccess) {
    suspend fun verifySession() { ... }
    suspend fun logout() { ... }
}

class AuthViewModel(
    loginHandler: AuthLoginHandler,
    sessionHandler: AuthSessionHandler,
) : BaseViewModel<AuthState, AuthEffect>() {
    fun loginWithGoogle(idToken: String) = viewModelScope.launch { loginHandler.loginWithGoogle(idToken) }
    fun verifySession() = viewModelScope.launch { sessionHandler.verifySession() }
    // ... delegates only — no business logic in VM
}
```

**Why this wins**: VMs stay thin (< 80 lines). Handlers are independently testable.
State is still a single source of truth. No StateFlow fragmentation.

#### 3.1.3 `UseCase` — Keep Contracts, Enforce Statelessness via Lint

```kotlin
// No change to contracts
fun interface UseCase<in P, out R> {
    suspend operator fun invoke(params: P): Try<R>
}
fun interface FlowUseCase<in P, out R> {
    operator fun invoke(params: P): Flow<R>
}
```

Add a custom Detekt rule: `StatelessUseCaseRule` — fails if a `UseCase` subclass has
mutable `var` properties. Catches accidental state at compile-time.

#### 3.1.4 `OnEvents` — Keep, Add Structured Concurrency Option

```kotlin
// ADDITION: for effects that need sequential processing
@Composable
fun <T : Any> OnEventsOrdered(
    events: Flow<T>,
    handleEvent: suspend (T) -> Unit
) {
    // Uses a Mutex internally to prevent interleaving
}
```

#### 3.1.5 New: `UiState<T>` — Unified Loading/Success/Error Wrapper

The app currently mixes ad-hoc loading flags into state data classes. Standardise:

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val error: DomainError) : UiState<Nothing>
}

// Extension for common patterns
inline fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> = when (this) {
    is Loading -> Loading
    is Success -> Success(transform(data))
    is Error -> Error(error)
}

// Compose extension
@Composable
fun <T> UiState<T>.Content(
    loading: @Composable () -> Unit = { LexiconLoadingIndicator() },
    error: @Composable (DomainError) -> Unit = { LexiconErrorState(it) },
    success: @Composable (T) -> Unit,
) { ... }
```

This eliminates `isLoading: Boolean` + nullable `data` + nullable `error` triple in every VM.

---

### 3.2 Domain — The Contract

#### 3.2.1 Structure — Feature-Aligned Packages

```
domain/
  auth/
    model/          AuthUser, FeatureAccess, SessionResult
    repository/     IAuthRepository, ISessionManager
    usecase/        LoginWithGoogleUseCase, LogoutUseCase, ...
  words/
    model/          Word, Tag, LearningStage, ProgressStats
    repository/     IWordRepository, ITagRepository
    usecase/        ReviewWordUseCase, GetDueWordsUseCase, ...
    algorithm/      SpacedRepetitionAlgorithm (pure function, extracted from ReviewWordUseCase)
  settings/
    model/          ReviewSettings, ThemeMode, Language
    repository/     ISettingsRepository
    usecase/        GetCurrentLanguageUseCase, SetLanguageUseCase, ...
  sync/
    model/          SyncStatus, ConflictResolution
    repository/     ISyncRepository
    usecase/        SyncRemoteToLocalUseCase, ResolveSyncConflictUseCase
  notifications/
    repository/     INotificationRepository, IPushTokenRepository
    usecase/        ...
  tts/
    repository/     ITtsRepository
    usecase/        ...
  error/
    DomainError.kt  (see §4.1)
```

#### 3.2.2 Extract the SRS Algorithm

`ReviewWordUseCase` currently contains the SM-2 spaced repetition logic inline. Extract it:

```kotlin
// domain/words/algorithm/SpacedRepetitionAlgorithm.kt
object SpacedRepetitionAlgorithm {
    data class ReviewInput(
        val quality: Int,           // 0-5
        val currentEaseFactor: Float,
        val currentInterval: Int,
        val currentRepetitions: Int,
    )

    data class ReviewOutput(
        val newEaseFactor: Float,
        val newInterval: Int,
        val newRepetitions: Int,
        val nextReviewAt: Instant,
    )

    fun calculate(input: ReviewInput): ReviewOutput { ... }
}

// ReviewWordUseCase becomes a thin orchestrator
class ReviewWordUseCase(
    private val wordRepository: IWordRepository,
    private val algorithm: SpacedRepetitionAlgorithm = SpacedRepetitionAlgorithm,
) : UseCase<ReviewWordParams, Word> {
    override suspend fun invoke(params: ReviewWordParams): Try<Word> {
        val output = algorithm.calculate(params.toInput())
        return wordRepository.updateReview(params.wordId, output)
    }
}
```

**Why this wins**: The algorithm is now a pure function — no DI needed, trivially unit-tested
with property-based tests (kotlinx-test with Kotest Arbitrary). Use cases stay thin.

#### 3.2.3 Remove Side-Methods from UseCases

`IsAuthenticatedUseCase` has both `invoke()` and `.asFlow()`. This breaks the `fun interface`
contract and creates two ways to do the same thing.

**Fix**: Create two separate use cases.

```kotlin
// REMOVE the asFlow() side-method from IsAuthenticatedUseCase
class IsAuthenticatedUseCase : NoParamUseCase<Boolean>  // one-shot
class ObserveAuthStateUseCase : NoParamFlowUseCase<Boolean>  // streaming

// Repository interface must provide both:
interface IAuthRepository {
    suspend fun isAuthenticated(): Try<Boolean>
    fun observeAuthState(): Flow<Boolean>
}
```

---

### 3.3 Data — The Plumbing

#### 3.3.1 Repository Decomposition

Each `*RepositoryImpl` has one job. Complex repositories that currently juggle local, remote,
and sync become three separate classes composed in the DI graph:

```kotlin
// CURRENT — WordRepositoryImpl: local + remote + sync all in one
class WordRepositoryImpl(
    localDataSource,
    remoteSyncHandler,
    conflictResolver,
) : IWordRepository

// TARGET — three single-responsibility classes
class WordLocalRepository(localDataSource: IWordLocalDataSource) : IWordLocalRepository
class WordRemoteRepository(remoteDataSource: IWordRemoteDataSource) : IWordRemoteRepository

// Sync is owned by the Sync Engine (§4.3), not the repository
// The IWordRepository interface in domain sees a clean merged view:
class WordRepository(
    local: IWordLocalRepository,
    syncEngine: ISyncEngine,
) : IWordRepository {
    override fun getWords(): Flow<List<Word>> = local.getWords()
    override suspend fun updateReview(id: Long, output: ReviewOutput): Try<Word> {
        val result = local.updateReview(id, output)
        syncEngine.enqueue(SyncOperation.ReviewWord(id)) // fire-and-forget
        return result
    }
}
```

#### 3.3.2 Network Layer — Typed Errors, No Throwable Leakage

```kotlin
// CURRENT — ApiClient returns Try<T> where Failure.throwable is untyped
// PROPOSED — Add ApiError sealed class in :core:network

sealed class ApiError : DomainError() {
    data class Http(val code: Int, val body: String?) : ApiError()
    data object Unauthorized : ApiError()
    data object NetworkUnavailable : ApiError()
    data class Timeout(val timeoutMs: Long) : ApiError()
    data object ServerError : ApiError()
    data class Deserialization(val cause: Throwable) : ApiError()
}

// ApiClient maps all Ktor exceptions to ApiError before returning Try<T>
// Nothing above ApiClient ever sees a raw HttpException or IOException
```

#### 3.3.3 SQLDelight — Schema Lifecycle Ownership

**Problem**: Schema migration strategy is not visible in the repository.

**Solution**: Own schema versions explicitly:

```
data/src/commonMain/sqldelight/
  migrations/
    1.sqm       # initial schema
    2.sqm       # add analytics queue
    3.sqm       # add streak table
  com/lexicon/
    Words.sq
    Tags.sq
    Settings.sq
    Analytics.sq
```

```kotlin
// AppDatabase.kt — explicit migration registration
val driver = createDriver(
    schema = LexiconDatabase.Schema,
    migrations = listOf(
        Migration(1, 2) { db -> db.execute(null, "ALTER TABLE ...", 0) },
        Migration(2, 3) { db -> db.execute(null, "CREATE TABLE streaks ...", 0) },
    )
)
```

Add a `DatabaseSchemaTest` that runs all migrations in order on an in-memory driver —
catches migration regressions before they reach users.

#### 3.3.4 Mapper Pattern — Enforce at Compile Time

Currently mappers are extension functions by convention. Make the contract explicit:

```kotlin
// :core:common
interface Mapper<in From, out To> {
    fun map(from: From): To
}

// :data — mappers implement the interface
class AuthResponseMapper : Mapper<AuthResponse, AuthUser> {
    override fun map(from: AuthResponse): AuthUser = AuthUser(
        id = from.id,
        email = from.email,
        // ...
    )
}
```

This makes mappers injectable, mockable in DataSource tests, and statically checked.

---

### 3.4 Presentation — The Surface

#### 3.4.1 Screen Anatomy — One Pattern Only

Every screen in the app must follow this exact structure, no exceptions:

```kotlin
// FeatureScreen.kt — the public entry point
@Composable
fun StudyScreen() {
    val viewModel = koinViewModel<StudyViewModel>()
    val state by viewModel.state()
    OnEvents(viewModel.effects) { effect ->
        when (effect) { ... }
    }
    StudyContent(
        state = state,
        onStartReview = viewModel::startReview,
        onWordReviewed = viewModel::reviewWord,
        onSpeakWord = viewModel::speakWord,
    )
}

// StudyContent.kt — pure composable, no ViewModel reference
@Composable
internal fun StudyContent(
    state: StudyState,
    onStartReview: () -> Unit,
    onWordReviewed: (Word, Int) -> Unit,
    onSpeakWord: (Word) -> Unit,
) { ... }
```

**Why split**: `StudyContent` is @Preview-able without DI. Lambda-based callbacks make
state machines trivially testable with `ComposeContentTestRule`.

#### 3.4.2 Navigation — Fully Type-Safe, Feature-Owned

```kotlin
// Each feature module owns its NavGraph extension
// feature/study/src/.../StudyNavGraph.kt
fun NavGraphBuilder.studyGraph() {
    composable<StudyRoute.Home> { StudyScreen() }
    composable<StudyRoute.Review> { entry ->
        val args = entry.toRoute<StudyRoute.Review>()
        ReviewScreen(tagId = args.tagId)
    }
}

// :app wires them together — knows nothing about screen content
fun NavHost(...) {
    authGraph()
    studyGraph()
    wordsGraph()
    insightsGraph()
    profileGraph()
    settingsGraph()
}
```

**Dead-code protection**: When a feature is removed, its entire `NavGraph` extension
disappears — zero orphan routes.

#### 3.4.3 Bottom Navigation — Data-Driven

```kotlin
// Currently hardcoded switch statements
// TARGET: data-driven with feature flag awareness

data class BottomTab(
    val route: Any,          // @Serializable type-safe route
    val labelRes: StringResource,
    val icon: ImageVector,
    val isEnabled: (FeatureAccess) -> Boolean = { true },
)

val BOTTOM_TABS = listOf(
    BottomTab(StudyRoute.Home, Res.string.tab_study, Icons.Study),
    BottomTab(WordsRoute.Home, Res.string.tab_words, Icons.Words),
    BottomTab(InsightsRoute.Home, Res.string.tab_insights, Icons.Insights) { it.hasPremiumAccess },
    BottomTab(ProfileRoute.Home, Res.string.tab_profile, Icons.Profile),
)
```

Adding a tab is adding one item to a list — no navigation graph changes needed.

---

### 3.5 Features — The Vertical Slices

#### 3.5.1 Feature Module Anatomy

Every feature module has the same internal structure:

```
feature/study/
  src/commonMain/kotlin/feature/study/
    di/
      StudyModule.kt        # Koin module — self-contained
    domain/                 # Feature-local use cases (if needed)
    ui/
      StudyScreen.kt        # Composable entry point
      StudyContent.kt       # Pure composable
      components/           # Feature-local reusable composables
    navigation/
      StudyNavGraph.kt      # NavGraphBuilder extension
      StudyRoute.kt         # @Serializable route definitions
    viewmodel/
      StudyViewModel.kt     # Thin orchestrator
      handlers/
        StudyProgressHandler.kt
        StudyReviewHandler.kt
        StudyTtsHandler.kt
    model/                  # Feature-local UI models (not domain models)
      StudyState.kt
      StudyEffect.kt
      ReviewItem.kt         # View-layer transform of Word
```

#### 3.5.2 Feature-Local DI

Each feature registers its own Koin module. `:app` includes them all:

```kotlin
// feature/study/di/StudyModule.kt
val studyModule = module {
    viewModelOf(::StudyViewModel)
    factoryOf(::StudyProgressHandler)
    factoryOf(::StudyReviewHandler)
    factoryOf(::StudyTtsHandler)
    // Use cases consumed by this feature
    singleOf(::GetDueWordsUseCase)
    singleOf(::ReviewWordUseCase)
    singleOf(::GetProgressStatsUseCase)
}

// app/src/.../AppModule.kt — compose root
fun appModule(...) = module {
    includes(
        coreModule(),
        dataModule(),
        analyticsModule(),
        studyModule,
        authModule,
        wordsModule,
        settingsModule,
        insightsModule,
        profileModule,
        leaderboardModule,
        onboardingModule,
    )
}
```

#### 3.5.3 Feature UI Models — Separate from Domain Models

Domain models are data contracts. UI models are view-optimised representations:

```kotlin
// domain model
data class Word(id, originalWord, translation, easeFactor, interval, nextReviewDate, ...)

// feature/study UI model
data class ReviewItem(
    val id: Long,
    val front: String,
    val back: String,
    val hint: String?,
    val difficultyLabel: String,   // "Easy", "Good", "Hard" — presentation decision
    val isOverdue: Boolean,        // derived, not stored
)

// Mapper lives in the feature module, not in domain
fun Word.toReviewItem(): ReviewItem = ReviewItem(
    id = id,
    front = originalWord,
    back = translation,
    hint = description.takeIf { it.isNotBlank() },
    difficultyLabel = level.toDifficultyLabel(),
    isOverdue = nextReviewDate < Clock.System.now(),
)
```

This keeps domain models clean and lets UI evolve independently.

---

### 3.6 Platforms — The Adapters

#### 3.6.1 No Logic in expect/actual

All logic moves to `:platforms` common interfaces. `expect/actual` is pure bridging:

```kotlin
// WRONG — logic in expect/actual
expect class TtsEngine {
    suspend fun speak(text: String, lang: String): Boolean
}

// RIGHT — interface in :platforms common, expect/actual is just a factory
interface ITtsEngine {
    suspend fun speak(text: String, language: Language, rate: Float): Try<Unit>
    fun stop()
    fun isLanguageSupported(language: Language): Boolean
}

// expect/actual creates the right implementation
expect fun createTtsEngine(context: PlatformContext): ITtsEngine
// actual (android) { return AndroidTtsEngine(context) }
// actual (ios)     { return IosTtsEngine() }
```

#### 3.6.2 Platform Context — Single Entry Point

```kotlin
// :platforms/common — PlatformContext carries everything platform-specific
data class PlatformContext(
    val applicationContext: Any,   // Android: Application, iOS: UIApplication proxy
    val appVersion: String,
    val buildNumber: Int,
    val isDebug: Boolean,
)

// Passed once from the platform entry point into the DI graph
// No more scattered Context passing through the call chain
```

#### 3.6.3 Secure Storage — Unified API

```kotlin
interface ISecureStorage {
    suspend fun put(key: String, value: String): Try<Unit>
    suspend fun get(key: String): Try<String?>
    suspend fun delete(key: String): Try<Unit>
    suspend fun clear(): Try<Unit>
}

// Android: EncryptedSharedPreferences
// iOS: Keychain via CryptoKit bridge
// Tests: InMemorySecureStorage (in :core:testing)
```

---

## 4. Cross-Cutting Concerns

### 4.1 Error Taxonomy

**Current**: `Throwable` propagates from data sources all the way to the UI. ViewModels
call `.message ?: "Unknown error"` — fragile and unlocalisable.

**Target**: A sealed `DomainError` hierarchy. UI maps `DomainError` → `StringResource`.

```kotlin
// domain/error/DomainError.kt
sealed class DomainError(message: String? = null) : Throwable(message) {

    // Auth errors
    sealed class Auth : DomainError() {
        data object NotAuthenticated : Auth()
        data object SessionExpired : Auth()
        data object AccountDeleted : Auth()
        data class ProviderError(val provider: String, override val cause: Throwable?) : Auth()
    }

    // Network errors
    sealed class Network : DomainError() {
        data object NoConnection : Network()
        data object Timeout : Network()
        data class ServerError(val code: Int) : Network()
        data object RateLimited : Network()
    }

    // Data errors
    sealed class Data : DomainError() {
        data class NotFound(val entity: String) : Data()
        data class InvalidInput(val field: String, val reason: String) : Data()
        data class SyncConflict(val conflictId: String) : Data()
    }

    // Feature errors
    sealed class Subscription : DomainError() {
        data object PremiumRequired : Subscription()
        data object EntitlementExpired : Subscription()
    }

    // Fallback
    data class Unknown(override val cause: Throwable) : DomainError(cause.message)
}
```

```kotlin
// :core:design-system — Error display is consistent and localisable
fun DomainError.toUserMessage(): StringResource = when (this) {
    is DomainError.Network.NoConnection -> Res.string.error_no_connection
    is DomainError.Auth.SessionExpired -> Res.string.error_session_expired
    is DomainError.Subscription.PremiumRequired -> Res.string.error_premium_required
    // ...
    is DomainError.Unknown -> Res.string.error_generic
}
```

**Migration path**: Each `ApiError` is mapped to a `DomainError` in the data layer.
`Try.Failure.throwable` is always a `DomainError` by the time it reaches a ViewModel.

---

### 4.2 Analytics as a First-Class Layer

**Current problem**: Analytics calls are scattered inline in ViewModels. `AuthViewModel`
alone has 20+ `analyticsTracker.track(...)` calls mixed with business logic.

**Target**: Analytics is an independent `:analytics` module with a typed event bus.
ViewModels know nothing about tracking.

```
:analytics
  event/
    AnalyticsEvent.kt     # sealed class hierarchy — all events defined here
    AnalyticsTracker.kt   # interface: fun track(event: AnalyticsEvent)
  impl/
    FirebaseAnalyticsTracker.kt
    CompositeAnalyticsTracker.kt  # fan-out to multiple backends
    LoggingAnalyticsTracker.kt    # debug only
  middleware/
    AnalyticsMiddleware.kt        # wires BaseViewModel effects → tracking
```

```kotlin
// All analytics events are typed and centrally defined
sealed class AnalyticsEvent {
    sealed class Auth : AnalyticsEvent() {
        data class LoginStarted(val provider: String) : Auth()
        data class LoginSuccess(val provider: String) : Auth()
        data class LoginFailed(val provider: String, val errorCode: String) : Auth()
        data object LogoutCompleted : Auth()
    }

    sealed class Study : AnalyticsEvent() {
        data class ReviewStarted(val wordCount: Int, val tagId: Long?) : Study()
        data class WordReviewed(val quality: Int, val wordId: Long) : Study()
        data class ReviewSessionCompleted(val wordsReviewed: Int, val durationMs: Long) : Study()
    }

    sealed class Subscription : AnalyticsEvent() {
        data object PremiumPromptShown : Subscription()
        data object UpgradeStarted : Subscription()
        data class UpgradeCompleted(val plan: String) : Subscription()
    }
}
```

```kotlin
// ViewModels emit effects — analytics middleware listens and tracks
// Zero analytics code in ViewModels
class AnalyticsMiddleware(private val tracker: AnalyticsTracker) {
    fun <F : Any> observeEffects(effects: Flow<F>) {
        effects.onEach { effect ->
            effect.toAnalyticsEvent()?.let { tracker.track(it) }
        }.launchIn(analyticsScope)
    }
}

// Effect → AnalyticsEvent mapping lives in :analytics, not in features
fun Any.toAnalyticsEvent(): AnalyticsEvent? = when (this) {
    is AuthEffect.LoginSuccess -> AnalyticsEvent.Auth.LoginSuccess(provider)
    is StudyEffect.ReviewCompleted -> AnalyticsEvent.Study.ReviewSessionCompleted(wordsReviewed, durationMs)
    else -> null
}
```

**Why this wins**:
- ViewModels have zero analytics code → pure business logic
- All events are centrally auditable in one file
- Swap Firebase for any other backend by changing one `AnalyticsTracker` binding
- Analytics tests don't require ViewModel tests to change
- Analytics events are typed → refactoring is compile-time safe

---

### 4.3 Offline-First Sync Engine

**Current problem**: Sync logic is scattered across `WordRepositoryImpl`,
`SyncRemoteToLocalUseCase`, and a `remoteSyncHandler`. Retry is ad-hoc. Conflict
resolution is hidden inside repositories.

**Target**: A dedicated `:core:sync` module with a write-ahead queue.

```kotlin
// :core:sync
interface ISyncEngine {
    fun enqueue(operation: SyncOperation)
    suspend fun drainNow(): Try<SyncResult>
    fun observeStatus(): Flow<SyncStatus>
}

sealed class SyncOperation {
    data class ReviewWord(val wordId: Long, val quality: Int, val reviewedAt: Instant) : SyncOperation()
    data class CreateWord(val word: Word) : SyncOperation()
    data class UpdateWord(val word: Word) : SyncOperation()
    data class DeleteWord(val wordId: Long) : SyncOperation()
    data class CreateTag(val tag: Tag) : SyncOperation()
}

sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Syncing : SyncStatus()
    data class Failed(val pendingCount: Int, val lastError: DomainError) : SyncStatus()
    data class Success(val syncedAt: Instant) : SyncStatus()
}
```

```kotlin
// SyncEngine implementation
class SyncEngineImpl(
    private val queue: ISyncQueue,        // SQLDelight-backed write-ahead log
    private val remoteExecutor: ISyncRemoteExecutor,
    private val conflictResolver: IConflictResolver,
    private val networkMonitor: INetworkMonitor,
) : ISyncEngine {

    init {
        // Auto-drain on network reconnect
        networkMonitor.isOnline
            .filter { it }
            .onEach { drainNow() }
            .launchIn(engineScope)
    }

    override fun enqueue(operation: SyncOperation) {
        queue.enqueue(operation)
        engineScope.launch { drainNow() }
    }

    override suspend fun drainNow(): Try<SyncResult> {
        val pending = queue.dequeueAll()
        return pending
            .map { remoteExecutor.execute(it) }
            .fold(...)
    }
}
```

**Properties**:
- SQLDelight WAL survives app kill → no lost writes
- Exponential backoff with jitter on network errors
- Conflict resolution is an explicit interface, not hidden logic
- `observeStatus()` lets UI show sync badge (e.g., "Syncing..." in top bar)

---

### 4.4 Caching Strategy

**Current problem**: `FeatureAccessResponse` and other remote data are fetched on-demand
with no in-memory cache. Every screen that needs `featureAccess` triggers a network call.

**Target**: A multi-tier cache with explicit TTL policy per data type.

```kotlin
// :core:common
interface ICache<K, V> {
    suspend fun get(key: K): V?
    suspend fun put(key: K, value: V, ttl: Duration)
    suspend fun invalidate(key: K)
    suspend fun clear()
}

// :data — InMemoryCache backed by a ConcurrentHashMap + expiry
class InMemoryCache<K, V> : ICache<K, V> { ... }

// Cache policies — defined per data type
object CachePolicy {
    val featureAccess = CacheTtl(5.minutes)   // refresh every 5 min
    val leaderboard  = CacheTtl(2.minutes)   // refresh every 2 min
    val userProfile  = CacheTtl(10.minutes)  // refresh every 10 min
    val dueWords     = CacheTtl(30.seconds)  // refresh every 30s (study mode)
}

// Repository uses cache transparently
class FeatureAccessRepository(
    private val remote: IFeatureAccessRemoteDataSource,
    private val cache: ICache<String, FeatureAccess>,
) : IFeatureAccessRepository {
    override suspend fun getFeatureAccess(): Try<FeatureAccess> {
        cache.get("current")?.let { return Try.success(it) }
        return remote.fetchFeatureAccess()
            .doOnSuccess { cache.put("current", it, CachePolicy.featureAccess) }
    }
}
```

**Stale-While-Revalidate** for streamed data:

```kotlin
// Return cached data immediately, refresh in background
fun observeLeaderboard(): Flow<UiState<List<LeaderboardEntry>>> = flow {
    val cached = cache.get("leaderboard")
    if (cached != null) emit(UiState.Success(cached))
    else emit(UiState.Loading)

    remote.fetchLeaderboard()
        .doOnSuccess { cache.put("leaderboard", it, CachePolicy.leaderboard) }
        .fold(
            onSuccess = { emit(UiState.Success(it)) },
            onFailure = { if (cached == null) emit(UiState.Error(it.toDomainError())) }
        )
}
```

---

### 4.5 Feature Flags & Entitlements

**Current problem**: Premium feature gating is ad-hoc — some screens check
`featureAccess.hasPremiumAccess`, some don't check at all, some check in the VM,
some check in the repository.

**Target**: A unified entitlement gate at the navigation layer.

```kotlin
// domain — entitlement check is a domain concept
class RequiresEntitlementUseCase(
    private val featureAccessRepository: IFeatureAccessRepository,
) : UseCase<Entitlement, Unit> {
    override suspend fun invoke(params: Entitlement): Try<Unit> {
        return featureAccessRepository.getFeatureAccess()
            .flatMap { access ->
                if (access.allows(params)) Try.success(Unit)
                else Try.failure(DomainError.Subscription.PremiumRequired)
            }
    }
}

// Navigation-layer guard — declared on routes
@Serializable data object InsightsRoute : EntitledRoute(Entitlement.Premium)

// NavGraphBuilder extension checks entitlement before composing the screen
fun NavGraphBuilder.guardedComposable(
    route: EntitledRoute,
    content: @Composable () -> Unit,
) {
    composable(route) {
        EntitlementGuard(route.entitlement, onUpgradeRequired = { navController.navigate(SubscriptionRoute) }) {
            content()
        }
    }
}
```

---

## 5. Testing Strategy

### 5.1 Test Pyramid — Target Ratios

```
                    ┌────────────────┐
                    │   E2E (5%)     │  Maestro or Espresso — happy paths only
                    ├────────────────┤
                    │ Integration    │  Repository + DataSource wired together
                    │    (15%)       │  Real SQLDelight, MockEngine for HTTP
                    ├────────────────┤
                    │   Unit (80%)   │  VM handlers, use cases, algorithm, mappers
                    │                │  Fast, hermetic, no I/O
                    └────────────────┘
```

### 5.2 `:core:testing` — Shared Fake Infrastructure

**Current problem**: Manual fakes are duplicated across 20+ test files. When
`IAuthRepository` adds a method, every fake breaks.

**Target**: Canonical fakes live in `:core:testing`. Feature tests inherit or compose:

```kotlin
// :core:testing/fake/FakeAuthRepository.kt
class FakeAuthRepository : IAuthRepository {
    var loginResult: Try<AuthUser> = Try.success(TestData.authUser)
    var logoutResult: Try<Unit> = Try.success(Unit)
    var authStateFlow = MutableStateFlow(false)

    override suspend fun loginWithGoogle(idToken: String) = loginResult
    override suspend fun loginWithApple(token: String, nonce: String) = loginResult
    override suspend fun logout() = logoutResult
    override fun observeAuthState(): Flow<Boolean> = authStateFlow
    // ... all methods have sensible defaults
}

// :core:testing/data/TestData.kt
object TestData {
    val authUser = AuthUser(id = "test-user", email = "test@lexicon.app", ...)
    val word = Word(id = 1L, originalWord = "Hola", translation = "Hello", ...)
    val tag = Tag(id = 1L, name = "Spanish", wordCount = 5L)
}

// Feature tests compose fakes, not redefine them
class AuthViewModelTest : ViewModelTestBase() {
    private val fakeRepo = FakeAuthRepository()

    @Test fun `login emits success effect`() = runTest {
        fakeRepo.loginResult = Try.success(TestData.authUser)
        val vm = AuthViewModel(loginHandler = AuthLoginHandler(stateAccess, fakeRepo, ...))
        vm.loginWithGoogle("fake-token")
        assertEquals(TestData.authUser, vm.currentState.user)
    }
}
```

### 5.3 Handler-Level Tests

With the ViewModel decomposition pattern, handlers are tested independently:

```kotlin
class AuthLoginHandlerTest {
    private val stateAccess = FakeStateAccess(AuthState.initial())
    private val fakeRepo = FakeAuthRepository()
    private val handler = AuthLoginHandler(stateAccess, fakeRepo)

    @Test fun `loginWithGoogle on success updates state with user`() = runTest {
        fakeRepo.loginResult = Try.success(TestData.authUser)
        handler.loginWithGoogle("fake-token")
        assertEquals(TestData.authUser, stateAccess.currentState.user)
    }

    @Test fun `loginWithGoogle on failure sets error in state`() = runTest {
        fakeRepo.loginResult = Try.failure(DomainError.Auth.ProviderError("google", null))
        handler.loginWithGoogle("bad-token")
        assertNotNull(stateAccess.currentState.error)
        assertFalse(stateAccess.currentState.isLoading)
    }
}
```

### 5.4 Algorithm Tests — Property-Based

```kotlin
// SpacedRepetitionAlgorithmTest.kt
class SpacedRepetitionAlgorithmTest {
    @Test fun `ease factor never drops below 1_3`() {
        repeat(100) {
            val input = ReviewInput(quality = 0, currentEaseFactor = Random.nextFloat() * 3f + 1.3f, ...)
            val output = SpacedRepetitionAlgorithm.calculate(input)
            assertTrue(output.newEaseFactor >= 1.3f)
        }
    }

    @Test fun `quality 5 always increases interval`() {
        val input = ReviewInput(quality = 5, currentInterval = 10, ...)
        val output = SpacedRepetitionAlgorithm.calculate(input)
        assertTrue(output.newInterval > input.currentInterval)
    }
}
```

### 5.5 Sync Engine Tests — Fake Network Monitor

```kotlin
class SyncEngineTest {
    private val fakeQueue = FakeSyncQueue()
    private val fakeExecutor = FakeSyncRemoteExecutor()
    private val networkMonitor = FakeNetworkMonitor(isOnline = false)
    private val engine = SyncEngineImpl(fakeQueue, fakeExecutor, FakeConflictResolver(), networkMonitor)

    @Test fun `operations enqueued offline are drained when network reconnects`() = runTest {
        engine.enqueue(SyncOperation.ReviewWord(1L, 4, Clock.System.now()))
        assertEquals(1, fakeQueue.pending.size)
        assertEquals(0, fakeExecutor.executedCount)

        networkMonitor.goOnline()
        advanceUntilIdle()

        assertEquals(0, fakeQueue.pending.size)
        assertEquals(1, fakeExecutor.executedCount)
    }
}
```

### 5.6 Database Migration Tests

```kotlin
class DatabaseMigrationTest {
    @Test fun `all migrations run cleanly on fresh install`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LexiconDatabase.Schema.migrate(driver, oldVersion = 0, newVersion = LexiconDatabase.Schema.version)
        // Assert schema version is correct and critical tables exist
        val db = LexiconDatabase(driver)
        assertNotNull(db.wordsQueries)
    }

    @Test fun `migration 2 to 3 preserves existing word data`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LexiconDatabase.Schema.migrate(driver, 0, 2)
        // Insert test data at v2 schema
        driver.execute(null, "INSERT INTO words (...) VALUES (...)", 0)
        // Migrate to v3
        LexiconDatabase.Schema.migrate(driver, 2, 3)
        // Assert data survived
        val count = driver.executeQuery(null, "SELECT COUNT(*) FROM words", 0, 0).getLong(0)
        assertEquals(1L, count)
    }
}
```

---

## 6. Build System & CI/CD

### 6.1 Convention Plugins — Expand Coverage

Current `build-logic` has convention plugins but doesn't fully enforce module boundaries.

```kotlin
// build-logic/convention/src/.../ModuleBoundaryPlugin.kt
// Uses Gradle module-graph plugin to enforce dependency rules at build time
// Any violation fails the build — not a code review finding

// :domain must have zero non-stdlib dependencies
tasks.register("checkDomainBoundaries") {
    doLast {
        val domainDeps = project(":domain").configurations.runtimeClasspath.resolvedConfiguration
        val violations = domainDeps.resolvedArtifacts.filter { it.moduleVersion.id.group != "org.jetbrains.kotlin" }
        check(violations.isEmpty()) { "Domain module has illegal dependencies: $violations" }
    }
}
```

### 6.2 Gradle Catalogs — Pin Everything

```toml
# gradle/libs.versions.toml — every dependency pinned, no dynamic versions
[versions]
kotlin = "2.3.10"
ktor = "3.4.1"
sqldelight = "2.3.1"

# No "+" or "latest.release" anywhere in the codebase
# Dependabot PRs upgrade versions via catalog only
```

### 6.3 CI Pipeline — Layered Checks

```yaml
# .github/workflows/pr.yml
jobs:
  lint:           # Detekt + custom rules (StatelessUseCase, NoBangBang)
  boundaries:     # Module boundary check (fast, < 30s)
  unit-tests:     # commonTest (< 3 min)
  android-build:  # assembleDebug (< 5 min)
  ios-build:      # linkDebugFrameworkIosSimulatorArm64 (< 8 min)
  android-tests:  # testDebugUnitTest + DB migration tests
  coverage:       # Kover report — fail if < 80% on domain + core
```

**Fast feedback**: Lint + boundaries run in < 1 minute and block the PR immediately.
Unit tests are separate — don't wait for the slow build.

### 6.4 Version Management

```bash
# Current: manual scripts
./scripts/bump-version.sh --hotfix

# Target: automated via CI on merge to main
# versioning.properties updated automatically
# Git tag created
# App Store / Play Store upload triggered via Fastlane
```

---

## 7. Observability

### 7.1 Structured Logging

```kotlin
// :core:common — structured logger
interface ILogger {
    fun debug(tag: String, message: String, metadata: Map<String, Any?> = emptyMap())
    fun info(tag: String, message: String, metadata: Map<String, Any?> = emptyMap())
    fun warn(tag: String, message: String, error: Throwable? = null, metadata: Map<String, Any?> = emptyMap())
    fun error(tag: String, message: String, error: Throwable, metadata: Map<String, Any?> = emptyMap())
}

// android: Timber, ios: os.log — both route through ILogger
// Tests: InMemoryLogger — assert log calls in tests
```

### 7.2 Performance Tracing — First-Class

```kotlin
// :platforms — IPerformanceTracer already exists, expand it
interface IPerformanceTracer {
    suspend fun <T> traceAsync(name: String, attributes: Map<String, String> = emptyMap(), block: suspend () -> T): T
    fun startTrace(name: String): Trace
}

// Use cases automatically wrapped in traces via DI decorator
class TracingUseCase<P, R>(
    private val delegate: UseCase<P, R>,
    private val tracer: IPerformanceTracer,
) : UseCase<P, R> {
    override suspend fun invoke(params: P): Try<R> =
        tracer.traceAsync(delegate::class.simpleName ?: "UseCase") { delegate(params) }
}
```

### 7.3 Crash Reporting — Domain Error Enrichment

```kotlin
// All crashes are enriched with domain context before Firebase/Sentry receives them
class CrashReporter(private val firebaseCrashlytics: FirebaseCrashlytics) {
    fun report(error: DomainError) {
        firebaseCrashlytics.setCustomKey("error_type", error::class.simpleName ?: "unknown")
        firebaseCrashlytics.setCustomKey("error_category", error.category())
        if (error is DomainError.Unknown) {
            firebaseCrashlytics.recordException(error.cause)
        } else {
            // Domain errors are expected — log as non-fatal
            firebaseCrashlytics.log("Domain error: ${error::class.simpleName}")
        }
    }
}
```

---

## 8. Migration Playbook

The current codebase is 70% of the way there. Here's the exact order to complete migration
without breaking production:

### Phase 1 — Foundation (no feature changes, 1 week)

1. **Create `DomainError` hierarchy** — replace raw `Throwable` in all `Try.Failure` returns
2. **Create `:core:testing` shared fakes** — extract from test files, centralise
3. **Add `UiState<T>`** — opt-in per screen, no forced migration
4. **Add module boundary Gradle check** — make violations fail CI
5. **Document SQLDelight schemas** — migrate `.sql` files into `migrations/` folder

### Phase 2 — Feature Migration (one feature at a time, 2-3 weeks)

6. **Migrate `:presentation` screens to features** — one screen per PR
   - StudyScreen → `:feature:study`
   - SettingsScreen → `:feature:settings`
   - Delete `:presentation` when empty
7. **Move DI into feature modules** — `AuthModule` → `feature/auth/di/AuthModule.kt`
8. **Extract StateHandlers** — start with `AuthViewModel` (largest)

### Phase 3 — Cross-Cutting (2 weeks)

9. **Implement `:analytics` module** — strip all `analyticsTracker` calls from ViewModels
10. **Implement Sync Engine** — replace scattered sync logic
11. **Implement caching layer** — `FeatureAccess`, `Leaderboard`, `UserProfile`
12. **Add E2E tests** — cover login, study session, word management happy paths

### Phase 4 — Observability & Quality (1 week)

13. **Structured logging** — replace `println` and ad-hoc log calls
14. **Detekt custom rules** — `StatelessUseCaseRule`, `NoBareThrowableRule`
15. **Coverage gates** — domain + core must hit 80%

---

## 9. What to Keep, What to Kill

### Keep (it's genuinely good)

| Component | Why it's good |
|---|---|
| `Try<T>` | Correct, cancellation-safe, composable — don't replace with Kotlin `Result` |
| `BaseViewModel<S, F>` | Single state + one-shot effects is the right mental model |
| `UseCase<P, R>` / `FlowUseCase<P, R>` | Stateless fun interfaces — zero ceremony |
| `OnEvents` composable | Correct lifecycle-aware effect collection |
| Fakes over mocks philosophy | Prevents test-prod divergence |
| SQLDelight | Type-safe, multiplatform, fast — correct choice |
| Ktor | Right for KMP — don't swap for OkHttp |
| Koin | Lighter than Hilt for KMP — keep |
| `mutableStateOf` snapshot state | Compose-native, no StateFlow overhead |
| Type-safe navigation with `@Serializable` | Compile-time safe — keep and expand |
| `SRS algorithm` | SM-2 is well-established — just extract it |

### Kill (replace these patterns)

| Pattern | Problem | Replacement |
|---|---|---|
| Raw `Throwable` in `Try.Failure` | Untyped, unlocalizable | `DomainError` sealed hierarchy |
| `isLoading: Boolean` + nullable `data` in state | Fragmented state machine | `UiState<T>` |
| Analytics calls in ViewModels | Coupling, hard to audit | `:analytics` middleware |
| Manual fakes per test file | Duplication, drift risk | Shared fakes in `:core:testing` |
| Sync scattered in repositories | Non-deterministic retry | Dedicated `SyncEngine` |
| `IsAuthenticatedUseCase.asFlow()` side method | Breaks fun interface contract | Separate `ObserveAuthStateUseCase` |
| 200+ line ViewModels | Low cohesion | Handler decomposition |
| Uncached feature access fetch | Wasted network | TTL-based in-memory cache |
| Feature DI in flat AppModule | No feature isolation | Feature-local Koin modules |
| `!!` operator anywhere | Crash risk | Exhaustive `when`, Elvis, `requireNotNull` |

---

## Final Thought

The Lexicon codebase is not broken — it's mid-evolution. The right abstractions are already
in place. The work is: complete the vertical slice migration, pull analytics out of ViewModels,
own the error taxonomy, and add the sync engine. None of these require rewriting what's already
working.

The result is an app where:
- A new developer can read a feature module and understand it completely without reading other modules
- Adding a new feature requires creating one new module, not editing five existing ones
- Analytics is auditable in one file, not scattered across fifty
- A sync bug has one place to look, not seven
- Every test failure points to exactly the thing that's broken

That is maintainable, scalable, and efficient Lexicon.
