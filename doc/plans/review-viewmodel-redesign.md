# ReviewViewModel — Ideal Redesign

> Senior design audit. Every violation is mapped to a specific SOLID principle with the exact
> lines that break it. The ideal design follows the audit.

---

## 1. SOLID Violations in the Current Code

### S — Single Responsibility Principle

A class should have one reason to change. `ReviewViewModel` has at least **ten**:

| Responsibility | Location |
|---|---|
| Word loading (5 strategies) | `startReview`, `startDueReview`, `startStageReview`, `startTagReview`, `startStageTagReview` |
| SRS level computation | `computeNewLevel(previousLevel, quality, settings)` |
| Session lifecycle | `session.begin(...)`, `session.end(...)` via `ReviewSessionManager` |
| Session counter mutation | `session.correctCardCount++`, `session.reviewedCardCount++` |
| Analytics orchestration | 6 direct `analyticsTracker.*` calls scattered across methods |
| TTS playback | `speakWord`, `setTtsSpeechRate` |
| Word CRUD in-session | `updateWord`, `deleteWord` with in-place list patching |
| Streak recording | `sessionUseCases.recordStreak(count)` |
| Language code resolution | `resolveLanguageCode(text, languageCode, state)` |
| Session ID generation | inside `ReviewSessionManager.begin()` |

`ReviewSessionManager` also violates SRP on its own. It is responsible for:
1. Holding mutable session state (ID, counters, start time)
2. Fetching review settings (`sessionUseCases.getSettings(Unit)`)
3. Calling analytics at session end (`analyticsTracker.logReviewSessionComplete(...)`)
4. Generating the session ID

Four unrelated reasons to change inside one private class.

**Each of these responsibilities should be its own unit.** A change to the SRS algorithm, the analytics schema, the TTS API, or the streak contract all force modifications to the same file.

---

### O — Open/Closed Principle

Software entities should be open for extension, closed for modification.

**Violation 1 — New card source = new method in the ViewModel.**

Every time a new way to load cards is needed (e.g., "by difficulty", "favorites", "recently added"), a developer must open `ReviewViewModel` and add a new `start*` method. The ViewModel is closed for extension and open for modification.

```kotlin
// You can't add a new source without editing this file:
fun startDueReview()               // open ReviewViewModel.kt, add code
fun startStageReview(stage: ...)   // open ReviewViewModel.kt, add code
fun startTagReview(tagId: ...)     // open ReviewViewModel.kt, add code
fun startStageTagReview(stage, tagId)  // open ReviewViewModel.kt, add code
```

**Violation 2 — SRS change requires modifying two files.**

`computeNewLevel` in the ViewModel is a reduced re-implementation of `ReviewWordUseCase`. If the SRS algorithm changes, you must modify both `ReviewWordUseCase` and `ReviewViewModel`. Two files must change for one behavior.

```kotlin
// ReviewWordUseCase.kt — the real algorithm
newLevel = min(6, word.level + 1)

// ReviewViewModel.kt — a different, incomplete copy
settings.successesToAdvance <= 1 && previousLevel < 6 -> minOf(6, previousLevel + 1)
```

**Violation 3 — New effects require modifying `ReviewEffect` AND `ReviewViewModel`.**

Because `ReviewWordUseCase` returns `Try<Unit>`, all outcome logic must live in the ViewModel. Adding "show mastery animation when word reaches level 6" requires opening the ViewModel to add the check. With a rich `ReviewWordResult` return type, this is a zero-ViewModel-change extension.

---

### L — Liskov Substitution Principle

Subtypes must be substitutable for their base types without changing program correctness. In a broader sense: every method's behavior must match the contract implied by its name and signature.

**Violation 1 — `startReview()` and `startDueReview()` are false synonyms.**

Both names imply "start a review session with due words." They have different contracts:

```kotlin
fun startReview() {
    // Peeks at one word, emits an effect — NEVER starts a session
    .firstOrNull()?.let { emitEffect(ReviewEffect.StartReview(it)) }
}

fun startDueReview() {
    // Actually begins a session and loads the full queue
    session.begin("REVIEW")
    ...
}
```

A caller who substitutes one for the other will get silent wrong behavior. Neither name signals this distinction. This is a naming contract violation — the signature says "start review" but the behavior is split into "check if review is possible" vs "actually start review."

**Violation 2 — `ReviewWordUseCase` returns `Try<Unit>` but callers need more.**

The `UseCase<Params, Unit>` contract promises: "invoke this to perform the action, get success or failure." But callers also need to know the new level, whether it was mastered, and what changed. Because the return type is `Unit`, callers re-implement the algorithm:

```kotlin
// reviewWord() in ViewModel — caller re-derives what the use case already computed:
val previousLevel = word.level
wordUseCases.reviewWord(word, quality)      // ← discards the result entirely
val newLevel = computeNewLevel(...)          // ← re-does the computation
```

The use case's actual post-condition (word updated, level changed by algorithm) is richer than what `Unit` communicates. Every caller is forced to violate the use case's abstraction boundary to get the information they need.

**Violation 3 — `ReviewSessionManager.begin()` silently overwrites in-progress sessions.**

The `begin()` contract implies "start a new session." If called twice without `end()`, it silently resets `currentSessionId` and all counters. The previous session is orphaned in the database with no `endedAt`. The implementation doesn't match the implied contract:

```kotlin
suspend fun begin(reviewType: String) {
    // No guard: if currentSessionId != null, the old session is abandoned silently
    currentSessionId = sessionId   // overwrites
    reviewedCardCount = 0          // resets counters
    ...
}
```

**Violation 4 — `reviewWord(word, quality)` silently accepts words outside the active session.**

The method has no precondition that `word` belongs to the currently loaded list. The session counters increment regardless. The caller is implicitly expected to pass only words from `wordListState`, but nothing enforces this. A call with a stale or unrelated word produces corrupt session statistics.

---

### I — Interface Segregation Principle

Clients should not depend on interfaces they don't use.

**Violation 1 — `ISettingsRepository` exposes ~15 methods; the ViewModel uses 2.**

```kotlin
class ReviewViewModel(
    private val settingsRepository: ISettingsRepository,  // ← fat interface
    ...
)
```

The ViewModel calls only `getTtsSettings()` and `setTtsSpeechRate()`. But the injected interface also exposes `getLanguage`, `setLanguage`, `getThemeMode`, `setThemeMode`, `clearSettings`, `getNotificationsEnabled`, `setNotificationsEnabled`, `getReviewRemindersEnabled`, `getMotivationalMessagesEnabled`, `getDailyReminderTime`, `getMinimumDueCards`, and more. The ViewModel is coupled to changes in any of those.

**Violation 2 — `ITtsRepository` is injected; only its `ttsState` flow is used.**

```kotlin
class ReviewViewModel(
    ttsRepository: ITtsRepository,   // ← stored as val in a workaround
    ...
)
```

The init block reads `ttsRepository.ttsState`. The other 8 methods of `ITtsRepository` (`speak`, `stop`, `isModelDownloaded`, `downloadModel`, `isLanguageSupported`, `getSupportedLanguageCodes`, `getModelInfo`, `deleteModel`) are entirely unused by the ViewModel. The dependency is on the full interface for one property.

**Violation 3 — `ReviewSessionUseCases` groups unrelated concerns.**

`ReviewSessionUseCases` bundles `startSession`, `endSession`, `recordEvent`, `recordStreak`, and `getSettings` — but `recordStreak` is called from the ViewModel directly (`sessionUseCases.recordStreak(count)`), while `getSettings` is called from inside `ReviewSessionManager` (a completely different caller). These five use cases have different clients and different change rates. Bundling them creates unnecessary coupling.

**Violation 4 — Test fakes must implement the entire `IWordRepository` (15+ methods).**

```kotlin
// ReviewViewModelTest.kt
private fun fakeWordRepo() = object : IWordRepository {
    override fun getDueCards(): Flow<List<Word>> = flowOf(dueWords)
    override fun getDueCardsByTag(...) ...
    override fun getWordsByStage(...) ...
    override suspend fun deleteWord(...) ...
    override suspend fun updateWord(...) ...
    override suspend fun getAllWordsAsync() ...
    override fun getAllWords() ...
    // ... 8 more stubs
}
```

This is ISP pain leaking into tests. Every test fake must stub methods it doesn't care about. If `IWordRepository` grows, every fake in the codebase breaks.

---

### D — Dependency Inversion Principle

High-level modules should not depend on low-level modules. Both should depend on abstractions.

**Violation 1 — `ReviewSessionManager` is a concrete class instantiated directly.**

```kotlin
private val session = ReviewSessionManager(sessionUseCases, analyticsTracker)
```

There is no `IReviewSessionManager`. The ViewModel is hardwired to this implementation. You cannot substitute a test double, a session-replay implementation, or a no-op implementation without modifying `ReviewViewModel`. The high-level policy (ViewModel) directly creates a low-level detail (SessionManager).

**Violation 2 — ViewModel depends on repository layer directly (architecture violation).**

```kotlin
class ReviewViewModel(
    private val settingsRepository: ISettingsRepository,  // repository, not use case
    ttsRepository: ITtsRepository,                         // repository, not use case
    ...
)
```

The project's own architecture rules state the data flow is `ViewModel → UseCase → Repository`. The ViewModel must never import from the data layer. Both repository injections bypass this contract. The ViewModel now depends on the data layer's interface, coupling it to persistence decisions.

**Violation 3 — `computeNewLevel` imports domain algorithm detail into presentation.**

```kotlin
import domain.settings.model.ReviewSettings

private fun computeNewLevel(
    previousLevel: Int,
    quality: Int,
    settings: ReviewSettings,   // ← domain model imported into ViewModel
): Int { ... }
```

The ViewModel imports `ReviewSettings` not to display it but to run a domain algorithm. Presentation layer depending on domain algorithm models is a direction violation. The domain algorithm belongs entirely in the domain layer; the ViewModel should only receive results.

**Violation 4 — `ReviewSessionManager` depends on `IAnalyticsTracker` (wrong direction).**

```kotlin
private class ReviewSessionManager(
    private val sessionUseCases: ReviewSessionUseCases,
    private val analyticsTracker: IAnalyticsTracker,  // ← cross-cutting concern injected into coordinator
) {
    suspend fun end(...) {
        ...
        analyticsTracker.logReviewSessionComplete(...)  // ← session manager fires analytics
    }
}
```

`ReviewSessionManager` is an internal coordinator (low-level). `IAnalyticsTracker` is a cross-cutting concern that should be orchestrated by the high-level module (the ViewModel). A low-level module calling a cross-cutting concern is a DIP violation: the dependency arrow points the wrong way. The ViewModel should call analytics after the session manager signals completion — not have the session manager fire analytics itself.

**Violation 5 — `startStageReview` calls a public sibling method internally.**

```kotlin
fun startStageReview(stage: LearningStage) {
    loadWordsByStage(stage)        // ← calls another public method on `this`
    viewModelScope.launch {
        session.begin("BROWSE")
        ...
    }
}
```

`startStageReview` depends on `loadWordsByStage` by calling it directly. These two coroutines race: `session.begin()` executes concurrently with `loadWordsByStage`'s coroutine launch, so `session.begin()` may run before or after the state update. The internal coupling between public methods is invisible to the caller and impossible to test in isolation.

---

### Additional: Violations in the UI layer (`ReviewBottomSheet.kt`)

The document so far focused on `ReviewViewModel`. A full audit must also cover `ReviewBottomSheet.kt` — it contains violations that are equally serious and directly caused by the ViewModel's design failures.

---

#### A — The composable IS a second ViewModel (SRP)

`ReviewBottomSheet` declares **seven mutable state variables** that belong in the ViewModel:

```kotlin
var currentIndex by remember { mutableStateOf(0) }        // card cursor
var isFlipped by remember { mutableStateOf(false) }        // flip state
var showCompletion by remember { mutableStateOf(false) }   // session lifecycle
var reviewedCount by remember { mutableIntStateOf(0) }     // session stat
var knownCount by remember { mutableIntStateOf(0) }        // session stat
var unknownCount by remember { mutableIntStateOf(0) }      // session stat
var isAutoPlayEnabled by remember { mutableStateOf(false) } // playback pref

val initialWordCount = remember(wordListState) { ... }     // session start snapshot
```

The composable function has taken on the ViewModel's responsibilities: it drives navigation, tracks session statistics, and decides when the session ends. This is SRP broken at the architecture boundary — not just inside a class, but across layers.

---

#### B — Duplicate session tracking creates a correctness gap (SRP + correctness bug)

Session statistics are now tracked in **two independent places**:

| Stat | ViewModel (`ReviewSessionManager`) | Composable (`ReviewBottomSheet`) |
|---|---|---|
| Correct / Known | `correctCardCount` | `knownCount` |
| Incorrect / Unknown | `incorrectCardCount` | `unknownCount` |
| Total reviewed | `reviewedCardCount` | `reviewedCount` |

The ViewModel's counters are persisted to the analytics database. The composable's counters are shown to the user on the completion screen. If they ever diverge — and they can, since there is no synchronization — the user sees one number while a different number is stored. Two sources of truth for the same facts is a LSP violation at the data-flow level.

---

#### C — `initialWordCount` recomputes on deletion — a real bug (SRP cascade)

```kotlin
val initialWordCount = remember(wordListState) {
    if (wordListState is UiState.Loaded) wordListState.value.size else 0
}
```

`remember(key)` recomputes whenever the key changes. `wordListState` changes every time a word is deleted (the list is smaller). So if the user starts with 10 words, deletes 2, then completes the session, `initialWordCount` = 8, not 10. The completion screen says "8 reviewed" — a wrong number. This bug exists solely because session-start state is stored in a composable `remember` block instead of the ViewModel.

---

#### D — `handleReview` free function — business logic in the UI file (SRP + D)

```kotlin
// ReviewBottomSheet.kt — not a composable, a plain function with business logic
private fun handleReview(
    words: List<Word>,
    currentIndex: Int,
    rating: Int,
    onReviewWord: (Word, Int) -> Unit,
    onNext: () -> Unit,
    onComplete: () -> Unit
) {
    if (currentIndex < words.size) {
        onReviewWord(words[currentIndex], rating)
        val isLastWord = currentIndex == words.size - 1
        if (!isLastWord) onNext() else onComplete()
    }
}
```

This decides: "rate the word, then advance to the next card OR end the session." That is a business rule. It lives in a Compose UI file. The ViewModel's `reviewWord()` method should make this decision and emit a `SessionComplete` effect on the last card. Instead the UI carries the responsibility.

---

#### E — `LaunchedEffect` blocks contain business rules (SRP + architecture anti-pattern)

CLAUDE.md explicitly bans `LaunchedEffect` for effects ("use `OnEvents`"). Three `LaunchedEffect` blocks in `ReviewBottomSheet` violate this and each encodes a business rule:

```kotlin
// Rule: "when the word list becomes empty in REVIEW mode, show completion screen"
LaunchedEffect(wordListState) {
    if (wordListState is UiState.Loaded) {
        val words = wordListState.value
        when {
            words.isEmpty() && !showCompletion -> {
                if (reviewType == ReviewType.REVIEW && initialWordCount > 0) {
                    reviewedCount = initialWordCount
                    showCompletion = true
                } else {
                    onReviewComplete()
                }
            }
        }
    }
}

// Rule: "reset flip state when index changes"
LaunchedEffect(currentIndex) {
    isFlipped = false
}

// Rule: "auto-play TTS when card changes and auto-play is on"
LaunchedEffect(currentIndex, isAutoPlayEnabled) { ... }
```

All three of these belong in the ViewModel: the first as a state transition, the second as part of the navigate action, the third as a side-effect triggered by index change in state.

---

#### F — `isFlipped = false` is written in four separate places (SRP + no single source of truth)

```kotlin
LaunchedEffect(currentIndex) { isFlipped = false }    // implicit reset via effect

onNavigateBack = {
    if (currentIndex > 0) {
        currentIndex--
        isFlipped = false   // explicit reset
    }
},
onNavigateForward = {
    if (currentIndex < words.size - 1) {
        currentIndex++
        isFlipped = false   // explicit reset
    }
},
onNext = {
    currentIndex++
    isFlipped = false       // explicit reset
},
```

The LaunchedEffect adds an extra recomposition cycle (coroutine launch overhead) to reset what the navigation handlers already reset manually. These four sites can drift independently: adding a fifth navigation action might forget to reset `isFlipped`.

---

#### G — `ReviewBottomSheetContent` overrides ViewModel state — inverted ownership (DIP)

```kotlin
ReviewBottomSheet(
    state = reviewState.copy(reviewType = reviewType),  // ← discards VM's reviewType
    ...
)
```

The caller passes `reviewType` as a separate parameter, then overwrites the ViewModel's `reviewType` in state. This means the ViewModel's `reviewType` is never actually used. The UI is the authoritative source for `reviewType`, not the ViewModel. This inverts the data flow: the screen dictates state to the screen's own state object. In the ideal design, `ReviewSource` and `ReviewType` are established when `startSession` is called — the screen has no reason to pass them separately.

---

#### H — `ErrorState` classifies domain errors by parsing exception message strings (DIP + fragility)

```kotlin
val isNetworkError = message.contains("timeout", ignoreCase = true) ||
    message.contains("connect", ignoreCase = true) ||
    message.contains("network", ignoreCase = true) ||
    message.contains("internet", ignoreCase = true)
```

Error classification is domain/use-case logic. A composable reading the raw exception message string and inferring the error type from keywords is a DIP violation: the presentation layer depends on the internal detail (exception message text) of the infrastructure layer. If the network library changes its error messages, or the app is localized, this silently breaks. The correct fix is a sealed `ReviewError` in the domain layer (`NetworkError`, `EmptyResult`, `UnknownError`) that the use case returns and the UI pattern-matches.

---

#### I — `startStageReview` analytics always logs `cardCount = 0` (race is worse than noted)

The race condition in Section 1 (D-Violation 5) understates the impact. The two coroutines don't just race — one of them **always loses**:

```kotlin
fun startStageReview(stage: LearningStage) {
    loadWordsByStage(stage)      // Coroutine A: sets state to Loading, then Loaded
    viewModelScope.launch {      // Coroutine B: runs concurrently
        session.begin("BROWSE")
        // Reads currentState.review.wordListState here.
        // Coroutine A set it to UiState.Loading at its start.
        // Coroutine B's first suspension point is session.begin() which calls getSettings().
        // By the time getSettings() suspends and resumes, Coroutine A may have set Loaded.
        // But there is NO await or ordering guarantee.
        val wordListState = currentState.review.wordListState
        val cardCount = if (wordListState is UiState.Loaded) wordListState.value.size else 0
        analyticsTracker.logReviewSessionStart(cardCount = cardCount)
        // In practice: almost always logs cardCount = 0
    }
}
```

`loadWordsByStage` immediately sets state to `UiState.Loading`. Coroutine B reads state while Coroutine A is still fetching. The analytics event fires with 0 cards. This is a measurable data quality defect, not just a theoretical race.

---

#### J — DI module confirms repository injection as systemic (DIP)

`StudyModule.kt` wires the ViewModel with:

```kotlin
settingsRepository = get(),
ttsRepository = get(),
```

The architecture violation is encoded into the DI graph. It cannot be fixed by changing only `ReviewViewModel.kt` — the module must be updated too. This confirms the DIP violation is not a local oversight but a systemic wiring decision.

---

---

## 1C. SoC, DRY, YAGNI, KISS, Maintainability, and Boundary Violations

---

### Separation of Concerns

**SoC-1 — `startObservingProgress()` mixes four unrelated concerns in one `collect` block**

`StudyProgressViewModel.startObservingProgress()` (same feature module, same architectural context):

```kotlin
getProgressStatsUseCase.invoke().collect { stats ->
    // Concern 1: state update
    updateState { copy(progress = UiState.Loaded(screenState)) }
    // Concern 2: performance tracing
    performanceTracer.putMetric(trace, "total_words", stats.totalWords.toLong())
    performanceTracer.stopTrace(trace)
    // Concern 3: analytics
    analyticsTracker.updateUserProgress(...)
    // Concern 4: notification scheduling
    scheduleNotificationsUseCase(stats = stats, ...)
}
```

A single stats emission triggers state, telemetry, analytics, and push notifications. Each concern has a different change rate and a different reason to fail. A notification scheduling failure should not affect the state update. These should be separate flows or a post-`collect` side-effect pipeline.

**SoC-2 — `ReviewSessionManager.end()` fires analytics directly (wrong layer)**

Already noted in the DIP section — but framed as a SoC issue: session accounting (counting cards, timestamps) is one concern. Analytics reporting (`logReviewSessionComplete`) is a completely separate cross-cutting concern. Mixing them means the session coordinator knows about the analytics schema.

**SoC-3 — `resolveLanguageCode` traverses the full word list to detect TTS language**

```kotlin
private fun resolveLanguageCode(text: String, languageCode: String, state: ReviewState): String {
    val words = (state.review.wordListState as? UiState.Loaded<List<Word>>)?.value
    val languageCodes = words.map { word ->
        if (isTargetSide) word.targetLanguage.code else word.sourceLanguage.code
    }
    return languageCodes.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: languageCode
}
```

Language resolution (a domain/TTS concern) is implemented as a private free function in the presentation layer, reading full word-list state on every TTS call. The correct owner is the `SpeakWordUseCase` or a `ResolveWordLanguageUseCase` that takes the word directly.

**SoC-4 — `StudyScreen` makes routing decisions that belong in the ViewModel**

```kotlin
onStartReview = {
    if (dueTags.isNotEmpty() && !skipTagSelector) {
        // show tag selector sheet, then call startReview() or startTagReview(tag.id)
    } else {
        reviewViewModel.startReview()
    }
}
```

The screen reads `dueTags` and `skipTagSelector` from the ViewModel and branches on them. The routing rule "show tag selector iff there are tags and selector is not skipped" is business logic in the screen. The ViewModel should expose a single `requestStartReview()` method and emit an effect (`ShowTagSelector(tags)` or `BeginReview`) — the screen renders, not decides.

**SoC-5 — `onDeleteWord` callback runs UI cursor adjustment synchronously before deletion completes**

```kotlin
// StudyScreen.kt
onDeleteWord = { wordId, onComplete ->
    reviewViewModel.deleteWord(wordId)
    onComplete()    // ← called immediately, before ViewModel's deleteWord coroutine has finished
}
```

`deleteWord` launches a coroutine; `onComplete()` runs right after the launch, not after the deletion. If deletion fails, the UI has already adjusted `currentIndex`. The callback pattern mixes screen navigation state with async repository results — two concerns that must be sequenced but aren't.

---

### DRY (Don't Repeat Yourself)

**DRY-1 — `GetReviewSettingsUseCase` fetched twice per review action**

`ReviewWordUseCase.invoke()` calls `getReviewSettingsUseCase(Unit).getOrThrow()`. `ReviewSessionManager.begin()` also calls `sessionUseCases.getSettings(Unit)`. Both paths run on every review. The settings value is always `ReviewSettings.BALANCED` — a compile-time constant (see below). Two async suspending calls for the same constant, with duplicated error handling.

**DRY-2 — `ReviewBottomSheetContent` block copy-pasted in `StudyScreen`**

The overlay-opening code for a due-word review and a tag-filtered review is nearly identical — same composable, same lambda structure, ~20 lines each, differing only in `initialWord` and `onLoadWords`. Neither is extracted into a function. Any change to the sheet parameters (new TTS flag, new callback) requires two edits.

**DRY-3 — Network error string-matching duplicated across screens**

`ReviewStateScreens.ErrorState` and `StudyScreen`'s progress error block both contain the same `message.contains("timeout", ...)` pattern to classify network errors. Two separate parsing implementations for the same rule.

**DRY-4 — `computeNewLevel` in ViewModel re-implements `ReviewWordUseCase` algorithm**

Already documented in the SOLID section (OCP-Violation 2). Noted here for completeness as the primary DRY violation: one algorithmic fact expressed in two files.

---

### YAGNI (You Aren't Gonna Need It)

**YAGNI-1 — `GetReviewSettingsUseCase` is a use case wrapping a hardcoded constant**

```kotlin
// The entire use case:
class GetReviewSettingsUseCase : NoParamUseCase<ReviewSettings> {
    override suspend operator fun invoke(params: Unit): Try<ReviewSettings> {
        return Try.success(ReviewSettings.BALANCED)  // ← always BALANCED
    }
}
```

The inline comment in the file says: *"Settings are no longer user-configurable — simplified to client-side only."* The use case interface, the `Try` wrapper, the `suspend` boundary, and the `getOrThrow()` call at every invocation site exist to support configurability that was removed. The current code should use `ReviewSettings.BALANCED` as a constant directly. Every async call site pays a suspension overhead for a value that could be a `val`.

**YAGNI-2 — `startReview()` + `ReviewEffect.StartReview(firstWord)` is an unnecessary two-step protocol**

```kotlin
// Step 1: screen calls startReview()
// Step 2: ViewModel peeks at first word, emits effect
// Step 3: effect handler in screen calls startDueReview() + opens overlay
// Step 4: overlay passes firstWord back to ReviewBottomSheet as initialWord
// Step 5: LaunchedEffect in ReviewBottomSheet finds firstWord in the list → index 0
```

The `firstWord` carried through this chain resolves to index 0 every time — it IS the first word in the due list that `startDueReview` will load. The entire pre-flight check adds a round-trip network call (getDueWords peek), an effect emission, and a `LaunchedEffect` lookup in the UI just to confirm "the first word is index 0." A single `startDueReview()` call achieves the same result. If the list is empty, `EmptyState` renders — no pre-flight check needed.

**YAGNI-3 — `ReviewSessionUseCases` bundle adds an indirection level with no benefit**

```kotlin
data class ReviewSessionUseCases(
    val startSession: StartStudySessionUseCase,
    val endSession: EndStudySessionUseCase,
    val recordEvent: RecordReviewEventUseCase,
    val recordStreak: RecordStreakActivityUseCase,
    val getSettings: GetReviewSettingsUseCase,
)
```

This data class exists to group five use cases for constructor brevity. But three of the five are called in different places with different callers (ViewModel vs. SessionManager vs. direct session end). The grouping obscures the actual dependencies and provides no isolation benefit. Individual injection is clearer.

---

### KISS (Keep It Simple, Stupid)

**KISS-1 — The `startReview → effect → startDueReview` ping-pong pattern**

The simple intent — "user taps Start, load and show due words" — is implemented as a two-ViewModel-call, effect-mediated protocol with an `initialWord` hand-off through three layers. The simplest design: one public method, one state transition. The current design is the opposite of KISS.

**KISS-2 — `ReviewSessionManager` is an unnecessary class**

The class holds 4 `var` fields and 2 `suspend fun` methods. Its total state is `sessionId`, `startTime`, `reviewedCount`, `correctCount`, `incorrectCount`, and `sessionSettings`. In the ideal design these become an immutable `SessionContext` data class — no class, no constructor, no indirection. The "manager" abstraction adds cognitive load without encapsulating anything.

**KISS-3 — Ad-hoc session ID generation**

```kotlin
val sessionId = Clock.System.now().toEpochMilliseconds().toString() +
    "-" + (0..999999).random().toString().padStart(6, '0')
```

This hand-rolls a timestamp + random-suffix ID. `kotlin.uuid.Uuid.random().toString()` is one call and collision-free. The custom format adds complexity and is not standard anywhere in the codebase.

**KISS-4 — `ReviewWordUseCases` bundle mirrors `ReviewSessionUseCases` pattern unnecessarily**

Two data classes (`ReviewWordUseCases`, `ReviewSessionUseCases`) bundle dependencies. Neither enforces grouping rules — a future developer can put anything in either. Individual constructor parameters with clear names are simpler and self-documenting.

---

### Maintainability

**Maintainability-1 — Session state split across ViewModel and UI makes bug reproduction impossible**

When a user reports "the completion screen showed the wrong count," diagnosing the bug requires inspecting both `ReviewSessionManager` counter state (ViewModel) and `knownCount`/`initialWordCount` state (composable `remember`). Neither is observable from outside in a test. No single log line can capture the full session state.

**Maintainability-2 — `startStageReview` is the only method that doesn't set `Loading` state before `session.begin()`**

```kotlin
fun startDueReview()    { updateState { copy(...Loading) } ; session.begin() }
fun startTagReview()    { session.begin() ; updateState { copy(...Loading) } }
fun startStageTagReview() { session.begin() ; updateState { copy(...Loading) } }
fun startStageReview()  { loadWordsByStage() ; session.begin() }  // no Loading set at all, delegated
```

Each `start*` method has a slightly different sequencing of `Loading` state and `session.begin()`. This inconsistency is invisible in tests (the state machine isn't verified) and will silently produce different behaviors for different entry points. A maintainer adding a sixth method has no template to follow.

**Maintainability-3 — `onDeleteWord` has two different contracts at two call sites**

In `StudyScreen`, `onDeleteWord = { wordId, onComplete -> reviewViewModel.deleteWord(wordId); onComplete() }` — the callback fires unconditionally. But the `EditWordSheetContent` usage (inside `ReviewBottomSheet`) has complex index-clamping logic in the callback body. The same interface means different things to different callers. There is no contract, only convention.

**Maintainability-4 — `ReviewType` in state is overridden by the caller at every call site**

Every place that opens `ReviewBottomSheetContent` passes `reviewType` as a parameter AND `reviewState` (which already contains `reviewType`), then discards the ViewModel's value with `.copy(reviewType = reviewType)`. This means there are two sources of `reviewType` truth at every call site, and the ViewModel's value is always wrong. A future developer who sets `reviewType` in the ViewModel state and expects it to be respected will be silently ignored.

---

## 2. Ideal Design

### 2.1 Sealed lifecycle state — the cornerstone fix

The review session has clear phases. The current flat `ReviewState` with a nested `UiState<List<Word>>` cannot represent them without boolean/null checks in the UI. Model them explicitly:

```kotlin
sealed class ReviewState {

    /** Pre-session: nothing to show, no resource held */
    data object Idle : ReviewState()

    /** Words are being fetched */
    data object Loading : ReviewState()

    /** Fetch succeeded but zero words matched this source */
    data object Empty : ReviewState()

    /** Fetch failed — retry is safe */
    data class Error(val message: String) : ReviewState()

    /** Live session: ViewModel owns the card cursor completely */
    data class Active(
        val queue: List<Word>,
        val currentIndex: Int,
        val isFlipped: Boolean,
        val reviewType: ReviewType,
        val source: ReviewSource,
        val ttsState: TtsState,
        val speechRate: Float,
    ) : ReviewState() {
        val currentWord: Word get() = queue[currentIndex]
        val progress: Float get() = (currentIndex + 1f) / queue.size
        val isFirst: Boolean get() = currentIndex == 0
        val isLast: Boolean get() = currentIndex == queue.size - 1
        val remainingCount: Int get() = queue.size - currentIndex - 1
    }

    /** Session ended normally — show summary, then dismiss */
    data class Completed(
        val totalReviewed: Int,
        val correctCount: Int,
        val incorrectCount: Int,
        val durationMs: Long,
        val source: ReviewSource,
    ) : ReviewState() {
        val accuracy: Float
            get() = if (totalReviewed > 0) correctCount.toFloat() / totalReviewed else 0f
    }
}
```

Fixes addressed:
- **SRP**: The state data class is pure data — no logic, no loading, no counters.
- **No empty-list checks in UI**: `Empty` is a first-class state.
- **Card cursor in ViewModel** (fixes SRP, LSP, and testability): `currentIndex` + `isFlipped` owned here.
- **Session summary in `Completed`**: No need to read from a mutable session object in `onCleared`.

---

### 2.2 `ReviewSource` — open for extension, closed for modification

```kotlin
sealed class ReviewSource {
    data object DueCards : ReviewSource()
    data class ByStage(val stage: LearningStage) : ReviewSource()
    data class ByTag(val tagId: Long) : ReviewSource()
    data class ByStageAndTag(val stage: LearningStage, val tagId: Long) : ReviewSource()
}
```

Adding a new source: one new `data class`, one new `when` branch in `LoadReviewQueueUseCase`. Zero changes in `ReviewViewModel`.

One public method replaces five:
```kotlin
fun startSession(source: ReviewSource, type: ReviewType = ReviewType.REVIEW)
```

---

### 2.3 `LoadReviewQueueUseCase` — OCP + ISP + SRP

Consolidate all 5 loading paths. The ViewModel depends on one abstraction, not 3 concrete use cases:

```kotlin
class LoadReviewQueueUseCase(
    private val getDueWords: GetDueWordsUseCase,
    private val getWordsByStage: GetWordsByStageUseCase,
    private val getDueWordsByTag: GetDueWordsByTagUseCase,
) : UseCase<ReviewSource, List<Word>> {

    override suspend fun invoke(params: ReviewSource): Try<List<Word>> = when (params) {
        is ReviewSource.DueCards ->
            getDueWords(Unit).firstOrCollect()

        is ReviewSource.ByStage ->
            getWordsByStage(params.stage).firstOrCollect()

        is ReviewSource.ByTag ->
            getDueWordsByTag(params.tagId).firstOrCollect()

        is ReviewSource.ByStageAndTag ->
            getWordsByStage(params.stage).firstOrCollect()
                .map { words -> words.filter { params.tagId in it.tagIds } }
    }
}
```

The ViewModel is now open for extension (new sources) without modification.

---

### 2.4 `ReviewWordResult` — LSP + OCP fix for the use case

`ReviewWordUseCase` returns `Try<Unit>`, forcing callers to duplicate the level computation. Fix:

```kotlin
// domain layer
data class ReviewWordResult(
    val updatedWord: Word,
    val previousLevel: Int,
    val newLevel: Int,
    val wasCorrect: Boolean,
) {
    val didLevelUp: Boolean get() = newLevel > previousLevel
    val didLevelDown: Boolean get() = newLevel < previousLevel
    val wasMastered: Boolean get() = previousLevel < 6 && newLevel == 6
}
```

`ReviewWordUseCase` returns `Try<ReviewWordResult>`.

- **LSP fix**: The use case's post-condition is fully expressed in the return type. Callers don't need to re-derive it.
- **OCP fix**: New outcome handling (e.g., "celebrate level 5→6") requires zero changes to `ReviewWordUseCase`.
- **SRP fix**: `computeNewLevel` can be deleted from the ViewModel entirely.
- **DIP fix**: ViewModel no longer imports `ReviewSettings`.

---

### 2.5 `SessionContext` — immutable private data, no shadow state machine

Replace `ReviewSessionManager` (concrete class with `var` fields) with an immutable value object:

```kotlin
private data class SessionContext(
    val sessionId: String,
    val reviewType: String,
    val startedAt: Long,
    val reviewed: Int = 0,
    val correct: Int = 0,
    val incorrect: Int = 0,
) {
    fun withReview(wasCorrect: Boolean) = copy(
        reviewed = reviewed + 1,
        correct = if (wasCorrect) correct + 1 else correct,
        incorrect = if (!wasCorrect) incorrect + 1 else incorrect,
    )
}

// In ViewModel:
private var session: SessionContext? = null
```

Each review replaces `session` atomically: `session = session?.withReview(wasCorrect)`. No mutable fields. No hidden secondary state machine.

- **SRP fix**: `SessionContext` holds data only. All logic around when to start/end lives in the ViewModel methods.
- **DIP fix**: No concrete helper class instantiated with `private val`. No `analyticsTracker` injected into a coordinator.
- **LSP fix**: `begin()` cannot overwrite silently — you simply assign a fresh `SessionContext`.

---

### 2.6 TTS and settings via use cases — DIP + ISP fix

Replace direct repository injection with focused use cases:

```kotlin
// Exposes only the ttsState stream — client depends on minimal abstraction
class ObserveTtsStateUseCase(
    private val repo: ITtsRepository,
) : FlowUseCase<Unit, TtsState> {
    override fun invoke(params: Unit): Flow<TtsState> = repo.ttsState
}

// Exposes only the speech rate stream
class ObserveSpeechRateUseCase(
    private val repo: ISettingsRepository,
) : FlowUseCase<Unit, Float> {
    override fun invoke(params: Unit): Flow<Float> =
        repo.getTtsSettings().map { it.speechRate }
}

// Exposes only the write side
class SetSpeechRateUseCase(
    private val repo: ISettingsRepository,
) : UseCase<Float, Unit> {
    override suspend fun invoke(params: Float): Try<Unit> =
        repo.setTtsSpeechRate(params)
}
```

The ViewModel now depends only on use-case abstractions. `ISettingsRepository`'s 15-method surface is hidden behind a 1-method interface. The full `ITtsRepository` is hidden behind a single `Flow<TtsState>` pipe.

---

### 2.7 Effects — precise contracts

```kotlin
sealed class ReviewEffect {
    /** Session ended — parent screen should refresh stats/progress */
    data object SessionComplete : ReviewEffect()

    /** A word just reached mastery — trigger celebration */
    data class WordMastered(val word: Word) : ReviewEffect()
}
```

`StartReview(firstWord: Word)` is removed. It violated SRP (ViewModel reaching into parent navigation concern) and ISP (passing a `Word` object the parent only used to check "is non-null"). The parent screen should observe due count independently.

---

### 2.8 `updateActiveState` — guard that enforces the state machine

```kotlin
private inline fun updateActiveState(
    transform: ReviewState.Active.() -> ReviewState.Active,
) {
    val current = currentState
    if (current is ReviewState.Active) updateState { current.transform() }
}
```

Every card navigation and flip operation routes through this. Operations that are only valid in `Active` are structurally prevented from firing in other states. This enforces the LSP-style behavioral contract: you cannot flip a card that isn't being shown.

---

### 2.9 `cardShownAt` belongs in the state

In the ideal design, `cardShownAt` is not a naked `private var` — it's recorded when `Active.currentIndex` changes:

```kotlin
data class Active(
    ...
    val cardShownAt: Long,   // set when index changes, used in rateCard
) : ReviewState()
```

Then in `navigateNext` / `navigateBack`:
```kotlin
fun navigateNext() {
    updateActiveState {
        if (!isLast) copy(
            currentIndex = currentIndex + 1,
            isFlipped = false,
            cardShownAt = Clock.System.now().toEpochMilliseconds(),
        )
        else this
    }
}
```

`rateCard` reads `active.cardShownAt` directly from state — no side-channel mutable field, fully observable and testable.

---

### 2.10 `onCleared` — fix the coroutine race

```kotlin
override fun onCleared() {
    super.onCleared()
    val ctx = session ?: return
    // NonCancellable ensures this survives scope cancellation
    viewModelScope.launch(NonCancellable) {
        endSessionUseCase(ctx.toEndParams(completedNormally = false))
    }
}
```

The original `viewModelScope.launch { session.end(...) }` in `onCleared` races against the scope being cancelled. `NonCancellable` is the correct fix.

---

## 3. Final ViewModel Constructor

All previous violations resolved:

```kotlin
class ReviewViewModel(
    // Loading — one use case for all sources (OCP, SRP)
    private val loadQueueUseCase: LoadReviewQueueUseCase,
    // Word SRS — returns rich result, no re-computation needed (LSP, OCP, SRP)
    private val reviewWordUseCase: ReviewWordUseCase,
    // Word editing in-session
    private val updateWordUseCase: UpdateWordUseCase,
    private val deleteWordUseCase: DeleteWordUseCase,
    // Session lifecycle — individual use cases, not a bundle (ISP)
    private val startSessionUseCase: StartStudySessionUseCase,
    private val endSessionUseCase: EndStudySessionUseCase,
    private val recordEventUseCase: RecordReviewEventUseCase,
    private val recordStreakUseCase: RecordStreakActivityUseCase,
    // TTS — use-case abstractions, not repositories (DIP, ISP)
    private val speakWordUseCase: SpeakWordUseCase,
    private val observeTtsState: ObserveTtsStateUseCase,
    private val observeSpeechRate: ObserveSpeechRateUseCase,
    private val setSpeechRateUseCase: SetSpeechRateUseCase,
    // Analytics — still a cross-cutting concern injected at the right level (DIP)
    private val analyticsTracker: IAnalyticsTracker,
) : BaseViewModel<ReviewState, ReviewEffect>()
```

Zero repository injections. Zero concrete internal class instantiations. Every dependency is either an interface or a use-case abstraction.

---

## 4. Screen: purely declarative

```kotlin
@Composable
fun ReviewScreen(viewModel: ReviewViewModel) {
    val state = viewModel.state()

    OnEvents(viewModel.effects) { effect ->
        when (effect) {
            ReviewEffect.SessionComplete -> { /* parent reloads progress stats */ }
            is ReviewEffect.WordMastered -> MasteredCelebration(effect.word)
        }
    }

    when (state) {
        ReviewState.Idle    -> { /* nothing rendered */ }
        ReviewState.Loading -> LoadingState()
        ReviewState.Empty   -> EmptyState()
        is ReviewState.Error     -> ErrorState(state.message, onRetry = viewModel::retryLoad)
        is ReviewState.Active    -> ActiveReviewContent(
            state       = state,
            onFlip      = viewModel::flipCard,
            onBack      = viewModel::navigateBack,
            onForward   = viewModel::navigateNext,
            onRate      = viewModel::rateCard,
            onSpeak     = viewModel::speak,
            onClose     = viewModel::completeSession,
        )
        is ReviewState.Completed -> CompletionScreen(
            state     = state,
            onDismiss = viewModel::dismissCompletion,
        )
    }
}
```

No `currentIndex` in `remember`. No `isFlipped` in `remember`. No `cardShownTimestamp` anywhere in the UI. The screen has exactly one responsibility: render what the ViewModel says.

---

## 5. What Each Principle Now Buys You

| Principle | Violations found | How the ideal design fixes them |
|---|---|---|
| **S** — Single responsibility | ViewModel: 10 responsibilities. `ReviewSessionManager`: 4. `ReviewBottomSheet`: owns 7 mutable state vars, `handleReview` business logic, session completion rule in `LaunchedEffect` | ViewModel orchestrates only. `SessionContext` holds data only. `LoadQueueUseCase` loads only. `ReviewWordResult` carries outcome only. All card cursor + flip state moves into `ReviewState.Active`. Composable renders only. |
| **O** — Open/Closed | New card source = modify ViewModel (5 `start*` methods). SRS change = modify 2 files. `handleReview` in UI must be opened to change advance/complete logic. | `ReviewSource` sealed class + `LoadReviewQueueUseCase`. New source = 1 subclass + 1 `when` branch. `rateCard` emits `SessionComplete` effect on last card — advance/complete logic is in one place. |
| **L** — Liskov substitution | `startReview` ≠ `startDueReview`. `Try<Unit>` forces callers to re-derive level. `begin()` silently overwrites sessions. Dual tracking of stats (VM + UI) can diverge. | Single `startSession(source, type)`. `ReviewWordResult` fully expresses post-condition. `SessionContext` replaced atomically (no overwrite). One tracking location. |
| **I** — Interface segregation | `ISettingsRepository` (15 methods, 2 used). `ITtsRepository` (8 methods, 1 used). `ReviewSessionUseCases` bundles unrelated concerns. Test fakes implement 15+ methods. | `ObserveSpeechRateUseCase`, `SetSpeechRateUseCase`, `ObserveTtsStateUseCase` each expose one method. Sealed `ReviewError` replaces fat error strings. |
| **D** — Dependency inversion | `ReviewSessionManager` concrete class instantiated in VM. `settingsRepository` + `ttsRepository` injected at VM level (confirmed in DI module). `computeNewLevel` imports `ReviewSettings` into presentation. `ReviewBottomSheetContent` overrides ViewModel's `reviewType`. `ErrorState` depends on exception message strings from infrastructure layer. | `SessionContext` is a value object. All VM dependencies are use-case abstractions. `ReviewWordResult` eliminates `ReviewSettings` import. Single `startSession` as source of truth for `reviewType`. Sealed `ReviewError` replaces string-matching. |

---

## 6. Testability Matrix (all plain coroutine tests, no Compose harness)

The ideal design moves all session state into the ViewModel. Every scenario below is testable with plain `runTest` — no Compose harness, no `remember`, no `LaunchedEffect` to observe.

| Scenario | Assert on | Currently testable? |
|---|---|---|
| `startSession(DueCards)` → `Active` state | `state is Active`, `queue.size`, `currentIndex == 0` | Partially (no cursor) |
| `startSession(DueCards)` with empty list → `Empty` | `state is Empty` | No (Empty not a state) |
| `startSession(...)` failure → `Error` with typed error | `state is Error`, error type is `NetworkError` vs `UnknownError` | No (string only) |
| `flipCard()` toggles `isFlipped` | `Active.isFlipped` | No (lives in UI `remember`) |
| `navigateNext()` advances index | `Active.currentIndex == 1` | No (lives in UI `remember`) |
| `navigateNext()` at last card is no-op | index unchanged | No |
| `rateCard(1)` on last card emits `SessionComplete` | `SessionComplete` effect | No (decision in UI `handleReview`) |
| `rateCard(0)` for mastered word does NOT emit `WordMastered` | no `WordMastered` effect | No |
| `rateCard(1)` for level 5 word emits `WordMastered` | `WordMastered` effect | No |
| `rateCard(n)` updates session counters | `session.correct`, `session.incorrect` | No (split across VM + UI) |
| `deleteWord(id)` removes word, clamps index | word absent, index within `[0, queue.lastIndex]` | No (index in UI) |
| `completeSession()` → `Completed` with correct stats | `Completed.correctCount` matches `rateCard` calls, `durationMs > 0` | No (stats split, `initialWordCount` bug) |
| `startSession(ByStage)` logs correct `cardCount` in analytics | fake tracker receives actual count | No (always 0 due to race) |
| `onCleared` while Active ends session with `completedNormally = false` | `endSession` called on fake recorder | No (coroutine race in `onCleared`) |
| `retryLoad()` re-issues `startSession` with same source | `state is Active` after fake returns words | No (no retry mechanism) |
