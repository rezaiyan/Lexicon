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

Add to `NavHost` block in `LexiconApp.kt`:

```kotlin
composable<FeatureDestination> {
    FeatureScreen(
        onNavigateBack = { navController.popBackStack() },
        onNavigateTo = { dest -> navController.navigate(dest) }
    )
}
```

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

## Checklist

1. Routes defined as `@Serializable` data classes/objects
2. Screen registered in NavHost in `LexiconApp.kt`
3. Navigation via callback lambdas — no `NavController` in screens
4. Dialogs/sheets via `OverlayHost` — not direct Material composables
5. Each overlay has a unique `tag` string
