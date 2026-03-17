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

## Layout Rhythm

Apply consistent spacing rhythm for a calm, scannable UI:

```kotlin
LexiconColumn(title = "Screen Title") {
    // Section 1 — hero or summary
    HeroCard(...)

    Spacer(Modifier.height(Theme.spacing.lg))  // 24dp between sections

    // Section 2 — content list
    SectionHeader(title = "Recent")
    Spacer(Modifier.height(Theme.spacing.sm))  // 12dp heading → content
    items.forEach { item ->
        ItemCard(item)
        Spacer(Modifier.height(Theme.spacing.md))  // 16dp between cards
    }
}
```

Key spacing rules:
- **Screen horizontal margins**: handled by `LexiconColumn` — `Theme.spacing.md` (16dp)
- **Between content sections**: `Theme.spacing.lg` (24dp) — major visual breaks
- **Between related items** (cards in a list): `Theme.spacing.md` (16dp)
- **Between heading and its content**: `Theme.spacing.xs` (8dp) to `Theme.spacing.sm` (12dp)
- **Card internal padding**: `Theme.spacing.md` (16dp)
- **Bottom clearance from nav bar**: `Theme.spacing.lg` (24dp) minimum

### Content Organization

- Group related content into **cards** — cards provide visual containment and scanability
- **One primary CTA per screen** — use accent color sparingly for maximum attention direction
- Empty space is intentional — resist filling every pixel
- Use **surface hierarchy**: base background → card surface → raised elements → overlays
- Typography hierarchy through **weight and color** before size changes — max 3 font sizes per section

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

## Visual Patterns

### Stat / Summary Cards

For data display (analytics, progress, profile stats):

```kotlin
Card(
    shape = RoundedCornerShape(Theme.shapes.medium),
    elevation = CardDefaults.cardElevation(defaultElevation = Theme.elevation.low),
) {
    Column(Modifier.padding(Theme.spacing.md)) {
        Text("Cards Reviewed", style = MaterialTheme.typography.labelMedium, color = onSurfaceVariant)
        Spacer(Modifier.height(Theme.spacing.xxs))
        Text("2,847", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(Theme.spacing.xxs))
        Text("23% vs last month", style = MaterialTheme.typography.bodySmall, color = secondary)
    }
}
```

### Press Feedback

Add tactile press feedback to interactive cards:

```kotlin
val interactionSource = remember { MutableInteractionSource() }
val isPressed by interactionSource.collectIsPressedAsState()
val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.97f else 1f,
    animationSpec = spring(stiffness = Spring.StiffnessMediumHigh),
    label = "press-scale"
)

Card(
    modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
    interactionSource = interactionSource,
    onClick = { ... }
)
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
