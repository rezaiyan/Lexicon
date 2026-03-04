---
name: screen-patterns
description: Create Compose Multiplatform screens following Lexicon's screen structure, LexiconColumn scaffold, UiState handling, and state hoisting conventions
argument-hint: "<screen-description>"
user-invocable: true
allowed-tools: ["Read", "Write", "Edit", "Glob", "Grep"]
agent: test-writer
---

# Lexicon Screen Patterns

Use this skill when creating or modifying Compose screens.

## Screen Structure

Every screen follows this exact structure:

```kotlin
@Composable
fun FeatureScreen(
    onNavigateTo: (Destination) -> Unit = {},  // navigation callbacks
) {
    val viewModel = koinViewModel<FeatureViewModel>()
    val overlayHost = LocalOverlayHost.current
    val snackbarHostState = LocalSnackbarHostState.current

    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LexiconColumn(
        title = "Screen Title",
        // actionIcon1 = ActionIconConfig(...),
    ) {
        when (uiState) {
            is UiState.Loading -> LoadingScreen()
            is UiState.Error -> ErrorScreen(
                message = (uiState as UiState.Error).message,
                onRetry = { viewModel.retry() }
            )
            is UiState.Loaded -> {
                val data = (uiState as UiState.Loaded).value
                FeatureContent(data = data, ...)
            }
        }
    }
}
```

## Rules

- Get ViewModel via `koinViewModel<T>()` at the **top-level screen composable only**
- **Never pass ViewModel down** to child composables — extract state and pass data + callback lambdas instead
- Collect state with `collectAsStateWithLifecycle()` — never `collectAsState()`
- Wrap content in `LexiconColumn` (from LexiconScaffold.kt) — never raw `Column` + `Scaffold`
- Use `UiState<T>` sealed interface for all async data (Loading/Error/Loaded)
- Split large screens: public wrapper (state collection + ViewModel) → private content composable (receives data + lambdas, no ViewModel reference)

## LexiconColumn API

```kotlin
@Composable
fun LexiconColumn(
    title: String? = null,
    showNavigationIcon: Boolean = false,
    actionIcon1: ActionIconConfig? = null,
    actionIcon2: ActionIconConfig? = null,
    scrollable: Boolean = true,
    scrollState: ScrollState? = null,
    topBarColor: TopBarColor = TopBarColor.Background,
    collapsedContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
)
```

## Status Screens (from design-system)

- `LoadingScreen(modifier, message?)` — centered spinner
- `ErrorScreen(message, title?, icon?, onRetry?)` — full error with retry
- `EmptyScreen(title, subtitle?, icon?)` — empty state

## Checklist

1. ViewModel obtained via `koinViewModel` at top level only
2. ViewModel never passed to child composables
3. State collected with `collectAsStateWithLifecycle()`
4. Content wrapped in `LexiconColumn`
5. All three `UiState` branches handled (Loading/Error/Loaded)
6. Screen registered in NavHost inside `LexiconApp.kt`
7. ViewModel registered in `AppModule.kt` via `viewModelOf(::FeatureViewModel)`
