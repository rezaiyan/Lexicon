---
name: error-handling
description: Complete error handling guide — DomainError hierarchy, Try<T> propagation from DataSource to UI, UiState<T> for screen display, and what never to do
argument-hint: "<context — e.g. 'auth flow' or 'sync errors'>"
user-invocable: true
allowed-tools: ["Read", "Write", "Edit", "Glob", "Grep"]
---

# Lexicon Error Handling

Error handling is the most common source of silent failures. Follow this guide exactly.

---

## The Error Propagation Stack

```
DataSource (throws) → Repository (wraps in Try<T>) → UseCase (returns Try<T>) → ViewModel (.reduce()) → Screen (UiState.Error)
```

Each layer has one job. Never let errors leak past the layer boundary they belong to.

---

## Layer 1: Data Source — Always Throw

Data sources throw raw exceptions. They don't wrap. Callers decide how to handle.

```kotlin
class WordRemoteDataSourceImpl(private val client: HttpClient) : IWordRemoteDataSource {
    override suspend fun fetchWords(): List<WordDto> =
        client.get("words").body()  // Ktor throws on network failure — let it
}
```

---

## Layer 2: Repository — Always Wrap in Try<T>

Repositories are the **only** place that converts throws → `Try<T>`. Use `Try { }` block or `.getOrElse`.

```kotlin
class WordRepositoryImpl(
    private val remote: IWordRemoteDataSource,
    private val local: IWordLocalDataSource,
) : IWordRepository {

    override suspend fun syncWithRemote(): Try<Unit> = Try {
        val words = remote.fetchWords()  // may throw
        local.replaceAll(words.map { it.toDomain() })  // may throw
    }

    override suspend fun getWordById(id: Int): Word? =
        local.getWordById(id)  // non-fallible reads return T? directly — no Try needed
}
```

### Rules
- `suspend fun` returns `Try<T>` — never bare type, never throws
- `fun` returning `Flow<T>` — errors surface via Flow `.catch {}`, not Try
- `T?` is allowed only for reads that structurally cannot fail (e.g., local DB lookup)

---

## Layer 3: Use Case — Thread Through Try<T>

Use cases don't catch — they compose. Return whatever the repository gives you.

```kotlin
class SyncWordsUseCase(
    private val wordRepository: IWordRepository,
) : NoParamUseCase<Unit> {
    override suspend fun invoke(params: Unit): Try<Unit> =
        wordRepository.syncWithRemote()  // just thread through
}

// Multi-step use case: chain with flatMap
class LoginUseCase(
    private val authRepository: IAuthRepository,
    private val wordRepository: IWordRepository,
) : UseCase<LoginParams, AuthUser> {
    override suspend fun invoke(params: LoginParams): Try<AuthUser> =
        authRepository.login(params.credential)
            .flatMap { user -> wordRepository.syncWithRemote().map { user } }
}
```

---

## Layer 4: ViewModel — Fold into State with .reduce()

```kotlin
fun syncWords() {
    viewModelScope.launch {
        updateState { copy(isLoading = true) }
        syncWordsUseCase(Unit).reduce(
            onSuccess = { copy(isLoading = false, words = it) },
            onFailure = { copy(isLoading = false, error = it.toUserMessage()) }
        )
    }
}

// For Flow use cases — errors go via .catch {}
private fun observeWords() {
    getWordsUseCase(Unit)
        .onEach { words -> updateState { copy(words = words) } }
        .catch { e -> updateState { copy(error = e.toUserMessage()) } }
        .launchIn(viewModelScope)
}
```

---

## Layer 5: Screen — UiState<T> for async sections

```kotlin
@Composable
fun StudyScreen(viewModel: StudyViewModel = koinViewModel()) {
    val state by viewModel.state()
    OnEvents(viewModel.effects) { effect ->
        when (effect) {
            is StudyEffect.ShowError -> /* snackbar */
        }
    }
    when (val wordState = state.words) {
        is UiState.Loading -> LoadingIndicator()
        is UiState.Success -> WordList(wordState.data)
        is UiState.Error   -> ErrorCard(wordState.message, onRetry = viewModel::loadWords)
        is UiState.Empty   -> EmptyState()
    }
}
```

---

## DomainError Hierarchy (Target Architecture)

When error type matters to the UI (e.g., show paywall on `Commerce.PremiumRequired`), use typed `DomainError` instead of raw `Throwable`. Define in `:domain`:

```kotlin
sealed class DomainError : Exception() {
    sealed class Learning : DomainError() {
        data class NoDueCards(val filter: String?) : Learning()
        data object SessionNotActive : Learning()
    }
    sealed class Data : DomainError() {
        data class NotFound(val type: String, val id: String) : Data()
        data class DuplicateCard(val word: String) : Data()
    }
    sealed class Network : DomainError() {
        data object NoConnection : Network()
        data object Timeout : Network()
        data class ServerError(val code: Int, val body: String?) : Network()
    }
    sealed class Auth : DomainError() {
        data object NotAuthenticated : Auth()
        data object SessionExpired : Auth()
    }
    sealed class Commerce : DomainError() {
        data object PremiumRequired : Commerce()
        data object PurchaseFailed : Commerce()
    }
}
```

### Where to throw DomainError

```kotlin
// Repository maps raw exceptions → DomainError
override suspend fun getDueWords(filter: SessionFilter): Try<List<Word>> = Try {
    val words = local.getDueWords(filter)
    if (words.isEmpty()) throw DomainError.Learning.NoDueCards(filter.tagName)
    words
}

// ViewModel branches on typed error
reviewUseCase(params).reduce(
    onSuccess = { copy(words = it) },
    onFailure = { error ->
        when (error) {
            is DomainError.Commerce.PremiumRequired ->
                copy(showPaywall = true)
            is DomainError.Learning.NoDueCards ->
                copy(words = emptyList(), message = "All caught up!")
            else ->
                copy(errorMessage = error.toUserMessage())
        }
    }
)
```

---

## Error → User Message Mapping

Put this extension in `:core:common` or `:presentation`:

```kotlin
fun Throwable.toUserMessage(): String = when (this) {
    is DomainError.Network.NoConnection -> "No internet connection"
    is DomainError.Network.Timeout      -> "Request timed out. Try again."
    is DomainError.Auth.SessionExpired  -> "Session expired. Please sign in again."
    is DomainError.Commerce.PremiumRequired -> "This feature requires a subscription"
    is DomainError.Learning.NoDueCards  -> "No cards due for review"
    else -> message ?: "Something went wrong"
}
```

---

## Try<T> Rules (from core/common/Try.kt)

```kotlin
// SAFE — wrap code that can throw
val result: Try<Word> = Try { remoteDataSource.fetchWord(id) }

// SAFE — chain without losing cancellation safety
result.map { it.toDomain() }.flatMap { repo.save(it) }

// SAFE — fold at call site
result.fold(
    onSuccess = { word -> /* use word */ },
    onFailure = { e -> /* handle error */ }
)

// SAFE — recover to a default
result.recover { Try.Success(Word.empty()) }
```

### Critical: CancellationException is always re-thrown

`Try {}` catches `Throwable` but **re-throws `CancellationException` and `Error`** automatically. This means coroutine cancellation always works. Never catch `CancellationException` yourself inside a `Try` transform.

```kotlin
// BAD — breaks coroutine cancellation
val result = Try {
    try { remoteCall() }
    catch (e: CancellationException) { null }  // NEVER DO THIS
}

// GOOD — let Try handle it
val result = Try { remoteCall() }
```

---

## Anti-Patterns

```kotlin
// BAD — bare throw from suspend function (breaks caller contract)
override suspend fun syncWords(): Unit = remoteDataSource.fetchWords().let { local.save(it) }

// BAD — try-catch for control flow
suspend fun getWords(): List<Word> {
    return try { remote.fetchWords() } catch (e: Exception) { emptyList() }
}

// BAD — wrapping in runCatching unnecessarily (use Try<T> instead)
suspend fun getWords(): Result<List<Word>> = runCatching { remote.fetchWords() }

// BAD — swallowing errors silently
.catch { /* empty */ }

// BAD — Flow<Try<T>> — callers can't use .catch {} correctly
fun observeWords(): Flow<Try<List<Word>>>

// GOOD
fun observeWords(): Flow<List<Word>>  // errors via .catch {} in VM
```

---

## UiState<T> Reference

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String?) : UiState<Nothing>
    data object Empty : UiState<Nothing>
}

// In state
data class InsightState(
    val stats: UiState<StudyInsights> = UiState.Loading,
    val heatmap: UiState<List<HeatmapDay>> = UiState.Loading,
)

// In ViewModel
statsUseCase(Unit).reduce(
    onSuccess = { copy(stats = UiState.Success(it)) },
    onFailure = { copy(stats = UiState.Error(it.toUserMessage())) }
)
```

---

## Checklist

1. Data sources: throw raw exceptions — no wrapping
2. Repository suspend methods: always return `Try<T>`
3. Repository Flow methods: errors via `.catch {}` in VM — no `Flow<Try<T>>`
4. Use cases: thread `Try<T>` through — use `flatMap` for multi-step
5. ViewModels: `.reduce()` for Try, `.catch {}` for Flow
6. Never catch `CancellationException` inside a `Try` block
7. Use `DomainError` subtypes when UI needs to branch on error type
8. `UiState<T>` for async sections that have Loading/Success/Error/Empty states
9. Never use `!!` — use `requireNotNull(x) { "reason" }` or safe calls
