---
name: di-patterns
description: Koin DI registration patterns for every layer — single vs factory vs viewModelOf, interface binding, module organization, and scope rules
argument-hint: "<layer or component to register>"
user-invocable: true
allowed-tools: ["Read", "Write", "Edit", "Glob", "Grep"]
---

# Lexicon DI Patterns (Koin)

All DI registrations go in `composeApp/src/commonMain/kotlin/di/AppModule.kt` (current flat structure).
In the target feature module architecture, each feature owns a `FeatureModule.kt`.

---

## Scope Rules

| Layer | Scope | Reason |
|---|---|---|
| `DataSource` (remote) | `single` | Shared HTTP client; one instance per app |
| `DataSource` (local) | `single` | Shared DB connection; one instance per app |
| `Repository` | `single` | Stateless; safe to share across VMs |
| `UseCase` | `factory` | Stateless; cheap to create; avoids accidental state |
| `ViewModel` | `viewModel` | Tied to screen lifecycle; one per screen |
| `HttpClient` | `single` | One configured Ktor client for the whole app |

---

## Registration Syntax

### Single (one instance, app-scoped)

```kotlin
// Bind implementation to its interface (preferred pattern)
singleOf(::WordRemoteDataSourceImpl) { bind<IWordRemoteDataSource>() }
singleOf(::WordLocalDataSourceImpl) { bind<IWordLocalDataSource>() }
singleOf(::WordRepositoryImpl) { bind<IWordRepository>() }

// With manual constructor args (when Koin can't auto-resolve)
single<IAuthRepository> {
    AuthRepositoryImpl(
        remote = get(),
        local = get(),
        tokenStore = get(),
    )
}
```

### Factory (new instance per injection)

```kotlin
factoryOf(::GetDueWordsUseCase)
factoryOf(::ReviewWordUseCase)
factoryOf(::SyncWordsUseCase)

// Parameterized use cases — Koin resolves constructor params automatically
factoryOf(::ImportViaFileUseCase)
```

### ViewModel

```kotlin
viewModelOf(::StudyViewModel)
viewModelOf(::WordManagerViewModel)
viewModelOf(::AuthViewModel)
```

---

## Full Layer Registration Example

```kotlin
// One feature's complete DI block — order: DataSources → Repository → UseCases → ViewModel
val wordModule = module {
    // Data sources
    singleOf(::WordRemoteDataSourceImpl) { bind<IWordRemoteDataSource>() }
    singleOf(::WordLocalDataSourceImpl) { bind<IWordLocalDataSource>() }

    // Repository
    singleOf(::WordRepositoryImpl) { bind<IWordRepository>() }

    // Use cases — factory, not single
    factoryOf(::GetAllWordsUseCase)
    factoryOf(::GetDueWordsUseCase)
    factoryOf(::ReviewWordUseCase)
    factoryOf(::UpdateWordUseCase)
    factoryOf(::DeleteWordUseCase)
    factoryOf(::SyncWordsUseCase)

    // ViewModel
    viewModelOf(::WordManagerViewModel)
}
```

---

## Current AppModule.kt Structure

Until feature modules are extracted, everything is registered flat:

```kotlin
val appModule = module {
    // Core infrastructure
    single<HttpClient> { buildHttpClient(get(), get()) }
    single { DatabaseDriverFactory(get()) }
    single { LexiconDatabase(get<DatabaseDriverFactory>().createDriver()) }

    // Analytics (separate file: AnalyticsModule.kt)
    includes(analyticsModule)

    // Words
    singleOf(::WordRemoteDataSourceImpl) { bind<IWordRemoteDataSource>() }
    singleOf(::WordLocalDataSourceImpl) { bind<IWordLocalDataSource>() }
    singleOf(::WordRepositoryImpl) { bind<IWordRepository>() }
    factoryOf(::GetDueWordsUseCase)
    factoryOf(::ReviewWordUseCase)
    // ...
    viewModelOf(::ReviewViewModel)

    // Auth
    singleOf(::AuthRemoteDataSourceImpl) { bind<IAuthRemoteDataSource>() }
    singleOf(::AuthRepositoryImpl) { bind<IAuthRepository>() }
    factoryOf(::LoginWithGoogleUseCase)
    viewModelOf(::AuthViewModel)

    // etc.
}
```

---

## Interface Binding Patterns

### One implementation per interface (most common)

```kotlin
singleOf(::WordRepositoryImpl) { bind<IWordRepository>() }
```

### Multiple implementations (strategy pattern)

```kotlin
single<ISchedulingAlgorithm> {
    val preset = get<ISettingsRepository>().getReviewPreset()
    when (preset) {
        ReviewPreset.EASY -> EasyScheduler()
        ReviewPreset.BALANCED -> BalancedScheduler()
        ReviewPreset.EXPERT -> ExpertScheduler()
    }
}
```

### Platform-specific (expect/actual in platforms module)

```kotlin
// In commonMain/di/AppModule.kt — declare as single
single<ISecureStorage> { SecureStorageFactory.create() }

// In androidMain — actual SecureStorageFactory delegates to Keystore
// In iosMain — actual SecureStorageFactory delegates to Keychain
```

---

## Koin in Compose Screens

```kotlin
// Screen composable — get ViewModel via koinViewModel()
@Composable
fun StudyScreen(
    navigator: StudyNavigator,
    viewModel: StudyViewModel = koinViewModel(),
) {
    val state by viewModel.state()
    // ...
}

// Pass non-ViewModel dependencies — inject via get() in composable only as last resort
// Prefer: pass through ViewModel constructor
```

---

## Anti-Patterns

```kotlin
// BAD — single for UseCase (hides accidental state, wrong lifecycle)
singleOf(::ReviewWordUseCase)  // should be factoryOf

// BAD — factory for Repository (creates new DB connection per injection)
factoryOf(::WordRepositoryImpl)  // should be singleOf

// BAD — manual get() chains inside single {} when singleOf works
single { WordRepositoryImpl(get(), get()) }  // prefer singleOf(::WordRepositoryImpl) { bind<...>() }

// BAD — registering in Screen composable directly
@Composable fun Screen() {
    val repo = KoinPlatform.getKoin().get<IWordRepository>()  // NEVER
}

// BAD — constructor property injection (field injection)
class ReviewViewModel : BaseViewModel<...>() {
    val useCase: ReviewWordUseCase by inject()  // NEVER — use constructor injection
}

// GOOD — constructor injection always
class ReviewViewModel(
    private val reviewWordUseCase: ReviewWordUseCase,
    private val getDueWordsUseCase: GetDueWordsUseCase,
) : BaseViewModel<ReviewState, ReviewEffect>()
```

---

## Target Feature Module DI (when extracted)

When the flat structure is migrated to feature modules, each feature exports its own Koin module:

```kotlin
// feature/study/src/commonMain/kotlin/feature/study/StudyModule.kt
val studyModule = module {
    singleOf(::StudyRemoteDataSourceImpl) { bind<IStudyRemoteDataSource>() }
    factoryOf(::GetDueWordsUseCase)
    factoryOf(::ReviewWordUseCase)
    viewModelOf(::ReviewViewModel)
    viewModelOf(::StudyProgressViewModel)
}

// composeApp/di/AppModule.kt
val appModule = module {
    includes(studyModule, authModule, wordsModule, profileModule, importModule)
}
```

---

## Checklist

1. DataSource → `singleOf` with `bind<IInterface>()`
2. Repository → `singleOf` with `bind<IInterface>()`
3. UseCase → `factoryOf`
4. ViewModel → `viewModelOf`
5. Never field-inject with `by inject()` — always constructor inject
6. Never `get<T>()` inside a Compose composable — use `koinViewModel()` for VMs
7. New registrations go in `AppModule.kt` (or the feature's own module when extracted)
8. `HttpClient` and DB driver are `single` — never `factory`
