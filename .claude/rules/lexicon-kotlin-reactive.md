---
description: Kotlin reactive programming rules — Flow, Coroutines, StateFlow for Lexicon KMP codebase
---

# Kotlin Reactive Programming

**Stack:** `kotlinx.coroutines`, `kotlinx.coroutines.flow`, `viewModelScope`, Turbine (testing)

---

## Coroutines

### Scope Rules

| Scope | Use When |
|-------|----------|
| `viewModelScope` | ViewModel-launched work (auto-cancels on clear) |
| `CoroutineScope(SupervisorJob())` | Service/singleton that outlives screens |
| `coroutineScope {}` | Structured concurrency inside suspend functions |
| `runTest {}` | Tests only — never in production |

**NEVER use `GlobalScope`.** It leaks coroutines and bypasses cancellation.

### Dispatcher Rules

```kotlin
// ✅ Inject dispatcher for testability
class MyRepository(private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    suspend fun load(): Try<Data> = withContext(dispatcher) { ... }
}

// ❌ Hard-coded dispatcher — untestable
suspend fun load() = withContext(Dispatchers.IO) { ... }
```

Use `Dispatchers.IO` for network/DB, `Dispatchers.Default` for CPU work. Compose/ViewModel code runs on `Main` by default — no need to specify.

### Error Handling

**NEVER `try-catch` for control flow.** Use `Try<T>` (domain layer) or Flow operators.

```kotlin
// ✅ Try<T> in suspend functions
suspend fun fetchWords(): Try<List<Word>> = Try {
    dataSource.getWords()
}

// ✅ Flow error handling
repository.observeWords()
    .catch { emit(Try.Failure(DomainError.from(it))) }
    .collect { ... }

// ❌ try-catch for control flow
try {
    val result = fetchWords()
} catch (e: Exception) { ... }
```

---

## Flow

### Cold vs Hot

| Type | API | Use For |
|------|-----|---------|
| **Cold** (per-collector) | `flow { }`, `channelFlow { }` | One-shot requests, DB queries |
| **Hot — state** | `StateFlow` | Current UI state, shared observable state |
| **Hot — events** | `SharedFlow` | One-time effects, fire-and-forget events |

### Operators — Prefer Declarative Chains

```kotlin
// ✅ Declarative chain — easy to read and test
repository.observeWords()
    .filter { it.isNotEmpty() }
    .map { it.sortedBy(Word::value) }
    .flowOn(Dispatchers.Default)
    .catch { emit(emptyList()) }
    .collect { updateState { copy(words = it) } }

// ❌ Imperative collect-and-do — hides intent
repository.observeWords().collect { list ->
    val filtered = list.filter { it.isNotEmpty() }
    val sorted = filtered.sortedBy { it.value }
    updateState { copy(words = sorted) }
}
```

**`flowOn` placement:** put it BEFORE collection, AFTER the transform that needs the dispatcher.

### Thread Safety

```kotlin
// ✅ StateFlow — safe for concurrent updates
private val _state = MutableStateFlow(initialState)
val state: StateFlow<State> = _state.asStateFlow()

// ✅ Atomicity via update {}
_state.update { current -> current.copy(loading = true) }

// ❌ Manual assignment — race condition
_state.value = _state.value.copy(loading = true)
```

### SharedFlow for Effects (One-Time Events)

```kotlin
private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 1)
val effects: SharedFlow<Effect> = _effects.asSharedFlow()

fun doAction() {
    viewModelScope.launch {
        _effects.emit(Effect.ShowSuccess)
    }
}
```

Use `replay = 0` (default) so late subscribers don't receive stale events.

---

## StateFlow in ViewModels

**In Lexicon, UI state is Compose-native `mutableStateOf` via `BaseViewModel`.** Do NOT use `collectAsStateWithLifecycle()` — use `viewModel.state()`.

```kotlin
// ✅ Lexicon pattern — state() extension in BaseViewModel
@Composable
fun MyScreen(viewModel: MyViewModel = koinViewModel()) {
    val state = viewModel.state()   // returns Compose State<S>
    OnEvents(viewModel.effects) { effect -> ... }
}

// ❌ Never in Lexicon
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

When collecting an external `Flow` into BaseViewModel state:

```kotlin
init {
    viewModelScope.launch {
        repository.observeData()
            .catch { handleError(it) }
            .collect { data -> updateState { copy(data = data) } }
    }
}
```

---

## Cancellation

- **Always check cancellation** in long loops: `ensureActive()` or `yield()`
- **`withTimeout`** for bounded operations — throws `TimeoutCancellationException` (a `CancellationException`, re-thrown automatically)
- **Don't catch `CancellationException`** — it signals structured cancellation

```kotlin
// ✅ Timeout + propagation
suspend fun fetchWithTimeout(): Try<Data> = Try {
    withTimeout(5_000) { dataSource.fetch() }
}
```

---

## Anti-Patterns

| ❌ Never | ✅ Instead |
|---------|-----------|
| `runBlocking` in production | `suspend fun` + proper scope |
| `GlobalScope.launch` | `viewModelScope.launch` |
| Nested `collect {}` | Compose operators (`flatMapLatest`, `combine`) |
| `!!` on nullable Flow values | Elvis + safe-call chain |
| Catching `CancellationException` | Let it propagate |
| `flow.value` access outside UI thread | `update {}` for mutation |
