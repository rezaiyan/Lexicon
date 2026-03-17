---
name: navigation-overlays
description: Handle navigation, dialogs, and bottom sheets using Lexicon's type-safe routes, OverlayHost pattern, and NavHost conventions
argument-hint: "<navigation-or-dialog-description>"
user-invocable: true
allowed-tools: ["Read", "Write", "Edit", "Glob", "Grep"]
---

# Lexicon Navigation & Overlays

Use this skill when adding navigation destinations, dialogs, or bottom sheets.

## Navigation

### Type-Safe Routes

```kotlin
@Serializable
object FeatureDestination  // no params

@Serializable
data class DetailDestination(val itemId: String)  // with params
```

### Tab Destinations

- `TabDestination.Study`
- `TabDestination.Profile`
- `TabDestination.Settings`

### Registering a Screen

Add to `NavHost` block in `NavigationGraph.kt` (or feature subgraph):

```kotlin
composable<FeatureDestination> {
    FeatureScreen(
        onNavigateBack = { navController.popBackStack() },
        onNavigateTo = { dest -> navController.navigate(dest) }
    )
}
```

### Feature Subgraphs (target structure)

Each feature module exports its own navigation subgraph:

```kotlin
// In :feature:study
fun NavGraphBuilder.studyGraph(navController: NavHostController) {
    navigation(startDestination = "study/review", route = "study") {
        composable("study/review") { ReviewScreen(...) }
        composable("study/progress") { ProgressScreen(...) }
    }
}

// In NavigationGraph.kt — assembles all subgraphs
NavHost(...) {
    authGraph(navController)
    studyGraph(navController)
    wordsGraph(navController)
    profileGraph(navController)
}
```

### Decomposed Navigation Files (target)

| File | Responsibility |
|---|---|
| `AppShell.kt` | Scaffold, bottom nav, snackbar host — pure layout |
| `NavigationGraph.kt` | Top-level NavHost, delegates to feature subgraphs |
| `AppFlowCoordinator.kt` | Auth gate, onboarding, splash -> ready transitions |
| `EffectHandler.kt` | Global snackbar + navigation side-effects |

### Rules

- Pass navigation callbacks as lambdas — **never pass `NavController` to screens**
- Use `launchSingleTop = true` for tab navigation
- Use `popUpTo(graph.findStartDestination().id)` to reset tab stacks

## Dialogs

Always use `OverlayHost` — never `AlertDialog` directly:

```kotlin
val overlayHost = LocalOverlayHost.current

overlayHost.showDialog(tag = "confirm") { nav ->
    LexiconDialogContent(
        title = "Title",
        message = "Message",
        confirmText = "OK",
        onConfirm = { nav.dismiss(); doAction() },
        onDismiss = nav::dismiss
    )
}
```

## Bottom Sheets

```kotlin
overlayHost.showFullscreenBottomSheet(
    tag = "detail",
    properties = LockedSheetProperties  // optional: prevent dismiss on outside tap
) { nav ->
    DetailSheetContent(onDismiss = { nav.dismiss() })
}
```

## Snackbar

```kotlin
val snackbarHostState = LocalSnackbarHostState.current

snackbarHostState.showSnackbar(
    message = "Done!",
    duration = SnackbarDuration.Short
)
```

Prefix with `[Error]` for error-styled snackbars.

## Animations

Existing utilities in `StudyAnimations.kt`:
- `Modifier.staggeredFadeSlide(index)` — staggered list entrance
- `rememberPulseScale(stopAfterMs)` — pulsing CTA effect
- `rememberAnimatedCounter(target, durationMs)` — number counting
- Screen transitions (fade + slide) already configured in NavHost

## Bottom Sheet & Dialog Visual Style

- Bottom sheets: rounded top corners `Theme.shapes.large` (16dp), content padding `Theme.spacing.md` (16dp)
- Dialog surfaces: `Theme.shapes.medium` (12dp) radius, subtle `Theme.elevation.modal` (12dp) shadow
- Scrim: black at 32% opacity — dark enough to focus attention, light enough to see context
- Sheet/dialog content follows same card-like internal padding and typography hierarchy
- Primary action button in sheets: pill shape, full width, at bottom with `Theme.spacing.md` (16dp) above

## Search Bar Pattern

For screens with search/filter:
```kotlin
// Pill-shaped search bar — prominent at top, collapses on scroll
OutlinedTextField(
    shape = RoundedCornerShape(Theme.shapes.pill),  // fully rounded
    colors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    ),
    modifier = Modifier.fillMaxWidth().height(48.dp),
)
```

## Filter Chips

Horizontal scrolling row for filter categories:
```kotlin
LazyRow(
    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    contentPadding = PaddingValues(horizontal = Theme.spacing.md),
) {
    items(filters) { filter ->
        FilterChip(
            selected = filter.isSelected,
            onClick = { onFilterToggle(filter) },
            label = { Text(filter.name) },
            shape = RoundedCornerShape(Theme.shapes.pill),  // pill-shaped chips
        )
    }
}
```

## Checklist

1. Routes defined as `@Serializable` data classes/objects
2. Screen registered in NavHost in `LexiconApp.kt`
3. Navigation via callback lambdas — no `NavController` in screens
4. Dialogs/sheets via `OverlayHost` — not direct Material composables
5. Each overlay has a unique `tag` string
