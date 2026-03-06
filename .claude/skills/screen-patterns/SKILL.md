---
name: screen-patterns
description: Create Compose Multiplatform screens following Lexicon's BaseViewModel event sink, Compose-native state(), OnEvents, LexiconColumn scaffold, and state hoisting conventions
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

    // Compose-native state — no collectAsState needed
    val state by viewModel.state()

    // One-shot effects via OnEvents
    OnEvents(viewModel.effects) { effect ->
        when (effect) {
            is FeatureEffect.NavigateTo -> onNavigateTo(effect.destination)
            is FeatureEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
        }
    }

    LexiconColumn(
        title = "Screen Title",
    ) {
        // Event sink — VM methods passed as lambdas
        FeatureContent(
            state = state,
            onAction = viewModel::doAction,
            onDelete = viewModel::deleteItem,
            onRetry = viewModel::retry,
        )
    }
}
```

## State Reading

Use `viewModel.state()` which returns Compose `State<S>` directly — **no `collectAsStateWithLifecycle()`** needed:

```kotlin
val state by viewModel.state()  // Compose snapshot — auto-recompose on change
```

## Content Composable (State Hoisting)

Split every screen into wrapper (state collection) and content (pure rendering):

```kotlin
@Composable
private fun FeatureContent(
    state: FeatureState,
    onAction: (Item) -> Unit,
    onDelete: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    if (state.isLoading) {
        LoadingScreen()
        return
    }

    state.error?.let { error ->
        ErrorScreen(message = error, onRetry = onRetry)
        return
    }

    // Render data
    LazyColumn {
        items(state.items, key = { it.id }) { item ->
            ItemRow(item = item, onClick = { onAction(item) })
        }
    }
}
```

## UiState<T> Handling (for async sections)

When state contains `UiState<T>` fields:

```kotlin
@Composable
private fun FeatureContent(state: FeatureScreenState, ...) {
    when (val offerings = state.offerings) {
        is UiState.Loading -> LoadingScreen()
        is UiState.Error -> ErrorScreen(message = offerings.message, onRetry = onRetry)
        is UiState.Empty -> EmptyScreen(title = "No items")
        is UiState.Success -> {
            OfferingsGrid(offerings = offerings.data, ...)
        }
    }
}
```

## OnEvents Pattern

Use `OnEvents` from `:core` for one-shot effects:

```kotlin
OnEvents(viewModel.effects) { effect ->
    when (effect) {
        is FeatureEffect.ShowReviewSheet -> {
            overlayHost.showFullscreenBottomSheet(tag = "review") { nav ->
                ReviewSheetContent(onDismiss = { nav.dismiss() })
            }
        }
        is FeatureEffect.ShowSnackbar -> {
            snackbarHostState.showSnackbar(effect.message)
        }
        is FeatureEffect.NavigateBack -> onNavigateBack()
    }
}
```

## Rules

- Get ViewModel via `koinViewModel<T>()` at the **top-level screen composable only**
- **Never pass ViewModel down** to child composables — extract state and pass data + callback lambdas
- Use `viewModel.state()` for Compose-native state — **not** `collectAsStateWithLifecycle()`
- Use `OnEvents` for one-shot effects — **not** `LaunchedEffect` with Flow collection
- Wrap content in `LexiconColumn` — never raw `Column` + `Scaffold`
- Pass VM methods as references: `viewModel::doAction` — these are the event sink
- Content composables are trivially previewable: they take data + lambdas, no ViewModel

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
3. State read via `viewModel.state()` — Compose-native, no Flow collection
4. Effects handled via `OnEvents(viewModel.effects)`
5. VM methods passed as references for event sink
6. Content wrapped in `LexiconColumn`
7. Content composable is pure: takes data + lambdas, no ViewModel
8. Screen registered in NavHost
9. ViewModel registered in AppModule.kt via `viewModelOf(::FeatureViewModel)`
