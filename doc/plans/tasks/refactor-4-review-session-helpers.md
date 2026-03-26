# REFACTOR-4 — Extract Session Helpers from ReviewViewModel

**Priority:** P2
**Status:** Open
**Wave:** 4 (high risk area — analytics boundary must be respected)

## Problem

`ReviewViewModel` contains two private helpers that implement domain logic but have no tests
because they are hidden inside the ViewModel:

1. **`buildSessionId()`** — generates a session ID using a timestamp + random 6-digit suffix.
   Generating identifiers is domain logic, not UI logic.

2. **`resolveLanguageCode()`** — determines the language code for a reviewed card by:
   - Returning `languageCode` immediately if non-blank (fast path)
   - Otherwise frequency-voting across the current word list to detect which side of the
     flashcard was shown (target vs. native)
   This is non-trivial domain reasoning that is currently untestable.

**Files with the problem:**
- `feature/study/src/commonMain/kotlin/feature/study/ReviewViewModel.kt`
  - `buildSessionId()` ~lines 323–325
  - `resolveLanguageCode()` ~lines 347–357

## Boundary — Do Not Touch

`critical-risks.md §2` documents live invariants in `ReviewViewModel`'s analytics flow.
The following must **not** change as part of this task:

- `buildEventParams()` — analytics event parameter construction
- `beginAnalyticsSession()` / `endAnalyticsSession()` — fire-and-forget session lifecycle
- `sessionContext` and `sessionContext.withReview()` — in-memory session accumulation
- The `reviewWord()` method body beyond replacing `buildSessionId()` and `resolveLanguageCode()` call sites

## Files to Create

### Domain use cases
```
domain/src/commonMain/kotlin/domain/study/usecase/GenerateSessionIdUseCase.kt
```
```kotlin
class GenerateSessionIdUseCase {
    operator fun invoke(): String
}
```
Produces `"${timestamp}-${randomSuffix}"`.
Accepts an optional `random: Random` parameter for test injection.

```
domain/src/commonMain/kotlin/domain/study/usecase/ResolveCardLanguageUseCase.kt
```
```kotlin
class ResolveCardLanguageUseCase {
    operator fun invoke(
        text: String,
        explicitCode: String,
        words: List<ReviewWord>,
    ): String
}
```
Logic:
- If `explicitCode.isNotBlank()` → return `explicitCode`
- Otherwise: check whether `text` appears as `originalWord` in any `words` entry.
  Frequency-vote across all words' target language codes; return the majority code.
  Fallback to `explicitCode` (empty string) if `words` is empty or tie.

### Tests
```
domain/src/commonTest/kotlin/domain/study/GenerateSessionIdUseCaseTest.kt
domain/src/commonTest/kotlin/domain/study/ResolveCardLanguageUseCaseTest.kt
feature/study/src/commonTest/kotlin/feature/study/ReviewViewModelTest.kt
```

## Files to Modify

```
feature/study/.../ReviewViewModel.kt
```
- Inject `GenerateSessionIdUseCase` and `ResolveCardLanguageUseCase` via constructor
- `buildSessionId()` — delete; replace call site in `startSession()` with `generateSessionIdUseCase()`
- `resolveLanguageCode()` — delete; replace call site in `buildEventParams()` with
  `resolveCardLanguageUseCase(text, languageCode, currentWords)`
- No other changes to `reviewWord()`, `buildEventParams()`, or analytics methods

```
composeApp/src/commonMain/kotlin/di/AppModule.kt
```
Add:
```kotlin
factoryOf(::GenerateSessionIdUseCase)
factoryOf(::ResolveCardLanguageUseCase)
```

## Test Cases

### `GenerateSessionIdUseCaseTest`
- Returns a non-blank string
- Format matches `"<digits>-<digits>"` pattern
- Two consecutive calls return different values (uniqueness)
- With injected `Random(seed = 42)` → deterministic output (for snapshot tests)

### `ResolveCardLanguageUseCaseTest`
- `explicitCode = "de"` → returns `"de"` immediately (fast path, no word list needed)
- `explicitCode = ""`, text matches `originalWord` of a Spanish word → returns `"es"`
- `explicitCode = ""`, text does NOT match any `originalWord` → returns the native language code
- `explicitCode = ""`, empty word list → returns `""`
- Majority vote: 3 words with `"es"`, 1 with `"de"` → returns `"es"`
- Tie (2 vs 2): deterministic fallback (first or explicit empty)

### `ReviewViewModelTest` (safe subset only)
- `startSession()` → `GenerateSessionIdUseCase` called once
- `speakWord()` with explicit language code → `ResolveCardLanguageUseCase` called with that code
- Analytics session lifecycle methods are NOT asserted in this task

## Acceptance Criteria

- [ ] `buildSessionId()` deleted from `ReviewViewModel`
- [ ] `resolveLanguageCode()` deleted from `ReviewViewModel`
- [ ] `buildEventParams()`, `beginAnalyticsSession()`, `endAnalyticsSession()` unchanged
- [ ] `reviewWord()` body unchanged beyond call-site replacements
- [ ] Both use cases fully tested including edge cases
- [ ] `ReviewViewModelTest` uses fakes for both new use cases
- [ ] `./gradlew composeApp:compileKotlinMetadata` passes
- [ ] `./gradlew composeApp:cleanAllTests composeApp:allTests` passes
