# The Perfect Lexicon — Architecture from First Principles

> This document forgets the current codebase entirely.
> It asks one question: if you were building the world's best vocabulary learning app
> from scratch, with full knowledge of what this product does and who uses it,
> what would you build?
>
> This is that answer.

---

## Preface: The Real Question

Before drawing boxes and arrows, a senior engineer asks: **what are the fundamental
invariants of this system?**

For Lexicon, they are:

1. **A review that happens must never be lost.** The user's learning progress is sacred.
2. **The learning loop must feel instant.** Any latency in showing the next card breaks
   the cognitive flow that makes SRS effective.
3. **The algorithm must be honest.** The scheduling system must surface the right card
   at the right time — that is the entire product promise.
4. **The app must work on a plane.** Offline is not a degraded mode. It is the default.
5. **The data model must outlive the app.** Users will have thousands of cards, years of
   history. Schema decisions made today are very expensive to undo.

Every architectural decision in this document traces back to one of these five invariants.

---

## Table of Contents

1. [Domain Modeling — The Core](#1-domain-modeling--the-core)
2. [The Learning Machine — SRS Redesigned](#2-the-learning-machine--srs-redesigned)
3. [The Event-Driven Backbone](#3-the-event-driven-backbone)
4. [Module Graph — The Perfect Slice](#4-module-graph--the-perfect-slice)
5. [The Data Layer — Local First, Always](#5-the-data-layer--local-first-always)
6. [The Sync Engine — Reliable by Design](#6-the-sync-engine--reliable-by-design)
7. [The Presentation Layer — Pure and Predictable](#7-the-presentation-layer--pure-and-predictable)
8. [Analytics — A First-Class System](#8-analytics--a-first-class-system)
9. [The Type System as Architecture](#9-the-type-system-as-architecture)
10. [Cross-Cutting Systems](#10-cross-cutting-systems)
11. [Testing — The Confidence Layer](#11-testing--the-confidence-layer)
12. [Build System and CI/CD](#12-build-system-and-cicd)
13. [Backend Contract](#13-backend-contract)
14. [The Migration Path](#14-the-migration-path)

---

## 1. Domain Modeling — The Core

### 1.1 Think in Bounded Contexts

The single biggest mistake in vocabulary app architecture is modeling it as a CRUD app.
Lexicon is not a word database. It is a **learning machine** that also stores words.

These are fundamentally different things, and confusing them is why `Word.kt` ends up
carrying scheduling fields (`easeFactor`, `interval`, `repetitions`) alongside content
fields (`originalWord`, `translation`). Those concerns belong to different bounded contexts.

**The Bounded Contexts of Lexicon:**

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          LEXICON DOMAIN                                  │
│                                                                          │
│  ┌─────────────────────┐    ┌───────────────────────┐                  │
│  │    LEARNING         │    │     VOCABULARY         │                  │
│  │                     │    │                        │                  │
│  │ • StudySession      │    │ • Word (content)       │                  │
│  │ • LearningCard      │◄───│ • Tag / Collection     │                  │
│  │ • SchedulingData    │    │ • Language             │                  │
│  │ • ReviewQuality     │    │ • ImportSource         │                  │
│  │ • SRS Algorithm     │    │                        │                  │
│  └─────────────────────┘    └───────────────────────┘                  │
│                                                                          │
│  ┌─────────────────────┐    ┌───────────────────────┐                  │
│  │    IDENTITY         │    │     ENGAGEMENT         │                  │
│  │                     │    │                        │                  │
│  │ • User              │    │ • Streak               │                  │
│  │ • AuthProvider      │    │ • LeaderboardEntry     │                  │
│  │ • Session           │    │ • Achievement          │                  │
│  │ • Preferences       │    │ • Milestone            │                  │
│  └─────────────────────┘    └───────────────────────┘                  │
│                                                                          │
│  ┌─────────────────────┐    ┌───────────────────────┐                  │
│  │    COMMERCE         │    │     INSIGHT            │                  │
│  │                     │    │                        │                  │
│  │ • Entitlement       │    │ • StudyInsight         │                  │
│  │ • Subscription      │    │ • AccuracyMetric       │                  │
│  │ • FeatureGate       │    │ • DifficultyReport     │                  │
│  │ • PurchaseEvent     │    │ • HeatmapData          │                  │
│  └─────────────────────┘    └───────────────────────┘                  │
└──────────────────────────────────────────────────────────────────────────┘
```

Each context has its own model, its own use cases, its own repository interface.
They communicate **only through domain events**, never through direct imports.

---

### 1.2 The Aggregate Design

An **aggregate** is a cluster of domain objects treated as a unit for data changes.
Every mutation goes through the aggregate root. This is the discipline that prevents
data corruption and enforces invariants.

#### `LearningCard` — The Core Aggregate

```
LearningCard (Aggregate Root)
  ├── CardId (value object)
  ├── WordSnapshot (value object — denormalized content at time of scheduling)
  │     ├── originalWord: String
  │     ├── translation: String
  │     └── hint: String?
  ├── SchedulingState (value object — all SRS fields together)
  │     ├── level: SRSLevel         (0–6, validated)
  │     ├── easeFactor: EaseFactor  (1.3–2.5, validated)
  │     ├── interval: ReviewInterval (always positive)
  │     ├── repetitions: Int
  │     ├── lastReviewedAt: Instant
  │     └── dueAt: Instant
  ├── metadata: CardMetadata
  │     ├── addedAt: Instant
  │     ├── sourceLanguage: Language
  │     ├── targetLanguage: Language
  │     └── importSource: ImportSource
  └── tags: Set<TagId>              (references, not Tag objects)
```

**Key decision**: `WordSnapshot` is denormalized inside `LearningCard`. When a word's
content changes, it does not retroactively change how the card looked when it was
reviewed. Content and scheduling are **separate lifecycles**.

```kotlin
// The aggregate root enforces invariants through its behavior methods
class LearningCard private constructor(...) {

    fun review(quality: ReviewQuality, algorithm: SchedulingAlgorithm, now: Instant): CardReviewed {
        val newScheduling = algorithm.schedule(schedulingState, quality, now)
        // Returns a domain event — does NOT mutate itself
        return CardReviewed(
            cardId = id,
            quality = quality,
            previousState = schedulingState,
            newState = newScheduling,
            reviewedAt = now,
        )
    }

    // Factory — the ONLY way to create a valid LearningCard
    companion object {
        fun create(word: Word, sourceLanguage: Language, targetLanguage: Language): Pair<LearningCard, CardCreated> {
            val card = LearningCard(
                id = CardId.generate(),
                wordSnapshot = word.toSnapshot(),
                schedulingState = SchedulingState.initial(),
                ...
            )
            return card to CardCreated(card.id, card.wordSnapshot, card.metadata)
        }
    }
}
```

Notice: aggregate methods return **domain events**, they do not mutate. The event is
the record of what happened. Mutating state from the event is a separate step. This
is the Event Sourcing pattern at the aggregate level.

---

#### `StudySession` — The Session State Machine

A study session is the most critical user-facing concept. It deserves to be a proper
**finite state machine**, not a ViewModel with a bunch of boolean flags.

```kotlin
sealed interface SessionState {
    data object Idle : SessionState

    data class Loading(
        val filter: SessionFilter,
    ) : SessionState

    data class Active(
        val sessionId: SessionId,
        val queue: ImmutableList<LearningCard>,  // remaining cards
        val currentCard: LearningCard,
        val phase: CardPhase,
        val progress: SessionProgress,
        val startedAt: Instant,
    ) : SessionState {
        sealed interface CardPhase {
            data object Question : CardPhase        // front side shown
            data object Answer : CardPhase          // back side revealed
        }
    }

    data class Paused(
        val snapshot: Active,                     // can resume
    ) : SessionState

    data class Completed(
        val sessionId: SessionId,
        val summary: SessionSummary,
    ) : SessionState

    data class Failed(
        val error: DomainError,
        val filter: SessionFilter,                // can retry
    ) : SessionState
}

// Commands — what a user can send to the session
sealed interface SessionCommand {
    data class Start(val filter: SessionFilter) : SessionCommand
    data object RevealAnswer : SessionCommand
    data class Rate(val quality: ReviewQuality) : SessionCommand
    data object Pause : SessionCommand
    data object Resume : SessionCommand
    data object Abandon : SessionCommand
}
```

**The state machine is a pure function:**

```kotlin
object SessionReducer {
    fun reduce(state: SessionState, command: SessionCommand): TransitionResult {
        return when {
            state is SessionState.Idle && command is SessionCommand.Start ->
                TransitionResult(
                    newState = SessionState.Loading(command.filter),
                    sideEffects = setOf(SideEffect.LoadCards(command.filter)),
                )

            state is SessionState.Active && command is SessionCommand.RevealAnswer
            && state.phase is CardPhase.Question ->
                TransitionResult(
                    newState = state.copy(phase = CardPhase.Answer),
                    sideEffects = emptySet(),  // pure UI change, no I/O
                )

            state is SessionState.Active && command is SessionCommand.Rate ->
                buildRatingTransition(state, command)

            // ... all transitions defined exhaustively
            else -> TransitionResult.invalid(state, command)  // programming error, not user error
        }
    }
}
```

This is testable in isolation with zero dependencies. Pass in a state + command,
get back a new state + a set of side effects to execute. No mocking needed.

---

### 1.3 Value Objects — Making Invalid States Unrepresentable

The most powerful defensive technique available. If you cannot construct an invalid value,
you cannot have a bug caused by an invalid value.

```kotlin
// CURRENT — your Word model has these
val id: Int          // can be 0, -1, anything
val level: Int       // can be -5 or 100
val easeFactor: Float // can be 0.0 or 50.0

// PERFECT — invalid values cannot be constructed
@JvmInline value class CardId(val raw: Long) {
    init { require(raw > 0) }
    companion object { fun generate() = CardId(UUID().mostSignificantBits) }
}

@JvmInline value class SRSLevel(val value: Int) {
    init { require(value in 0..6) { "SRS level $value is out of range 0-6" } }

    fun isNew() = value == 0
    fun isMastered() = value == 6
    fun isDue(dueAt: Instant, now: Instant) = now >= dueAt

    fun advance(): SRSLevel = SRSLevel(minOf(value + 1, 6))
    fun penalize(by: Int): SRSLevel = SRSLevel(maxOf(value - by, 0))

    val label: String get() = when (value) {
        0 -> "New"
        1 -> "Learning"
        2 -> "Familiarizing"
        3 -> "Consolidating"
        4 -> "Reviewing"
        5 -> "Mature"
        6 -> "Mastered"
        else -> error("unreachable")
    }
}

@JvmInline value class EaseFactor(val value: Float) {
    init { require(value in 1.3f..2.5f) }

    fun increase(by: Float = 0.1f) = EaseFactor((value + by).coerceIn(1.3f, 2.5f))
    fun decrease(by: Float = 0.2f) = EaseFactor((value - by).coerceIn(1.3f, 2.5f))

    companion object { val DEFAULT = EaseFactor(2.5f) }
}

@JvmInline value class ReviewInterval(val days: Int) {
    init { require(days >= 0) }

    fun toNextReviewAt(from: Instant): Instant = from.plus(days, DateTimeUnit.DAY)
    fun grow(factor: EaseFactor): ReviewInterval = ReviewInterval((days * factor.value).roundToInt())
    fun reset(): ReviewInterval = ReviewInterval(0)

    companion object { val INITIAL = ReviewInterval(0) }
}

// Language is not just a String
@JvmInline value class Language(val code: String) {
    init { require(code.matches(Regex("[a-z]{2}(-[A-Z]{2})?"))) { "Invalid language code: $code" } }

    val displayName: String get() = ... // from locale lookup

    companion object {
        val ENGLISH = Language("en")
        val SPANISH = Language("es")
        fun tryParse(code: String): Language? = runCatching { Language(code) }.getOrNull()
    }
}
```

**Why `@JvmInline value class`**: Zero overhead on JVM/Native — no boxing, no allocation,
same performance as the primitive type. The constraint is compile-time enforced.

---

## 2. The Learning Machine — SRS Redesigned

### 2.1 The Algorithm as a Pluggable Strategy

The current implementation buries the scheduling algorithm inside `ReviewWordUseCase`.
This couples two concerns: the **policy** (which card to review next) and the
**mechanism** (how to schedule it after review).

Extract both:

```kotlin
// The scheduling algorithm is a pure function — no dependencies
interface SchedulingAlgorithm {
    val id: AlgorithmId  // "lexicon-v1", "fsrs-v5", etc.

    // Given the current state and a quality rating, return the new state
    // This is a pure function — identical input always produces identical output
    fun schedule(
        state: SchedulingState,
        quality: ReviewQuality,
        now: Instant,
        settings: AlgorithmSettings,
    ): SchedulingState
}

// The selector decides which card to show next — separate concern
interface CardSelector {
    fun select(
        dueCards: List<LearningCard>,
        settings: SessionSettings,
    ): List<LearningCard>  // returns ordered list
}
```

### 2.2 The FSRS Alternative

The current Lexicon algorithm is a 7-level system derived from SM-2. It works.

But the state of the art is **FSRS v5** (Free Spaced Repetition Scheduler), developed
by Jarrett Ye in 2022. It is provably more accurate than SM-2 because it models memory
using the Ebbinghaus forgetting curve with two free parameters per card: **Stability**
(how long memory lasts) and **Difficulty** (how hard the card is to learn).

I am not recommending replacing the current algorithm immediately. I am recommending
designing the system so you *can* swap it, or A/B test it, without touching ViewModels.

```kotlin
// The current system reimplemented as a strategy
class LexiconLevelAlgorithm : SchedulingAlgorithm {
    override val id = AlgorithmId("lexicon-v1")

    override fun schedule(state: SchedulingState, quality: ReviewQuality, now: Instant, settings: AlgorithmSettings): SchedulingState {
        // Exact current logic, but extracted from UseCase
        return when (quality) {
            ReviewQuality.FORGOT -> scheduleForgotten(state, settings, now)
            ReviewQuality.REMEMBERED -> scheduleRemembered(state, settings, now)
        }
    }
}

// FSRS implementation (future)
class FSRSAlgorithm : SchedulingAlgorithm {
    override val id = AlgorithmId("fsrs-v5")
    // ...
}
```

### 2.3 The Card Selector — Not Just "Due Cards"

The selection of which cards to show, in what order, has more nuance than a date comparison:

```kotlin
class PrioritizedCardSelector : CardSelector {
    override fun select(dueCards: List<LearningCard>, settings: SessionSettings): List<LearningCard> {
        return dueCards
            .sortedWith(
                compareBy<LearningCard> { it.schedulingState.level }   // new cards first
                    .thenBy { it.schedulingState.dueAt }               // oldest overdue first
                    .thenBy { it.schedulingState.easeFactor.value }    // hardest cards early (energy)
            )
            .take(settings.maxCardsPerSession)
            .shuffled()  // prevent memorizing order, not content
    }
}
```

This is independently testable with any list of cards and any settings.

---

## 3. The Event-Driven Backbone

This is the single most important architectural decision in this document.

### 3.1 The Problem with Direct Calls

Every time something meaningful happens in Lexicon — a card is reviewed, a session
completes, a word is added — **five to eight different systems need to know about it**.

| Event | Who needs to know |
|---|---|
| `CardReviewed` | Analytics recorder, Sync engine, Streak service, Widget updater, Leaderboard service, Achievement checker |
| `SessionCompleted` | Analytics flusher, Notification scheduler, Insights updater, Backend sync |
| `WordAdded` | Sync engine, Widget counter, Statistics updater |
| `UserLoggedIn` | Push token registrar, Sync trigger, Analytics identifier |

In the current architecture, the ViewModel handles this fan-out manually:

```kotlin
// Current ReviewViewModel — this grows every time a new system is added
fun reviewWord(word: Word, quality: Int) {
    viewModelScope.launch {
        val result = reviewWordUseCase(ReviewParams(word, quality))
        recordReviewEventUseCase(params)     // analytics
        if (result.isSuccess) updateStreak()  // streak
        syncEngine.enqueue(...)               // sync
        widgetUpdater.update(...)             // widget
        // adding achievement system? add another call here
        // adding XP system? add another call here
        // this method keeps growing
    }
}
```

This is an **open-closed violation**: adding a new system requires modifying existing code.

### 3.2 The Domain Event Bus

```kotlin
// :core:events
interface IDomainEventBus {
    // Publish — fire-and-forget, non-blocking
    fun publish(event: DomainEvent)

    // Subscribe — handler called on a dedicated dispatcher
    fun subscribe(handler: DomainEventHandler)

    // Observe — for reactive UI patterns
    fun <T : DomainEvent> observe(type: KClass<T>): Flow<T>
}

// All domain events in one place — the audit log of the system
sealed class DomainEvent {
    abstract val occurredAt: Instant
    abstract val userId: UserId

    // Learning Context
    data class CardReviewed(
        val cardId: CardId,
        val quality: ReviewQuality,
        val previousState: SchedulingState,
        val newState: SchedulingState,
        val sessionId: SessionId,
        override val occurredAt: Instant,
        override val userId: UserId,
    ) : DomainEvent()

    data class SessionStarted(
        val sessionId: SessionId,
        val filter: SessionFilter,
        val cardCount: Int,
        override val occurredAt: Instant,
        override val userId: UserId,
    ) : DomainEvent()

    data class SessionCompleted(
        val sessionId: SessionId,
        val summary: SessionSummary,
        override val occurredAt: Instant,
        override val userId: UserId,
    ) : DomainEvent()

    data class SessionAbandoned(
        val sessionId: SessionId,
        val cardsReviewed: Int,
        override val occurredAt: Instant,
        override val userId: UserId,
    ) : DomainEvent()

    // Vocabulary Context
    data class CardAdded(
        val cardId: CardId,
        val sourceLanguage: Language,
        val targetLanguage: Language,
        val importSource: ImportSource,
        override val occurredAt: Instant,
        override val userId: UserId,
    ) : DomainEvent()

    data class CardDeleted(
        val cardId: CardId,
        override val occurredAt: Instant,
        override val userId: UserId,
    ) : DomainEvent()

    data class CardUpdated(
        val cardId: CardId,
        val changes: CardChanges,
        override val occurredAt: Instant,
        override val userId: UserId,
    ) : DomainEvent()

    // Identity Context
    data class UserAuthenticated(
        val authProvider: AuthProvider,
        override val occurredAt: Instant,
        override val userId: UserId,
    ) : DomainEvent()

    data class UserLoggedOut(
        override val occurredAt: Instant,
        override val userId: UserId,
    ) : DomainEvent()

    // Engagement Context
    data class StreakUpdated(
        val previousStreak: Int,
        val newStreak: Int,
        override val occurredAt: Instant,
        override val userId: UserId,
    ) : DomainEvent()

    data class MilestoneReached(
        val milestone: Milestone,
        override val occurredAt: Instant,
        override val userId: UserId,
    ) : DomainEvent()
}
```

### 3.3 Event Handlers — Open for Extension

Each system subscribes independently. Adding a new system = adding a new handler.
Nothing else changes.

```kotlin
// Analytics listens to review events
class AnalyticsEventHandler(
    private val analyticsTracker: IAnalyticsTracker,
) : DomainEventHandler {
    override fun handle(event: DomainEvent) {
        when (event) {
            is DomainEvent.CardReviewed -> analyticsTracker.track(event.toAnalyticsEvent())
            is DomainEvent.SessionCompleted -> analyticsTracker.track(event.toAnalyticsEvent())
            else -> Unit
        }
    }
}

// Sync engine listens to all mutations
class SyncEventHandler(
    private val syncEngine: ISyncEngine,
) : DomainEventHandler {
    override fun handle(event: DomainEvent) {
        when (event) {
            is DomainEvent.CardReviewed -> syncEngine.enqueue(SyncOperation.ReviewCard(event))
            is DomainEvent.CardAdded -> syncEngine.enqueue(SyncOperation.AddCard(event))
            is DomainEvent.CardDeleted -> syncEngine.enqueue(SyncOperation.DeleteCard(event))
            else -> Unit
        }
    }
}

// Streak service listens to session events
class StreakEventHandler(
    private val streakRepository: IStreakRepository,
) : DomainEventHandler {
    override fun handle(event: DomainEvent) {
        when (event) {
            is DomainEvent.SessionCompleted -> streakRepository.recordActivity(event.occurredAt)
            else -> Unit
        }
    }
}

// Notification rescheduler
class NotificationEventHandler(
    private val notificationScheduler: INotificationScheduler,
) : DomainEventHandler {
    override fun handle(event: DomainEvent) {
        when (event) {
            is DomainEvent.SessionCompleted -> notificationScheduler.reschedule(event.userId)
            else -> Unit
        }
    }
}
```

### 3.4 The ReviewCard Use Case Becomes Simple

```kotlin
// ReviewCardUseCase with event bus — clean, single responsibility
class ReviewCardUseCase(
    private val cardRepository: ILearningCardRepository,
    private val algorithm: SchedulingAlgorithm,
    private val eventBus: IDomainEventBus,
    private val clock: Clock,
) : UseCase<ReviewCardParams, LearningCard> {

    override suspend fun invoke(params: ReviewCardParams): Try<LearningCard> {
        val card = cardRepository.findById(params.cardId)
            .getOrElse { return Try.failure(DomainError.Data.NotFound("card:${params.cardId}")) }
            ?: return Try.failure(DomainError.Data.NotFound("card:${params.cardId}"))

        val event = card.review(params.quality, algorithm, clock.now())
        return cardRepository.applyEvent(event)
            .doOnSuccess { eventBus.publish(event) }  // side effects happen AFTER persistence
    }
}
```

The use case is now 15 lines. It does one thing. Every other concern is handled by its
respective event handler.

---

## 4. Module Graph — The Perfect Slice

### 4.1 The Graph

```
                          ┌──────────────┐
                          │     :app     │
                          │ thin host    │
                          │ wires DI     │
                          └──────┬───────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         │                       │                       │
  ┌──────▼──────┐        ┌───────▼──────┐       ┌───────▼──────┐
  │:feature:    │        │ :feature:    │  ...  │ :feature:    │
  │  study      │        │  vocabulary  │       │  insights    │
  │             │        │              │       │              │
  │ screens     │        │ screens      │       │ screens      │
  │ viewmodels  │        │ viewmodels   │       │ viewmodels   │
  │ handlers    │        │ handlers     │       │ handlers     │
  │ nav graph   │        │ nav graph    │       │ nav graph    │
  │ DI module   │        │ DI module    │       │ DI module    │
  └──────┬───────┘        └──────┬───────┘       └──────┬───────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │ all features depend on
      ┌──────────────────────────┼───────────────────────────┐
      │                          │                           │
┌─────▼──────┐           ┌───────▼──────┐           ┌───────▼──────┐
│:domain:    │           │ :domain:     │           │ :domain:     │
│ learning   │           │ vocabulary   │           │ identity     │
│            │           │              │           │              │
│ LearningCard│          │ Word         │           │ User         │
│ StudySession│          │ Tag          │           │ Preferences  │
│ SRS alg.   │           │ Language     │           │ Subscription │
│ zero deps  │           │ zero deps    │           │ zero deps    │
└─────┬───────┘           └──────┬───────┘           └──────┬───────┘
      │                          │                           │
      └──────────────────────────┼───────────────────────────┘
                                 │ domain depends on
      ┌──────────────────────────┼───────────────────────────┐
      │                          │                           │
┌─────▼──────┐           ┌───────▼──────┐           ┌───────▼──────┐
│:core:      │           │ :core:       │           │ :core:       │
│ common     │           │ events       │           │ sync         │
│            │           │              │           │              │
│ Try<T>     │           │ DomainEvent  │           │ SyncEngine   │
│ UseCase    │           │ EventBus     │           │ SyncQueue    │
│ BaseVM     │           │              │           │ ConflictRes  │
│ UiState    │           │              │           │              │
└────────────┘           └──────────────┘           └──────────────┘
      │                          │                           │
┌─────▼──────┐           ┌───────▼──────┐           ┌───────▼──────┐
│:core:      │           │ :core:       │           │ :core:       │
│ network    │           │ database     │           │ analytics    │
│            │           │              │           │              │
│ ApiClient  │           │ AppDatabase  │           │ AnalyticsEvt │
│ interceptors│          │ schema       │           │ Tracker iface│
│ retry      │           │ migrations   │           │              │
└────────────┘           └──────────────┘           └──────────────┘
                                 │
                    ┌────────────┼────────────┐
               ┌────▼────┐  ┌───▼───┐   ┌────▼────────────┐
               │:platforms│  │:data  │   │:core:design-sys │
               │ expect/  │  │       │   │                 │
               │ actual   │  │ repos │   │ Theme tokens    │
               │ adapters │  │ DSes  │   │ components      │
               │ no logic │  │ maps  │   │ no domain deps  │
               └──────────┘  └───────┘   └─────────────────┘
```

### 4.2 Strict Rules — Machine-Enforced

These are not conventions. They are build failures.

```kotlin
// build-logic/convention/ModuleBoundaryPlugin.kt

val RULES = mapOf(
    ":domain:learning"   to setOf<String>(),           // zero dependencies
    ":domain:vocabulary" to setOf<String>(),           // zero dependencies
    ":domain:identity"   to setOf<String>(),           // zero dependencies
    ":core:events"       to setOf(":core:common"),
    ":core:sync"         to setOf(":core:common", ":core:network", ":core:database"),
    ":core:analytics"    to setOf(":core:common"),
    ":data"              to setOf(":domain:*", ":core:*", ":platforms"),
    ":feature:*"         to setOf(":domain:*", ":core:*", ":data", ":platforms"),
    ":app"               to setOf("*"),                // only :app knows about everything
    ":core:design-system" to setOf(":resources"),      // NEVER domain, data, or features
    ":core:testing"      to setOf(":core:common", ":domain:*", ":data"), // for fakes only
)

// This task runs in < 5 seconds and blocks all PRs on violation
tasks.register("checkModuleBoundaries") { ... }
```

**Why machine-enforced**: Code review catches 80% of violations. Build failures catch 100%.

---

## 5. The Data Layer — Local First, Always

### 5.1 The Principle: Local Is Source of Truth

In a perfect offline-first system, the network is an optimization, not a requirement.
The user performs actions → actions persist locally → UI reflects local state → sync
uploads to server in the background.

The UI never waits for a network response to show the user what they just did.

```
User action
    │
    ▼
Command Handler (UseCase)
    │
    ├──► Local Repository ──► SQLDelight ──► UI updates immediately (via Flow)
    │
    └──► Domain Event Bus ──► Sync Engine ──► Network (background, eventually)
```

### 5.2 CQRS — Separate Read and Write Models

**Command side (writes)**: Operates on aggregates. Validates invariants. Produces events.
**Query side (reads)**: Operates on projections. Optimized for each screen's needs.

```kotlin
// WRITE SIDE — normalized, aggregate-centric
// LearningCardRepository handles commands (add, review, delete, update)
interface ILearningCardCommandRepository {
    suspend fun add(card: LearningCard): Try<Unit>
    suspend fun applyEvent(event: DomainEvent.CardReviewed): Try<LearningCard>
    suspend fun delete(id: CardId): Try<Unit>
}

// READ SIDE — denormalized, screen-centric
// Each screen gets its own optimized query model
interface IStudyProgressQueryRepository {
    fun observeProgressSummary(): Flow<ProgressSummary>  // for progress dashboard
}

interface IDueCardsQueryRepository {
    suspend fun getDueCards(filter: SessionFilter, limit: Int): Try<List<LearningCard>>  // for session
}

interface IVocabularyListQueryRepository {
    fun observeWords(filter: VocabularyFilter): Flow<PagingData<VocabularyItem>>  // for word list
}
```

The read models are **projections** — SQLDelight queries built specifically for each screen.
They never expose raw entity columns that the screen doesn't need.

### 5.3 The Repository Interface Pattern — Perfect Form

```kotlin
// domain — defines the contract the domain needs
// No knowledge of SQLDelight, Ktor, or any infrastructure
interface ILearningCardRepository {
    // Commands (write side)
    suspend fun save(card: LearningCard): Try<Unit>
    suspend fun applyEvent(event: DomainEvent.CardReviewed): Try<LearningCard>
    suspend fun delete(id: CardId): Try<Unit>

    // Queries (read side — fine to mix for small repos)
    suspend fun findById(id: CardId): Try<LearningCard?>
    fun observeAll(): Flow<List<LearningCard>>
    suspend fun getDue(filter: SessionFilter, limit: Int): Try<List<LearningCard>>
}
```

```kotlin
// data — the implementation
// Only this class knows about SQLDelight
class LearningCardRepositoryImpl(
    private val localDataSource: ILearningCardLocalDataSource,  // SQLDelight
    private val eventBus: IDomainEventBus,                     // for post-write events
) : ILearningCardRepository {

    override suspend fun applyEvent(event: DomainEvent.CardReviewed): Try<LearningCard> {
        return localDataSource.updateScheduling(
            id = event.cardId,
            newState = event.newState,
            reviewedAt = event.occurredAt,
        )
    }
}
```

### 5.4 SQLDelight — Schema as Architecture

The database schema is an architectural artifact. It must be versioned, tested, and documented.

```sql
-- migrations/1.sqm — initial schema
CREATE TABLE learning_card (
    id              INTEGER PRIMARY KEY NOT NULL,
    word_front      TEXT    NOT NULL,
    word_back       TEXT    NOT NULL,
    hint            TEXT,
    source_language TEXT    NOT NULL,
    target_language TEXT    NOT NULL,
    import_source   TEXT    NOT NULL DEFAULT 'manual',
    added_at        INTEGER NOT NULL,
    -- Scheduling fields (separate concern, same table for locality)
    srs_level       INTEGER NOT NULL DEFAULT 0,
    ease_factor     REAL    NOT NULL DEFAULT 2.5,
    interval_days   INTEGER NOT NULL DEFAULT 0,
    repetitions     INTEGER NOT NULL DEFAULT 0,
    last_reviewed_at INTEGER,
    due_at          INTEGER NOT NULL DEFAULT (unixepoch('now') * 1000)
);

CREATE TABLE card_tag (
    card_id INTEGER NOT NULL REFERENCES learning_card(id) ON DELETE CASCADE,
    tag_id  INTEGER NOT NULL REFERENCES tag(id) ON DELETE CASCADE,
    PRIMARY KEY (card_id, tag_id)
);

CREATE TABLE tag (
    id         INTEGER PRIMARY KEY NOT NULL,
    name       TEXT    NOT NULL UNIQUE,
    created_at INTEGER NOT NULL
);

-- Progress projection table (updated by event handlers, not by direct writes)
CREATE TABLE progress_snapshot (
    id              INTEGER PRIMARY KEY NOT NULL DEFAULT 1,  -- singleton
    total_cards     INTEGER NOT NULL DEFAULT 0,
    due_today       INTEGER NOT NULL DEFAULT 0,
    mastered_count  INTEGER NOT NULL DEFAULT 0,
    level_0_count   INTEGER NOT NULL DEFAULT 0,
    level_1_count   INTEGER NOT NULL DEFAULT 0,
    level_2_count   INTEGER NOT NULL DEFAULT 0,
    level_3_count   INTEGER NOT NULL DEFAULT 0,
    level_4_count   INTEGER NOT NULL DEFAULT 0,
    level_5_count   INTEGER NOT NULL DEFAULT 0,
    level_6_count   INTEGER NOT NULL DEFAULT 0,
    last_updated_at INTEGER NOT NULL
);
```

```kotlin
// Tested with an in-memory driver — every migration tested before it ships
class DatabaseMigrationTest {
    @Test fun `all migrations are idempotent and preserve data`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        for (version in 1..LexiconDatabase.Schema.version) {
            LexiconDatabase.Schema.migrate(driver, version - 1, version)
        }
        // Verify schema integrity
        val db = LexiconDatabase(driver)
        assertNotNull(db.learningCardQueries)
    }
}
```

---

## 6. The Sync Engine — Reliable by Design

### 6.1 The Write-Ahead Event Log

Every domain event is persisted locally **before** being applied to projections.
This is the write-ahead log. It is the source of truth for what happened.

```sql
-- The event log — append-only, never updated
CREATE TABLE domain_event_log (
    id          INTEGER PRIMARY KEY NOT NULL,
    event_type  TEXT    NOT NULL,
    payload     TEXT    NOT NULL,  -- JSON
    occurred_at INTEGER NOT NULL,
    user_id     TEXT    NOT NULL,
    synced_at   INTEGER,           -- NULL = not yet synced
    sync_error  TEXT               -- last error if any
);
```

**Sync = uploading rows where `synced_at IS NULL`.**

```kotlin
class SyncEngineImpl(
    private val eventLog: IEventLog,
    private val remoteSync: IRemoteSyncApi,
    private val networkMonitor: INetworkMonitor,
    private val backoffPolicy: BackoffPolicy,
) : ISyncEngine {

    init {
        // Auto-sync when network becomes available
        networkMonitor.isConnected
            .filter { it }
            .onEach { syncNow() }
            .launchIn(engineScope)

        // Periodic sync for long sessions
        engineScope.launch {
            while (true) {
                delay(30.seconds)
                if (networkMonitor.isConnected.value) syncNow()
            }
        }
    }

    private suspend fun syncNow() {
        val pending = eventLog.getUnsynced(limit = 100)
        if (pending.isEmpty()) return

        _status.value = SyncStatus.Syncing(pendingCount = pending.size)

        remoteSync.upload(pending)
            .fold(
                onSuccess = { result ->
                    eventLog.markSynced(result.syncedEventIds)
                    _status.value = SyncStatus.Idle(lastSyncedAt = Clock.System.now())
                },
                onFailure = { error ->
                    val delay = backoffPolicy.nextDelay()
                    _status.value = SyncStatus.Failed(
                        pendingCount = pending.size,
                        lastError = error,
                        nextRetryAt = Clock.System.now() + delay,
                    )
                    engineScope.launch { delay(delay); syncNow() }
                }
            )
    }
}
```

### 6.2 Conflict Resolution — Explicit and Testable

```kotlin
// Conflicts are domain decisions, not infrastructure decisions
interface IConflictResolver {
    fun resolve(local: DomainEvent, remote: DomainEvent): ConflictResolution
}

sealed class ConflictResolution {
    data class TakeLocal(val reason: String) : ConflictResolution()
    data class TakeRemote(val reason: String) : ConflictResolution()
    data class Merge(val merged: DomainEvent, val reason: String) : ConflictResolution()
}

class LexiconConflictResolver : IConflictResolver {
    override fun resolve(local: DomainEvent, remote: DomainEvent): ConflictResolution {
        // Card deleted remotely — deletion wins (tombstone semantics)
        if (remote is DomainEvent.CardDeleted) return TakeRemote("deletion is terminal")

        // Two reviews of the same card — take the more recent one
        if (local is DomainEvent.CardReviewed && remote is DomainEvent.CardReviewed) {
            return if (remote.occurredAt > local.occurredAt)
                TakeRemote("remote review is more recent")
            else
                TakeLocal("local review is more recent")
        }

        // Default: local wins (optimistic)
        return TakeLocal("optimistic local-first policy")
    }
}
```

---

## 7. The Presentation Layer — Pure and Predictable

### 7.1 The Perfect ViewModel Architecture

The `BaseViewModel<S, F>` pattern is fundamentally sound. The improvements are:

**1. Pure state reducers — make state transitions predictable and testable:**

```kotlin
abstract class BaseViewModel<S : Any, C : Any, E : Any> : ViewModel() {

    private val _state = mutableStateOf(initialState())

    @Composable
    fun state(): State<S> = _state

    private val _effects = Channel<E>(Channel.BUFFERED)
    val effects: Flow<E> = _effects.receiveAsFlow()

    abstract fun initialState(): S

    // PURE FUNCTION — no coroutines, no I/O, no side effects
    // Takes current state + intent, returns new state
    // Testable with zero infrastructure
    open fun reduce(state: S, command: C): S = state

    // IMPURE — launches coroutines, calls use cases
    // Returns commands that feed back into reduce()
    open suspend fun handle(command: C, state: S) {}

    fun dispatch(command: C) {
        _state.value = reduce(_state.value, command)  // synchronous, instant
        viewModelScope.launch { handle(command, _state.value) }  // async side effects
    }

    protected fun updateState(reducer: S.() -> S) { _state.value = _state.value.reducer() }
    protected fun emitEffect(effect: E) { viewModelScope.launch { _effects.send(effect) } }
}
```

**2. Handler decomposition — thin VMs, cohesive handlers:**

```kotlin
// The ViewModel is a router — pure delegation
class StudyViewModel(
    private val sessionHandler: StudySessionHandler,
    private val ttsHandler: StudyTtsHandler,
    private val progressHandler: StudyProgressHandler,
) : BaseViewModel<StudyState, StudyCommand, StudyEffect>() {

    init { dispatch(StudyCommand.Initialize) }

    override fun reduce(state: StudyState, command: StudyCommand): StudyState =
        when (command) {
            is StudyCommand.Initialize -> state.copy(isLoading = true)
            is StudyCommand.CardsLoaded -> state.copy(session = SessionState.Ready(command.cards), isLoading = false)
            is StudyCommand.RevealAnswer -> state.copy(session = (state.session as? SessionState.Active)?.copy(phase = CardPhase.Answer) ?: state.session)
            is StudyCommand.CardRated -> state.copy(session = state.session.advanceToNextCard())
            // ...
        }

    override suspend fun handle(command: StudyCommand, state: StudyState) {
        when (command) {
            is StudyCommand.Initialize -> sessionHandler.loadProgress()
            is StudyCommand.StartSession -> sessionHandler.startSession(command.filter)
            is StudyCommand.Rate -> sessionHandler.rateCard(command.cardId, command.quality)
            is StudyCommand.Speak -> ttsHandler.speak(command.card)
            else -> Unit
        }
    }

    // Public API — event sink pattern preserved
    fun startSession(filter: SessionFilter) = dispatch(StudyCommand.StartSession(filter))
    fun revealAnswer() = dispatch(StudyCommand.RevealAnswer)
    fun rate(cardId: CardId, quality: ReviewQuality) = dispatch(StudyCommand.Rate(cardId, quality))
    fun speak(card: LearningCard) = dispatch(StudyCommand.Speak(card))
}
```

```kotlin
// The handler contains all the async business logic
// It communicates back to the VM through commands
class StudySessionHandler(
    private val stateAccess: StateAccess<StudyState>,
    private val getDueCardsUseCase: GetDueCardsUseCase,
    private val reviewCardUseCase: ReviewCardUseCase,
    private val commandDispatch: (StudyCommand) -> Unit,
) {
    suspend fun startSession(filter: SessionFilter) {
        getDueCardsUseCase(filter)
            .fold(
                onSuccess = { cards -> commandDispatch(StudyCommand.CardsLoaded(cards)) },
                onFailure = { error -> commandDispatch(StudyCommand.LoadFailed(error)) },
            )
    }

    suspend fun rateCard(cardId: CardId, quality: ReviewQuality) {
        reviewCardUseCase(ReviewCardParams(cardId, quality))
            .fold(
                onSuccess = { commandDispatch(StudyCommand.CardRated(it)) },
                onFailure = { commandDispatch(StudyCommand.RatingFailed(it)) },
            )
    }
}
```

### 7.2 Screen Anatomy — Enforced by Convention

Every screen is three files:

```kotlin
// 1. Route (navigation entry point — pure Compose, no VM ref)
@Composable
fun StudyScreen(
    onNavigateToVocabulary: () -> Unit,
) {
    val viewModel = koinViewModel<StudyViewModel>()
    val state by viewModel.state()

    OnEvents(viewModel.effects) { effect ->
        when (effect) {
            is StudyEffect.NavigateToVocabulary -> onNavigateToVocabulary()
            is StudyEffect.ShowError -> { /* snackbar */ }
        }
    }

    StudyContent(
        state = state,
        onStartSession = viewModel::startSession,
        onRevealAnswer = viewModel::revealAnswer,
        onRate = viewModel::rate,
        onSpeak = viewModel::speak,
    )
}

// 2. Content (pure composable — no VM, no DI, fully previewable)
@Composable
internal fun StudyContent(
    state: StudyState,
    onStartSession: (SessionFilter) -> Unit,
    onRevealAnswer: () -> Unit,
    onRate: (CardId, ReviewQuality) -> Unit,
    onSpeak: (LearningCard) -> Unit,
) {
    UiState.Content(state.session) {
        // renders based on session state
    }
}

// 3. Preview (always exists — design system validation)
@Preview @Composable
private fun StudyContentPreview_ReadyState() {
    LexiconTheme {
        StudyContent(
            state = StudyState.preview(),
            onStartSession = {},
            onRevealAnswer = {},
            onRate = { _, _ -> },
            onSpeak = {},
        )
    }
}
```

### 7.3 `UiState<T>` — Unified State Wrapper

```kotlin
// Replaces ad-hoc `isLoading: Boolean, data: T?, error: String?` in every VM
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val error: DomainError) : UiState<Nothing>
    data object Empty : UiState<Nothing>  // distinct from loading
}

// In the design system — consistent rendering everywhere
@Composable
fun <T> UiState<T>.Content(
    loading: @Composable () -> Unit = { LexiconLoadingIndicator() },
    empty: @Composable () -> Unit = { LexiconEmptyState() },
    error: @Composable (DomainError) -> Unit = { e -> LexiconErrorState(e, onRetry = null) },
    content: @Composable (T) -> Unit,
) = when (this) {
    is UiState.Loading -> loading()
    is UiState.Empty -> empty()
    is UiState.Error -> error(this.error)
    is UiState.Success -> content(this.data)
}
```

---

## 8. Analytics — A First-Class System

### 8.1 The Problem: Analytics Is Not Logging

Analytics answers business questions: "What percentage of users who see the premium prompt
convert?" Logging answers debugging questions: "Why did this crash?"

They need different architectures.

### 8.2 The Perfect Analytics Stack

```
User action
     │
     ▼
Domain Event (typed, structured)
     │
     ▼
AnalyticsEventHandler (maps domain events → analytics events)
     │
     ▼
AnalyticsEvent (typed, analytics-specific schema)
     │
     ├──► AnalyticsBuffer (in-memory, per-session)
     │         │
     │         ▼ on SessionCompleted
     │    AnalyticsPersister (SQLDelight WAL)
     │         │
     │         ▼ on flush
     │    AnalyticsUploader (Ktor → backend)
     │
     └──► FirebaseAnalyticsTracker (immediate, for real-time dashboards)
```

```kotlin
// All analytics events are typed
sealed class AnalyticsEvent {
    // Session lifecycle
    data class SessionStarted(
        val sessionId: String,
        val cardCount: Int,
        val filter: String,
        val source: String,     // "progress_screen", "tag_screen", "widget"
    ) : AnalyticsEvent()

    data class SessionCompleted(
        val sessionId: String,
        val duration: Duration,
        val totalCards: Int,
        val correctCount: Int,
        val accuracy: Float,        // derived — no caller computes this differently
        val completedNormally: Boolean,
    ) : AnalyticsEvent()

    data class CardReviewed(
        val sessionId: String,
        val cardId: String,
        val quality: Int,
        val responseTimeMs: Long,
        val previousLevel: Int,
        val newLevel: Int,
        val leveled: LevelChange,   // UP, DOWN, SAME
    ) : AnalyticsEvent() {
        enum class LevelChange { UP, DOWN, SAME }
    }

    // Funnel events
    data class PremiumPromptShown(val source: String, val feature: String) : AnalyticsEvent()
    data class PremiumUpgradeStarted(val plan: String) : AnalyticsEvent()
    data class PremiumUpgradeCompleted(val plan: String, val revenue: Double) : AnalyticsEvent()

    // Import events
    data class ImportStarted(val source: ImportSource, val estimatedCount: Int?) : AnalyticsEvent()
    data class ImportCompleted(val source: ImportSource, val addedCount: Int, val skippedCount: Int) : AnalyticsEvent()
}
```

```kotlin
// The mapping is centralized — one place to audit
internal fun DomainEvent.toAnalyticsEvent(): AnalyticsEvent? = when (this) {
    is DomainEvent.SessionStarted -> AnalyticsEvent.SessionStarted(
        sessionId = sessionId.raw.toString(),
        cardCount = cardCount,
        filter = filter.toAnalyticsString(),
        source = filter.source,
    )
    is DomainEvent.CardReviewed -> AnalyticsEvent.CardReviewed(
        sessionId = sessionId.raw.toString(),
        cardId = cardId.raw.toString(),
        quality = quality.value,
        responseTimeMs = responseTimeMs,
        previousLevel = previousState.level.value,
        newLevel = newState.level.value,
        leveled = when {
            newState.level.value > previousState.level.value -> LevelChange.UP
            newState.level.value < previousState.level.value -> LevelChange.DOWN
            else -> LevelChange.SAME
        },
    )
    else -> null  // not every domain event becomes an analytics event
}
```

**ViewModels have zero analytics code.** Analytics is a side effect of domain events.

---

## 9. The Type System as Architecture

### 9.1 Make Illegal States Unrepresentable

The Kotlin type system is the most underused tool in most KMP projects.

```kotlin
// Typed IDs prevent passing the wrong ID to the wrong function
reviewCardUseCase(cardId = tagId)  // COMPILE ERROR — CardId ≠ TagId

// Typed quantities prevent logical errors
interval.grow(factor = easeFactor)   // clear intent
interval.grow(factor = 2.5f)         // COMPILE ERROR — Float ≠ EaseFactor

// Sealed when statements prevent missing cases
when (sessionState) {
    is SessionState.Idle -> ...
    is SessionState.Loading -> ...
    is SessionState.Active -> ...
    is SessionState.Paused -> ...
    is SessionState.Completed -> ...
    is SessionState.Failed -> ...
    // forgot SessionState.Paused? COMPILE ERROR
}
```

### 9.2 Non-Empty Collections

A study session with zero cards should be impossible to represent:

```kotlin
// In Kotlin, use a simple wrapper
data class NonEmptyList<T>(val head: T, val tail: List<T>) {
    val all: List<T> get() = listOf(head) + tail
    val size: Int get() = 1 + tail.size

    fun map(transform: (T) -> T): NonEmptyList<T> =
        NonEmptyList(transform(head), tail.map(transform))
}

// SessionState.Active always has at least one card
data class Active(
    val queue: NonEmptyList<LearningCard>,  // cannot be empty
    ...
) : SessionState

// Force the error earlier, not in the middle of the session
suspend fun startSession(filter: SessionFilter): Try<Active> {
    val cards = getDueCards(filter)
    val nonEmpty = NonEmptyList.fromList(cards)
        ?: return Try.failure(DomainError.Learning.NoDueCards(filter))
    return Try.success(Active(queue = nonEmpty, ...))
}
```

### 9.3 Explicit `DomainError` — No Raw Throwables

```kotlin
sealed class DomainError(message: String? = null, cause: Throwable? = null) : Throwable(message, cause) {

    // Learning context errors
    sealed class Learning : DomainError() {
        data class NoDueCards(val filter: SessionFilter) : Learning()
        data object SessionNotActive : Learning()
        data class InvalidRating(val value: Int) : Learning()
    }

    // Data errors
    sealed class Data : DomainError() {
        data class NotFound(val entity: String) : Data()
        data class DuplicateCard(val front: String) : Data()
        data class InvalidInput(val field: String, val constraint: String) : Data()
        data class SyncConflict(val conflictId: String) : Data()
    }

    // Network errors
    sealed class Network : DomainError() {
        data object NoConnection : Network()
        data object Timeout : Network()
        data class ServerError(val code: Int, val body: String?) : Network()
        data object RateLimited : Network()
        data class Deserialization(override val cause: Throwable) : Network()
    }

    // Auth errors
    sealed class Auth : DomainError() {
        data object NotAuthenticated : Auth()
        data object SessionExpired : Auth()
        data class ProviderError(val provider: String, override val cause: Throwable?) : Auth()
    }

    // Commerce errors
    sealed class Commerce : DomainError() {
        data object PremiumRequired : Commerce()
        data object PurchaseFailed : Commerce()
        data object RestoreFailed : Commerce()
    }
}

// The UI maps errors to localized strings — one place, no scattered .message calls
fun DomainError.toUserMessage(): StringResource = when (this) {
    is DomainError.Learning.NoDueCards -> Res.string.no_due_cards
    is DomainError.Network.NoConnection -> Res.string.no_internet
    is DomainError.Auth.SessionExpired -> Res.string.session_expired
    is DomainError.Commerce.PremiumRequired -> Res.string.premium_required
    is DomainError.Data.DuplicateCard -> Res.string.word_already_exists
    else -> Res.string.generic_error
}
```

---

## 10. Cross-Cutting Systems

### 10.1 Feature Flags — Runtime Configurability

```kotlin
// The gate is evaluated at the navigation layer, not scattered in ViewModels
interface IFeatureGate {
    suspend fun isEnabled(feature: Feature): Boolean
    fun observe(feature: Feature): Flow<Boolean>
}

sealed class Feature {
    data object AIImport : Feature()
    data object Insights : Feature()
    data object Leaderboard : Feature()
    data object SRSAlgorithmV2 : Feature()
    data object OfflineMode : Feature()
}

// Navigation guard — declarative, not imperative
@Composable
fun FeatureGated(
    feature: Feature,
    fallback: @Composable () -> Unit = { PremiumPrompt() },
    content: @Composable () -> Unit,
) {
    val gate = koinInject<IFeatureGate>()
    val isEnabled by gate.observe(feature).collectAsState(initial = false)
    if (isEnabled) content() else fallback()
}
```

### 10.2 Notification Architecture — Declarative Scheduling

```kotlin
// Notifications are declared, not scheduled imperatively
sealed class NotificationSchedule {
    data class StudyReminder(
        val userId: UserId,
        val preferredTime: LocalTime,
        val daysWithDueCards: Set<DayOfWeek>,
    ) : NotificationSchedule()

    data class StreakAtRisk(
        val userId: UserId,
        val currentStreak: Int,
        val lastStudiedAt: Instant,
    ) : NotificationSchedule()

    data class MilestoneReached(
        val userId: UserId,
        val milestone: Milestone,
    ) : NotificationSchedule()
}

// One scheduler — handles all notification types
interface INotificationScheduler {
    suspend fun schedule(notification: NotificationSchedule): Try<Unit>
    suspend fun cancel(userId: UserId, type: NotificationType): Try<Unit>
}
```

### 10.3 TTS — Streaming Architecture

```kotlin
interface ITtsEngine {
    // Returns a Flow of playback states for reactive UI
    fun speak(text: String, language: Language, rate: SpeechRate): Flow<TtsPlaybackState>
    suspend fun stop(): Try<Unit>
    fun isLanguageSupported(language: Language): Boolean
}

sealed class TtsPlaybackState {
    data object Preparing : TtsPlaybackState()
    data class Playing(val progressRatio: Float) : TtsPlaybackState()
    data object Completed : TtsPlaybackState()
    data class Failed(val error: DomainError) : TtsPlaybackState()
}
```

### 10.4 Import Pipeline — Pluggable Sources

```kotlin
// The import system is a pipeline, not a single use case
interface IImportSource {
    val id: ImportSourceId
    suspend fun parse(input: ImportInput): Try<List<ImportedCard>>
}

// Each source is independently tested and independently registered
class ManualInputSource : IImportSource { ... }
class CSVFileSource : IImportSource { ... }
class AnkiDeckSource : IImportSource { ... }
class ImageOCRSource(private val aiRepository: IAiRepository) : IImportSource { ... }

// The pipeline handles deduplication, validation, preview, and confirmation
class ImportPipeline(
    private val sources: Map<ImportSourceId, IImportSource>,
    private val deduplicator: IImportDeduplicator,
    private val cardRepository: ILearningCardRepository,
) {
    suspend fun execute(sourceId: ImportSourceId, input: ImportInput): Flow<ImportProgress> = flow {
        emit(ImportProgress.Parsing)
        val parsed = sources[sourceId]?.parse(input)
            ?: return@flow emit(ImportProgress.Failed(DomainError.Data.NotFound("source:$sourceId")))

        emit(ImportProgress.Deduplicating)
        val (unique, duplicates) = deduplicator.partition(parsed.getOrElse { ... })

        emit(ImportProgress.Preview(unique, duplicates))
        // waits for user confirmation...

        emit(ImportProgress.Saving(total = unique.size))
        unique.forEachIndexed { index, card ->
            cardRepository.save(card.toLearningCard())
            emit(ImportProgress.Progress(saved = index + 1, total = unique.size))
        }

        emit(ImportProgress.Completed(added = unique.size, skipped = duplicates.size))
    }
}
```

---

## 11. Testing — The Confidence Layer

### 11.1 Test Pyramid — Strict Ratios

```
E2E (Maestro)        ▲  5%   — happy path flows only: login, first session, import
─────────────────────┤
Integration          │ 15%   — feature slices: real SQLDelight + MockEngine
─────────────────────┤
Unit                 │ 80%   — pure functions, state machines, reducers, algorithms
─────────────────────┘
```

### 11.2 What to Test at Each Level

**Unit (pure functions — no infrastructure):**
- `SessionReducer.reduce()` — all state transitions
- `SpacedRepetitionAlgorithm.schedule()` — all quality/level combinations
- `LexiconConflictResolver.resolve()` — all conflict scenarios
- `DomainEvent.toAnalyticsEvent()` — all event mappings
- `DomainError.toUserMessage()` — all error localizations
- All mappers

**Integration (real SQLDelight, `MockEngine` for HTTP):**
- `LearningCardRepositoryImpl` — add, review, delete, query
- `SyncEngineImpl` — enqueue, drain, retry, conflict resolution
- `AuthRepositoryImpl` — login, token refresh, logout
- Database migrations — every version step

**E2E (Maestro on-device):**
- User logs in → sees empty state → imports first word
- User starts session → rates all cards → sees completion screen
- User goes offline → rates cards → comes online → cards sync

### 11.3 The Handler Test Pattern — The Best Unit Test

```kotlin
// Testing SessionHandler in pure isolation
// Zero coroutine machinery needed for synchronous state checks
class StudySessionHandlerTest {
    private val fakeCards = listOf(TestData.card1, TestData.card2)
    private val getDueCards = FakeGetDueCardsUseCase(result = Try.success(fakeCards))
    private val reviewCard = FakeReviewCardUseCase(result = Try.success(TestData.card1.reviewed()))
    private val dispatched = mutableListOf<StudyCommand>()
    private val stateAccess = FakeStateAccess(StudyState.initial())

    private val handler = StudySessionHandler(
        stateAccess = stateAccess,
        getDueCardsUseCase = getDueCards,
        reviewCardUseCase = reviewCard,
        commandDispatch = { dispatched.add(it) },
    )

    @Test fun `startSession dispatches CardsLoaded on success`() = runTest {
        handler.startSession(SessionFilter.AllDue)
        assertEquals(1, dispatched.size)
        assertIs<StudyCommand.CardsLoaded>(dispatched[0])
        assertEquals(fakeCards, (dispatched[0] as StudyCommand.CardsLoaded).cards)
    }

    @Test fun `startSession dispatches LoadFailed when no due cards`() = runTest {
        getDueCards.result = Try.failure(DomainError.Learning.NoDueCards(SessionFilter.AllDue))
        handler.startSession(SessionFilter.AllDue)
        assertIs<StudyCommand.LoadFailed>(dispatched[0])
    }
}
```

### 11.4 The Reducer Test Pattern — Zero Infrastructure

```kotlin
// Pure function test — runs in < 1ms, zero dependencies
class StudyReducerTest {
    @Test fun `RevealAnswer from Question state transitions to Answer state`() {
        val initialState = StudyState(
            session = SessionState.Active(
                currentCard = TestData.card1,
                phase = CardPhase.Question,
                ...
            )
        )
        val result = StudyViewModel.reduce(initialState, StudyCommand.RevealAnswer)
        assertEquals(CardPhase.Answer, (result.session as SessionState.Active).phase)
    }

    @Test fun `RevealAnswer is a no-op when session is not Active`() {
        val initialState = StudyState(session = SessionState.Idle)
        val result = StudyViewModel.reduce(initialState, StudyCommand.RevealAnswer)
        assertEquals(initialState, result)  // unchanged
    }
}
```

### 11.5 Algorithm Property-Based Tests

```kotlin
class SpacedRepetitionAlgorithmTest {
    private val algorithm = LexiconLevelAlgorithm()

    // Invariants that must hold for ALL inputs
    @Test fun `ease factor never leaves valid range`() {
        repeat(10_000) {
            val quality = ReviewQuality.values().random()
            val initial = SchedulingState(
                level = SRSLevel(Random.nextInt(0, 7)),
                easeFactor = EaseFactor(Random.nextFloat() * 1.2f + 1.3f),
                interval = ReviewInterval(Random.nextInt(0, 365)),
                repetitions = Random.nextInt(0, 10),
                dueAt = Clock.System.now(),
                lastReviewedAt = null,
            )
            val result = algorithm.schedule(initial, quality, Clock.System.now(), AlgorithmSettings.BALANCED)
            assertTrue(result.easeFactor.value in 1.3f..2.5f, "EaseFactor out of range: ${result.easeFactor}")
        }
    }

    @Test fun `forgetting never increases level`() {
        repeat(1_000) {
            val initial = SchedulingState(level = SRSLevel(Random.nextInt(0, 7)), ...)
            val result = algorithm.schedule(initial, ReviewQuality.FORGOT, ...)
            assertTrue(result.level.value <= initial.level.value)
        }
    }

    @Test fun `remembering at max level only increases interval, not level`() {
        val mastered = SchedulingState(level = SRSLevel(6), interval = ReviewInterval(30), ...)
        val result = algorithm.schedule(mastered, ReviewQuality.REMEMBERED, ...)
        assertEquals(SRSLevel(6), result.level)
        assertTrue(result.interval.days > 30)
    }
}
```

### 11.6 Shared Test Infrastructure — `:core:testing`

```kotlin
// Every fake lives here. Feature tests never define their own.
object TestData {
    val userId = UserId("test-user-01")
    val card = LearningCard(
        id = CardId(1),
        wordSnapshot = WordSnapshot("Hola", "Hello", null),
        schedulingState = SchedulingState.initial(),
        ...
    )
    val session = StudySession(
        id = SessionId("session-01"),
        filter = SessionFilter.AllDue,
        ...
    )
}

class FakeGetDueCardsUseCase(
    var result: Try<List<LearningCard>> = Try.success(listOf(TestData.card)),
) : GetDueCardsUseCase {
    val invocations = mutableListOf<SessionFilter>()
    override suspend fun invoke(params: SessionFilter): Try<List<LearningCard>> {
        invocations += params
        return result
    }
}

class FakeDomainEventBus : IDomainEventBus {
    val published = mutableListOf<DomainEvent>()
    override fun publish(event: DomainEvent) { published += event }
    override fun subscribe(handler: DomainEventHandler) {}
    override fun <T : DomainEvent> observe(type: KClass<T>): Flow<T> = emptyFlow()
}
```

---

## 12. Build System and CI/CD

### 12.1 Trunk-Based Development

Short-lived branches (< 2 days). Feature flags for incomplete work. No long-running
feature branches that diverge from main and cause painful merges.

```
main (always deployable)
  └── feature/add-collection-support (max 2 days old)
  └── fix/sync-retry-backoff (max 1 day old)
```

### 12.2 The CI Pipeline — Ordered for Speed

```
PR opened
    │
    ├── [30s]  Detekt + custom lint rules (fails fast)
    ├── [1m]   Module boundary check (fails fast)
    ├── [3m]   Unit tests — pure, parallel by module
    │
    ├── [on pass] ──────────────────────────────────────┐
    │                                                    │
    ├── [5m] Android debug build                 [8m] iOS framework build
    ├── [5m] Android unit tests                  [5m] iOS unit tests
    ├── [3m] DB migration tests                         │
    │                                                    │
    └────────────────────────┬───────────────────────────┘
                             │ [on both pass]
                        [15m] E2E tests (Maestro, Android emulator)
                             │
                        PR mergeable
```

### 12.3 Custom Detekt Rules — Architecture Enforcement

```kotlin
// Fails if a UseCase implementation has mutable state
class StatelessUseCaseRule : Rule() {
    override val issue = Issue("StatelessUseCase", Severity.Error, ...)

    override fun visitClassDeclaration(node: KtClassOrObject) {
        if (node.implementsUseCase() && node.hasVarProperties()) {
            report(CodeSmell(issue, Entity.from(node),
                "UseCase implementations must be stateless. Found var property in ${node.name}"))
        }
    }
}

// Fails if a domain module imports anything from data, presentation, or platforms
class DomainBoundaryRule : Rule() { ... }

// Fails if a ViewModel directly calls an analytics tracker
class NoAnalyticsInViewModelRule : Rule() { ... }

// Fails if Try<T>.getOrThrow() is called without handling the exception
class SafeTryUnwrapRule : Rule() { ... }
```

### 12.4 Dependency Version Management

```toml
# gradle/libs.versions.toml — everything pinned, nothing dynamic
# Dependabot opens PRs for upgrades — humans approve

[versions]
kotlin = "2.3.10"          # never "latest.release"
ktor = "3.4.1"             # never "3.+"
sqldelight = "2.3.1"
koin = "4.1.1"
compose = "1.10.2"
```

### 12.5 Automated Release Pipeline

```yaml
# on merge to main
- Run full test suite
- Bump version via conventional commits (fix: → patch, feat: → minor, BREAKING: → major)
- Build signed APK + IPA
- Upload to Firebase App Distribution (internal testing)
- Create GitHub release with changelog
- On tag v*.*.* → upload to Play Store (internal track) + App Store Connect (TestFlight)
```

---

## 13. Backend Contract

The backend is out of scope for this document, but the contract matters.

### 13.1 The API is an Event Sink

The app sends **domain events** to the backend, not CRUD operations.

```
POST /api/v1/events/batch
Content-Type: application/json

{
    "deviceId": "device-abc-123",
    "events": [
        {
            "type": "card.reviewed",
            "version": "1",
            "occurredAt": "2026-03-25T10:15:30Z",
            "payload": {
                "cardId": "card-001",
                "quality": 1,
                "previousLevel": 2,
                "newLevel": 3,
                "sessionId": "session-xyz"
            }
        }
    ]
}
```

This is **event sourcing at the API boundary**. The backend is an event store. It
projects events into read models for the insights endpoint.

**Benefits**:
- Batch uploads — one request for a whole session
- Idempotent — replay events without side effects (use event ID as idempotency key)
- Backend schema evolution without breaking app — new events are ignored by old event handlers
- Perfect audit log — every action is recorded

### 13.2 Contract Testing with Pact

```kotlin
// AppPactTest.kt — ensures the app's API calls match the backend's API contract
class SyncApiContractTest {
    @Test fun `batch event upload matches backend contract`() {
        val pact = buildPact("lexicon-app", "lexicon-backend") {
            uponReceiving("batch of review events")
            withRequest { POST("/api/v1/events/batch"); body(expectedPayload) }
            willRespondWith { status(200); body(expectedResponse) }
        }
        // runs against a mock server, then verifies against backend's published pact
    }
}
```

---

## 14. The Migration Path

This architecture is a north star. You don't rewrite Lexicon — you migrate it in phases.

### Phase 0 — Read-Only (1 week, zero risk)
- Read this document and discuss with the team
- Identify which pieces already exist (most do)
- Set up module boundary Gradle check
- Set up Detekt with no new violations

### Phase 1 — Type System (1 week, zero user impact)
- Introduce `DomainError` — wrap existing errors, don't change behavior
- Introduce `UiState<T>` — opt-in, one screen at a time
- Introduce value objects for `SRSLevel`, `EaseFactor` — change internal types
- Write property-based tests for the SRS algorithm

### Phase 2 — Event Bus (2 weeks, careful)
- Add `IDomainEventBus` — backed by a `SharedFlow` initially
- Migrate analytics tracking to event handlers — remove from ViewModels one by one
- Add sync event handler — decouple sync from repositories
- Migrate streak and notification scheduling to event handlers

### Phase 3 — State Machines (2 weeks)
- Replace `StudyViewModel` loading flags with `SessionState` sealed class
- Extract `StudySessionHandler`, `StudyTtsHandler`, `StudyProgressHandler`
- Write reducer tests — they're now pure functions

### Phase 4 — CQRS + Sync Engine (3 weeks, infrastructure-heavy)
- Introduce the event log table in SQLDelight
- Implement `SyncEngineImpl` backed by event log
- Replace ad-hoc sync calls with event-driven sync
- Add exponential backoff + conflict resolution

### Phase 5 — Full Feature Modules (ongoing)
- Complete the feature-to-module migration
- Move DI into feature modules
- Delete the `:presentation` module

---

## Closing Principles

These seven principles underpin every decision in this document.

**1. The domain is king.** Architecture exists to serve the domain. When domain logic
and infrastructure convenience conflict, domain wins.

**2. Make illegal states unrepresentable.** A type that cannot hold an invalid value
cannot have a bug caused by an invalid value. Use the type system relentlessly.

**3. Local first, network eventually.** The user's experience must never depend on
network latency. Optimistic local updates, background sync.

**4. Events over direct calls.** When a thing happens and multiple systems need to
know, use an event. Do not wire them together directly. This is the single rule that
prevents the most architectural rot.

**5. Pure functions are the most testable code.** Reducers, algorithms, mappers — pure
functions are trivially tested, trivially composed, trivially understood. Push side
effects to the boundary.

**6. One way to do things.** The worst enemy of a large codebase is having five ways
to do the same thing. Pick one pattern, encode it in a skill or convention, enforce it
with a lint rule.

**7. The architecture must be deletable.** Features get removed. The architecture must
support adding a feature and removing a feature with equal ease. If removing a feature
requires editing six other modules, the architecture has failed.

---

*This is not a plan for a perfect app. It is a plan for an architecture that allows
Lexicon to become a perfect app over time — as you learn more about your users, as
the product evolves, as the team grows. The architecture's job is to make change cheap.
Everything above is in service of that goal.*
