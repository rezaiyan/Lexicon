---
name: viewmodel-patterns
description: Create ViewModels following Lexicon's StateFlow/Channel pattern, UiState wrapper, Flow error handling, and Koin DI conventions
argument-hint: "<viewmodel-description>"
user-invocable: true
allowed-tools: ["Read", "Write", "Edit", "Glob", "Grep"]
agent: test-writer
---

# Lexicon ViewModel Patterns

Use this skill when creating or modifying ViewModels.

## ViewModel Structure

```kotlin
class FeatureViewModel(
    private val getDataUseCase: GetDataUseCase,
    private val updateDataUseCase: UpdateDataUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<FeatureState>>(UiState.Loading)
    val state: StateFlow<UiState<FeatureState>> = _state.asStateFlow()

    // One-shot events via Channel
    private val _events = Channel<FeatureEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadData()
    }

    private fun loadData() {
        getDataUseCase()
            .onEach { data -> _state.value = UiState.Loaded(FeatureState(data)) }
            .catch { e -> _state.value = UiState.Error(e.message ?: "Unknown error") }
            .launchIn(viewModelScope)
    }
}
```

## UiState Sealed Interface

```kotlin
@Stable
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>
    data class Loaded<T>(val value: T) : UiState<T>
}
```

Extension functions available: `.isLoading()`, `.isError()`, `.isLoaded()`, `.onLoading {}`, `.onError {}`, `.onLoaded {}`

## Rules

- Expose `StateFlow` (not `MutableStateFlow`) publicly via `.asStateFlow()`
- Use `SharingStarted.WhileSubscribed(5000)` for derived state via `.stateIn()`
- Use `Channel<Event>` for one-shot events (navigation, snackbar) — not SharedFlow
- No `!!` — handle nullability explicitly
- No try-catch — use Flow `.catch {}` operator for error handling
- No unnecessary `runCatching` — prefer direct Flow-based error handling
- Use cases injected via constructor — never created inside ViewModel
- State data classes should be `@Stable` or `@Immutable`

## Event Pattern

```kotlin
sealed class FeatureEvent {
    data class NavigateTo(val destination: String) : FeatureEvent()
    data object ShowSuccess : FeatureEvent()
}
```

## DI Registration

Register in `composeApp/src/commonMain/kotlin/di/AppModule.kt`:

```kotlin
viewModelOf(::FeatureViewModel)
```

## Checklist

1. Public `StateFlow` exposed via `.asStateFlow()`
2. Events via `Channel<Event>(Channel.BUFFERED)` + `.receiveAsFlow()`
3. Flow errors handled with `.catch {}` — no try-catch
4. Use cases injected via constructor
5. No `!!` anywhere
6. Registered in `AppModule.kt`
