---
name: recomposition
description: Apply Compose recomposition best practices — deferred reads, derivedStateOf, stable types, remember, key(), and avoiding unnecessary allocations
user-invocable: false
allowed-tools: ["Read", "Edit", "Glob", "Grep"]
---

# Recomposition Best Practices

This skill is automatically loaded by agents working on Compose UI. Apply these rules to all composable code.

## Defer State Reads

Use lambda-based modifiers so Compose only re-reads during layout/draw, not recomposition:

```kotlin
// Good — deferred read
Modifier.offset { IntOffset(offsetX.value, 0) }

// Bad — triggers recomposition on every change
Modifier.offset(offsetX.value.dp, 0.dp)
```

## derivedStateOf

Use for computed values that shouldn't trigger recomposition on every source change:

```kotlin
val isScrolled by remember { derivedStateOf { scrollState.value > threshold } }
```

## remember

Always `remember` expensive computations. Use keys when the result depends on inputs:

```kotlin
val formatted = remember(data) { expensiveFormat(data) }
```

## Stable Types

Annotate types so Compose can skip recomposition:

- **`@Stable`** — for state data classes (values replaced via `copy()`, but may contain collections). `UiState` is already `@Stable`.
- **`@Immutable`** — for deeply immutable value objects where all properties are primitives or other `@Immutable` types.

```kotlin
// State classes — use @Stable (contains List which isn't deeply immutable)
@Stable
data class FeatureState(val items: List<Item>, val title: String)

// Value objects — use @Immutable (all primitives/strings)
@Immutable
data class ThemeColors(val primary: Color, val secondary: Color)
```

## Avoid Allocations in Composition

Do not create new lambdas, lists, or objects in composable body without `remember` — especially in `LazyColumn` items:

```kotlin
// Good
val onClick = remember(item.id) { { viewModel.onAction(item.id) } }

// Bad — new lambda on every recomposition
Button(onClick = { viewModel.onAction(item.id) })
```

## key() for Lists

```kotlin
// LazyColumn
LazyColumn {
    items(list, key = { it.id }) { item -> ... }
}

// forEach
list.forEach { item ->
    key(item.id) { ItemRow(item) }
}
```

## Side Effects

Never call suspend functions or mutate state directly in composable body:

```kotlin
// Good
LaunchedEffect(id) { viewModel.load(id) }
DisposableEffect(lifecycle) { onDispose { cleanup() } }

// Bad — runs on every recomposition
viewModel.load(id)
```
