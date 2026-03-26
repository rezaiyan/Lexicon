---
name: state-machines
description: Model complex screens as sealed interface state machines with pure reducers — StudySession pattern, TransitionResult, and integration with BaseViewModel
argument-hint: "<screen or flow to model as a state machine>"
user-invocable: true
allowed-tools: ["Read", "Write", "Edit", "Glob", "Grep"]
---

# Lexicon State Machine Patterns

Use state machines when a screen has mutually exclusive states with different data (not just different boolean flags). The study session, import wizard, and TTS download flow are natural state machines.

---

## When to Use a State Machine vs Flags

**Use a state machine when:**
- Two or more states are mutually exclusive (can't be Loading AND showing results)
- Each state has different associated data
- Illegal state combinations are possible with flat flags (e.g., `isLoading = true` AND `error != null`)

**Use flat state (`data class` with fields) when:**
- States can be combined (e.g., `isRefreshing` while still showing results)
- The screen always shows the same structure, just with different data

---

## Sealed Interface Pattern

```kotlin
// domain/src/commonMain/kotlin/domain/study/model/SessionState.kt
sealed interface SessionState {
    // No data needed — show empty/idle UI
    data object Idle : SessionState

    // Loading data — show skeleton
    data class Loading(val filter: SessionFilter) : SessionState

    // Core state — all data for the active study experience
    data class Active(
        val sessionId: String,
        val queue: List<Word>,       // remaining cards to show
        val currentCard: Word,
        val phase: CardPhase,        // SHOWING_FRONT | SHOWING_BACK
        val progress: SessionProgress,
        val startedAt: Long,
    ) : SessionState

    // User paused — snapshot of Active so we can resume
    data class Paused(val snapshot: Active) : SessionState

    // Session done — show summary
    data class Completed(
        val sessionId: String,
        val summary: SessionSummary,
    ) : SessionState

    // Something went wrong — show error + allow retry
    data class Failed(
        val error: Throwable,
        val filter: SessionFilter,
    ) : SessionState
}

enum class CardPhase { SHOWING_FRONT, SHOWING_BACK }

data class SessionProgress(
    val reviewed: Int,
    val correct: Int,
    val total: Int,
) {
    val accuracy: Float get() = if (reviewed == 0) 0f else correct.toFloat() / reviewed
    val isComplete: Boolean get() = reviewed >= total
}
```

---

## Pure Reducer

The reducer is a pure function — no side effects, no coroutines, no DI. Given state + command → new state.

```kotlin
// domain/src/commonMain/kotlin/domain/study/SessionReducer.kt
object SessionReducer {

    sealed interface Command {
        data class Start(val filter: SessionFilter, val words: List<Word>) : Command
        data object Flip : Command
        data class Review(val quality: ReviewQuality) : Command
        data object Pause : Command
        data object Resume : Command
        data object Abandon : Command
    }

    fun reduce(state: SessionState, command: Command): SessionState = when {
        // Start: Idle/Failed → Loading → Active
        command is Command.Start && state is SessionState.Idle ->
            if (command.words.isEmpty()) {
                SessionState.Failed(
                    error = IllegalStateException("No cards to review"),
                    filter = command.filter,
                )
            } else {
                SessionState.Active(
                    sessionId = generateSessionId(),
                    queue = command.words.drop(1),
                    currentCard = command.words.first(),
                    phase = CardPhase.SHOWING_FRONT,
                    progress = SessionProgress(reviewed = 0, correct = 0, total = command.words.size),
                    startedAt = currentTimeMs(),
                )
            }

        // Flip: Active(FRONT) → Active(BACK)
        command is Command.Flip && state is SessionState.Active
                && state.phase == CardPhase.SHOWING_FRONT ->
            state.copy(phase = CardPhase.SHOWING_BACK)

        // Review: Active → Active (next card) or Completed
        command is Command.Review && state is SessionState.Active
                && state.phase == CardPhase.SHOWING_BACK -> {
            val isCorrect = command.quality == ReviewQuality.REMEMBERED
            val newProgress = state.progress.copy(
                reviewed = state.progress.reviewed + 1,
                correct = state.progress.correct + if (isCorrect) 1 else 0,
            )
            if (state.queue.isEmpty()) {
                SessionState.Completed(
                    sessionId = state.sessionId,
                    summary = SessionSummary(
                        totalReviewed = newProgress.reviewed,
                        correctCount = newProgress.correct,
                        durationMs = currentTimeMs() - state.startedAt,
                    ),
                )
            } else {
                state.copy(
                    currentCard = state.queue.first(),
                    queue = state.queue.drop(1),
                    phase = CardPhase.SHOWING_FRONT,
                    progress = newProgress,
                )
            }
        }

        // Pause: Active → Paused
        command is Command.Pause && state is SessionState.Active ->
            SessionState.Paused(snapshot = state)

        // Resume: Paused → Active
        command is Command.Resume && state is SessionState.Paused ->
            state.snapshot

        // Abandon: any → Idle
        command is Command.Abandon -> SessionState.Idle

        // Illegal transition — log and return current state unchanged
        else -> {
            println("WARN: Illegal transition: ${state::class.simpleName} + ${command::class.simpleName}")
            state
        }
    }
}
```

**Rules:**
- Pure function: no `suspend`, no side effects, no DI
- Returns the **same** state type — not a result wrapper
- Illegal transitions return current state unchanged (log a warning, don't crash)
- Each valid `state × command` pair produces exactly one output state
- Helper functions (`generateSessionId`, `currentTimeMs`) are injected via parameters or platform expect/actual — never global mutable state

---

## ViewModel Integration

The ViewModel dispatches commands to the reducer and handles side effects separately:

```kotlin
class ReviewViewModel(
    private val getDueWordsUseCase: GetDueWordsUseCase,
    private val reviewWordUseCase: ReviewWordUseCase,
    private val analyticsRecorder: IAnalyticsRecorder,
) : BaseViewModel<ReviewState, ReviewEffect>() {

    override fun initialState() = ReviewState(session = SessionState.Idle)

    // Event sink — called from UI
    fun startSession(filter: SessionFilter = SessionFilter.All) {
        viewModelScope.launch {
            updateState { copy(session = SessionState.Loading(filter)) }
            getDueWordsUseCase(filter).collect { words ->
                val command = SessionReducer.Command.Start(filter, words)
                val newSession = SessionReducer.reduce(state.value.session, command)
                updateState { copy(session = newSession) }
                if (newSession is SessionState.Active) {
                    analyticsRecorder.startSession(newSession.sessionId)
                }
            }
        }
    }

    fun flipCard() {
        val newSession = SessionReducer.reduce(
            state.value.session,
            SessionReducer.Command.Flip,
        )
        updateState { copy(session = newSession) }
    }

    fun reviewCard(quality: ReviewQuality) {
        val currentSession = state.value.session as? SessionState.Active ?: return
        val command = SessionReducer.Command.Review(quality)
        val newSession = SessionReducer.reduce(currentSession, command)
        updateState { copy(session = newSession) }

        // Side effects: persist review, record analytics
        viewModelScope.launch {
            reviewWordUseCase(
                ReviewWordUseCase.Params(currentSession.currentCard, quality)
            ).fold(
                onSuccess = { /* state already updated via reducer */ },
                onFailure = { e -> updateState { copy(error = e.message) } }
            )
            analyticsRecorder.recordReview(currentSession.currentCard.id, quality)

            if (newSession is SessionState.Completed) {
                analyticsRecorder.endSession(currentSession.sessionId, newSession.summary)
                emitEffect(ReviewEffect.ShowCompletionSheet(newSession.summary))
            }
        }
    }

    fun pauseSession() {
        val newSession = SessionReducer.reduce(state.value.session, SessionReducer.Command.Pause)
        updateState { copy(session = newSession) }
    }

    fun resumeSession() {
        val newSession = SessionReducer.reduce(state.value.session, SessionReducer.Command.Resume)
        updateState { copy(session = newSession) }
    }
}

// Screen state holds the session state machine
data class ReviewState(
    val session: SessionState = SessionState.Idle,
    val error: String? = null,
)

sealed interface ReviewEffect {
    data class ShowCompletionSheet(val summary: SessionSummary) : ReviewEffect
    data object NavigateBack : ReviewEffect
}
```

---

## Screen Rendering

Map state machine states to UI — exhaustive `when` ensures compile-time coverage:

```kotlin
@Composable
fun ReviewContent(
    state: ReviewState,
    onFlip: () -> Unit,
    onReview: (ReviewQuality) -> Unit,
    onPause: () -> Unit,
    // ...
) {
    when (val session = state.session) {
        is SessionState.Idle ->
            IdlePrompt(onStart = { /* trigger load */ })

        is SessionState.Loading ->
            LoadingIndicator()

        is SessionState.Active ->
            StudyCard(
                card = session.currentCard,
                phase = session.phase,
                progress = session.progress,
                onFlip = onFlip,
                onReview = onReview,
                onPause = onPause,
            )

        is SessionState.Paused ->
            PausedOverlay(onResume = { /* resume */ })

        is SessionState.Completed ->
            CompletionScreen(summary = session.summary)

        is SessionState.Failed ->
            ErrorState(
                message = session.error.message ?: "Something went wrong",
                onRetry = { /* retry with session.filter */ },
            )
    }
}
```

---

## Testing the Reducer (Pure Function = Easy Tests)

```kotlin
class SessionReducerTest {

    private val words = listOf(
        Word(id = 1, originalWord = "hello", /* ... */),
        Word(id = 2, originalWord = "world", /* ... */),
    )

    @Test
    fun `start command transitions Idle to Active`() {
        val result = SessionReducer.reduce(
            state = SessionState.Idle,
            command = SessionReducer.Command.Start(SessionFilter.All, words),
        )
        assertIs<SessionState.Active>(result)
        assertEquals(words.first(), (result as SessionState.Active).currentCard)
    }

    @Test
    fun `review last card transitions Active to Completed`() {
        val active = SessionState.Active(
            sessionId = "test",
            queue = emptyList(),    // no more cards
            currentCard = words.first(),
            phase = CardPhase.SHOWING_BACK,
            progress = SessionProgress(reviewed = 1, correct = 1, total = 2),
            startedAt = 0L,
        )
        val result = SessionReducer.reduce(
            active,
            SessionReducer.Command.Review(ReviewQuality.REMEMBERED),
        )
        assertIs<SessionState.Completed>(result)
    }

    @Test
    fun `illegal transition returns current state`() {
        val idle = SessionState.Idle
        val result = SessionReducer.reduce(idle, SessionReducer.Command.Flip)
        assertEquals(idle, result)
    }
}
```

---

## Other State Machine Candidates in Lexicon

| Screen / Flow | States |
|---|---|
| Import wizard | `Idle → FileSelected → Extracting → Preview → Importing → Done / Failed` |
| TTS download | `Idle → Downloading(progress) → Available / Failed` |
| Auth flow | `Verifying → LoggedIn / LoggedOut / NeedsOnboarding` |
| Avatar upload | `Idle → Picking → Uploading(progress) → Done / Failed` |

---

## Checklist

1. Sealed interface with one subtype per exclusive state
2. Each subtype carries exactly the data it needs (no shared nullable fields)
3. Reducer is a pure function — `fun reduce(state, command): State`
4. Illegal transitions: return current state + log, never crash
5. ViewModel calls reducer synchronously, handles side effects in `viewModelScope.launch`
6. Screen rendering uses exhaustive `when` on the sealed interface
7. Tests for the reducer are pure unit tests — no coroutines, no mocks
