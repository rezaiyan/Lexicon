---
name: viewmodel-patterns
description: Create ViewModels following Lexicon's BaseViewModel<State, Effect> pattern with mutableStateOf, event sink methods, and Try<T>.reduce() integration
argument-hint: "<viewmodel-description>"
user-invocable: true
allowed-tools: ["Read", "Write", "Edit", "Glob", "Grep"]
agent: test-writer
---

# Lexicon ViewModel Patterns

Use this skill when creating or modifying ViewModels.

## BaseViewModel

All ViewModels extend `BaseViewModel<S, F>` — one atomic Compose state, one effect channel:

```kotlin
abstract class BaseViewModel<S, F> : ViewModel() {
    private val _state = mutableStateOf(initialState())

    @Composable
    fun state(): State<S> = _state  // Compose snapshot — auto-recompose on read

    private val _effects = Channel<F>(Channel.BUFFERED)
    val effects: Flow<F> = _effects.receiveAsFlow()

    abstract fun initialState(): S

    protected fun updateState(reducer: S.() -> S) {
        _state.value = _state.value.reducer()
    }

    protected fun emitEffect(effect: F) {
        viewModelScope.launch { _effects.send(effect) }
    }

    // Try<T> integration — fold result directly into state
    protected fun <T> Try<T>.reduce(
        onSuccess: S.(T) -> S,
        onFailure: S.(Throwable) -> S,
    ) {
        fold(
            onSuccess = { updateState { onSuccess(it) } },
            onFailure = { updateState { onFailure(it) } }
        )
    }
}
```

## ViewModel Structure (Event Sink Pattern)

Public methods ARE the event sink — no sealed Event class needed:

```kotlin
class StudyViewModel(
    private val getDueWordsUseCase: GetDueWordsUseCase,
    private val reviewWordUseCase: ReviewWordUseCase,
) : BaseViewModel<StudyState, StudyEffect>() {

    override fun initialState() = StudyState()

    init {
        loadWords()
    }

    // Event sink — public methods called directly from UI
    fun reviewWord(word: Word, quality: Int) {
        viewModelScope.launch {
            reviewWordUseCase(ReviewWordUseCase.Params(word, quality)).reduce(
                onSuccess = { copy(reviewedCount = reviewedCount + 1) },
                onFailure = { copy(error = it.message) }
            )
        }
    }

    fun deleteWord(wordId: Int) { /* ... */ }

    fun startReview() {
        emitEffect(StudyEffect.ShowReviewSheet)
    }

    private fun loadWords() {
        getDueWordsUseCase(Unit)
            .onEach { words -> updateState { copy(words = words, isLoading = false) } }
            .catch { e -> updateState { copy(error = e.message, isLoading = false) } }
            .launchIn(viewModelScope)
    }
}
```

## Screen State

One `data class` per screen — all fields in one atomic update:

```kotlin
@Stable
data class StudyState(
    val words: List<Word> = emptyList(),
    val reviewedCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
)
```

For screens with async sections, use `UiState<T>` within the state:

```kotlin
@Stable
data class SubscriptionScreenState(
    val offerings: UiState<SubscriptionOffering> = UiState.Loading,
    val customerInfo: SubscriptionCustomerInfo? = null,
    val isSubscribed: Boolean = false,
    val isPurchasing: Boolean = false,
)
```

## UiState<T> Sealed Interface

```kotlin
@Stable
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String?) : UiState<Nothing>
    data object Empty : UiState<Nothing>
}
```

## Effects

One-shot side effects (navigation, snackbar, etc.):

```kotlin
sealed interface StudyEffect {
    data class ShowReviewSheet(val word: Word) : StudyEffect
    data class ShowSnackbar(val message: String) : StudyEffect
    data object NavigateBack : StudyEffect
}
```

## Data Flow

```
UI -> vm::method -> ViewModel -> updateState -> mutableStateOf<S> -> snapshot read -> UI
                             \-> emitEffect() -> OnEvents -> Navigate / Snackbar
```

## Why Not MVI?

- No sealed `Event` boilerplate — direct method calls are simpler and more discoverable
- Method references (`vm::reviewWord`) are type-safe — compiler checks arity and types
- Content composables receive lambdas — trivially previewable with no ViewModel dependency
- `OnEvents` already exists in `:core` — just standardize its usage

## Error Handling Rules

- Use `.reduce()` for Try<T> results — folds success/failure into state atomically
- Use `.catch {}` on Flows — maps errors to state updates
- **Never** use try-catch for control flow
- **Never** use `!!`
- **Never** use unnecessary `runCatching`

## DI Registration

```kotlin
viewModelOf(::StudyViewModel)
```

## Checklist

1. Extends `BaseViewModel<State, Effect>`
2. Single `data class` state — no fragmented StateFlows
3. Public methods as event sink — no sealed Event class
4. `updateState { copy(...) }` for all state mutations
5. `emitEffect()` for one-shot side effects
6. `.reduce()` for Try<T> results
7. `.catch {}` for Flow errors — no try-catch
8. No `!!` anywhere
9. Use cases injected via constructor
10. Registered in AppModule.kt via `viewModelOf(::FeatureViewModel)`
