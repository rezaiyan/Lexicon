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

## Sheets & Prompts

**Every prompt is a bottom sheet.** No dialogs: `AlertDialog` / `BasicAlertDialog` imports are banned by detekt
(`ForbiddenImport`), and `showDialog` / `LexiconDialogContent` were removed. Build content from the sheet kit in
`design-system/src/commonMain/kotlin/components/sheet/` (see the `design-system` skill for the component list).

### Confirm / destructive prompt

```kotlin
overlayHost.showSizeToFitBottomSheet(tag = "delete-tag") { nav ->
    ConfirmSheetContent(
        icon = Icons.Default.DeleteOutline,
        title = stringResource(Res.string.delete_tag),
        message = stringResource(Res.string.delete_tag_message, name),
        confirmText = stringResource(Res.string.delete),
        onConfirm = { nav.dismiss(); viewModel.deleteTag(id) },
        dismissText = stringResource(Res.string.cancel),
        onDismiss = { nav.dismiss() },
        tone = ConfirmTone.Danger,          // Brand for non-destructive
        onClose = { nav.dismiss() },        // standalone sheet -> show X
    )
}
```

`ConfirmDialog(...)` (same parameters) is the only centered-dialog escape hatch — use it only where a sheet
is already open underneath and a nested sheet would stack (e.g. discard-changes inside a locked flow).

### Single page (form / picker)

```kotlin
overlayHost.showSizeToFitBottomSheet(tag = "rename-tag") { nav ->
    SheetPage(
        title = stringResource(Res.string.rename_tag),
        onClose = { nav.dismiss() },
        footer = {
            SheetFooterRow(
                secondary = { SheetTonalButton(text = cancel, onClick = { nav.dismiss() }, modifier = it) },
                primary = { SheetPrimaryButton(text = save, onClick = ..., enabled = canSave, modifier = it) },
            )
        },
    ) {
        SheetField(label = ..., value = name, onValueChange = { name = it })
    }
}
```

Single-choice pickers (theme, daily goal, language) **apply on tap and dismiss** — no Save footer.

### Multi-page flows — `BottomSheetPages`

Sub-steps (pickers, edit, confirm) are **pages in the same sheet**, never a second stacked sheet.
Never call `showSizeToFitBottomSheet` from inside sheet content.

```kotlin
private sealed interface DetailPage {
    data object Detail : DetailPage
    data object Edit : DetailPage
    data object ConfirmDelete : DetailPage
}

overlayHost.showSizeToFitBottomSheet(tag = "word-detail") { sheetNav ->
    val pages = rememberBottomSheetPageNavigator<DetailPage>(DetailPage.Detail)
    BottomSheetPages(navigator = pages, onClose = { sheetNav.dismiss() }, label = "wordDetailPages") { page ->
        when (page) {
            DetailPage.Detail -> WordDetailSheetContent(onEdit = { pages.navigateTo(DetailPage.Edit) }, ...)
            DetailPage.Edit -> EditWordContent(onDismiss = { pages.navigateBack() }, ...)
            DetailPage.ConfirmDelete -> DeleteConfirmationContent(onDismiss = { pages.navigateBack() }, ...)
        }
    }
}
```

- The pager draws the toolbar (back when stack > 1, close when `onClose` is passed) — pages inside it leave
  `SheetPage(onBack, onClose)` **null**. Standalone sheets pass `onClose`.
- System back pops a page before dismissing the sheet. A page that needs custom back (e.g. discard prompt)
  composes its own `expects.BackHandler` — it outranks the pager's handler.
- Flows holding unsaved input use `LockedSheetProperties` (no drag-to-dismiss); everything else keeps the handle.

### Fullscreen sheets

```kotlin
overlayHost.showFullscreenBottomSheet(tag = "detail", properties = LockedSheetProperties) { nav ->
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

## Bottom Sheet Visual Style

- Rounded top corners `Theme.shapes.large`; `SheetPage` owns content padding, title hierarchy and sticky footer
- Scrim: black at 32% opacity
- Footer: `SheetPrimaryButton` (pill, full width) alone, or `SheetFooterRow` (tonal/destructive secondary + primary)
- Never hand-roll sheet headers, footers, list rows or text fields — use the kit

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
4. Prompts are sheets via `OverlayHost` built from `components.sheet` — no dialogs, no raw `ModalBottomSheet`
6. Sub-steps are `BottomSheetPages` pages, never nested sheets
5. Each overlay has a unique `tag` string
