---
description: Compose Multiplatform performance rules — recomposition guards, remember, stable types, derivedStateOf for Lexicon KMP
---

# Compose Multiplatform Performance

**Goal:** Minimize recompositions. Every unnecessary recomposition wastes battery and causes jank.

---

## Recomposition Basics

A composable recomposes when its inputs change. Inputs = parameters + read `State<T>`. **Control what changes.**

```kotlin
// ✅ Pass only what the composable needs — stable, minimal surface
@Composable
fun WordCard(value: String, translation: String, onClick: () -> Unit)

// ❌ Pass the whole object — any Word field change triggers recomposition
@Composable
fun WordCard(word: Word, onClick: () -> Unit)
```

---

## Stability — The Root Cause of Unexpected Recompositions

Compose skips recomposition only for **stable** types. Unstable types = always recompose.

**Stable by default:** primitives, `String`, `@Immutable` / `@Stable` annotated classes, Kotlin `data class` with only stable fields.

**Unstable by default:** `List<T>`, `Map<K,V>`, `Set<T>`, classes from external libraries.

```kotlin
// ✅ Use kotlinx.collections.immutable for stable collections
@Composable
fun WordList(words: ImmutableList<Word>) { ... }

// ❌ List<T> is unstable — recomposes on every parent recomposition
@Composable
fun WordList(words: List<Word>) { ... }
```

```kotlin
// ✅ Mark domain models stable when all fields are stable
@Immutable
data class Word(val id: WordId, val value: String, val translation: String, val level: Int)

// ✅ Or mark the composable parameter stable explicitly
@Stable
data class WordUiState(val words: ImmutableList<Word>, val isLoading: Boolean)
```

---

## `remember` — Cache Expensive Work

```kotlin
// ✅ Cache derived value — only recalculates when words changes
@Composable
fun WordStats(words: ImmutableList<Word>) {
    val masteredCount = remember(words) { words.count { it.level == 6 } }
    Text("Mastered: $masteredCount")
}

// ❌ Recalculates on every recomposition
@Composable
fun WordStats(words: ImmutableList<Word>) {
    val masteredCount = words.count { it.level == 6 }
    Text("Mastered: $masteredCount")
}
```

**Rules:**
- `remember { }` — no key, cached for the composable's lifetime
- `remember(key) { }` — recalculate when `key` changes
- `rememberUpdatedRef` — for callbacks that capture mutable state (avoid stale closures)

---

## `derivedStateOf` — Derive State from Other State

Use when computed value should only trigger recomposition when the **result** changes, not every time the source state changes.

```kotlin
// ✅ Only recomposes when hasDueWords result changes (true→false or vice versa)
@Composable
fun StudyButton(words: ImmutableList<Word>) {
    val hasDueWords by remember {
        derivedStateOf { words.any { it.level < 6 } }
    }
    if (hasDueWords) StudyButton()
}

// ❌ Recomposes every time words list changes, even if hasDueWords didn't change
@Composable
fun StudyButton(words: ImmutableList<Word>) {
    if (words.any { it.level < 6 }) StudyButton()
}
```

**Rule:** Use `derivedStateOf` when the source changes frequently but the derived result changes rarely.

---

## `key()` — Stable Identity in Lists

```kotlin
// ✅ Stable key — Compose reuses existing composables on reorder/insert
LazyColumn {
    items(words, key = { it.id.value }) { word ->
        WordCard(word.value, word.translation)
    }
}

// ❌ No key — full list recomposition on any change
LazyColumn {
    items(words) { word -> WordCard(word.value, word.translation) }
}
```

Always provide `key` for `items()`, `itemsIndexed()`, and animated lists.

---

## Lambda Stability — Avoid Recompositions from Callbacks

```kotlin
// ✅ Stable lambda via event sink (Lexicon pattern) — same reference, no recomposition
WordCard(
    value = word.value,
    onDelete = viewModel::deleteWord  // method reference — stable
)

// ❌ Lambda literal — new instance every recomposition
WordCard(
    value = word.value,
    onDelete = { viewModel.deleteWord(word.id) }  // new lambda each time
)
```

When a lambda captures a mutable variable, wrap with `rememberUpdatedState`:

```kotlin
val currentOnAction by rememberUpdatedState(onAction)
val stableCallback = remember { { currentOnAction() } }
```

---

## Heavy Work Off the Main Thread

```kotlin
// ✅ Move filtering/sorting to ViewModel — not inside composables
class WordListViewModel(...) : BaseViewModel<...>() {
    init {
        viewModelScope.launch {
            repository.observeWords()
                .map { it.sortedBy(Word::value) }      // off main thread
                .flowOn(Dispatchers.Default)
                .collect { updateState { copy(words = it.toImmutableList()) } }
        }
    }
}

// ❌ Sorting inside composable — runs on every recomposition on main thread
@Composable
fun WordList(words: List<Word>) {
    val sorted = words.sortedBy { it.value }
    ...
}
```

---

## Anti-Patterns

| ❌ Never | ✅ Instead |
|---------|-----------|
| `List<T>` params | `ImmutableList<T>` (kotlinx.collections.immutable) |
| Computation in composable body | `remember(key) { }` or ViewModel |
| Lambda literals in params | Method references or `remember { }` |
| Missing `key` in `LazyColumn` items | `items(list, key = { it.id })` |
| Reading whole state object | Pass only the fields the composable needs |
| `derivedStateOf` without `remember` | Always wrap: `remember { derivedStateOf { } }` |
