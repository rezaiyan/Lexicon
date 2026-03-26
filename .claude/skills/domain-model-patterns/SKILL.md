---
name: domain-model-patterns
description: Design domain models — value objects, aggregates, pure data classes, domain events, and the rules that keep :domain free of all framework dependencies
argument-hint: "<domain concept to model>"
user-invocable: true
allowed-tools: ["Read", "Write", "Edit", "Glob", "Grep"]
---

# Lexicon Domain Model Patterns

Domain is the heart of the app. It has zero dependencies on Ktor, SQLDelight, Koin, Compose, or any platform code. If you find yourself importing any of those, you're in the wrong layer.

---

## The Absolute Rule

```
:domain must NEVER import from:
  data, presentation, platforms, core, composeApp
  Ktor, SQLDelight, Koin, Compose, Android, iOS APIs
```

Domain models are pure Kotlin. No annotations except `@Suppress` when needed.

---

## 1. Plain Domain Models (most common)

Data classes with only standard Kotlin types:

```kotlin
// domain/src/commonMain/kotlin/domain/word/model/Word.kt
data class Word(
    val id: Int,
    val originalWord: String,
    val translation: String,
    val description: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val level: Int,           // 0–6 SRS bucket — never use raw Int in logic, validate at boundaries
    val easeFactor: Float,    // 1.3–2.5
    val interval: Int,        // days; must be > 0 at level 6
    val repetitions: Int,
    val lastReviewDate: Long,
    val nextReviewDate: Long,
    val dateAdded: Long,
    val tagIds: List<Long> = emptyList(),
)
```

**Rules:**
- No `@Serializable`, `@Entity`, `@Parcelize` on domain models — those belong in data layer DTOs/entities
- No nullable fields unless `null` carries real domain meaning (not "loading" or "unset")
- Prefer `Long` for timestamps (epoch millis), `String` for language codes, `Int` for DB IDs

---

## 2. Value Objects (for concepts with invariants)

Use `@JvmInline value class` when the underlying type isn't enough — when you need validation, methods, or type safety:

```kotlin
// SRS level with enforced bounds and behavior
@JvmInline
value class SRSLevel(val value: Int) {
    init { require(value in 0..6) { "SRS level must be 0–6, got $value" } }

    fun isNew(): Boolean = value == 0
    fun isMastered(): Boolean = value == 6
    fun advance(): SRSLevel = SRSLevel(minOf(value + 1, 6))
    fun penalize(by: Int = 2): SRSLevel = SRSLevel(maxOf(value - by, 0))
}

// Ease factor with bounds
@JvmInline
value class EaseFactor(val value: Float) {
    init { require(value in 1.3f..2.5f) { "Ease factor must be 1.3–2.5, got $value" } }

    fun increase(by: Float = 0.1f): EaseFactor = EaseFactor(minOf(value + by, 2.5f))
    fun decrease(by: Float = 0.2f): EaseFactor = EaseFactor(maxOf(value - by, 1.3f))
}

// Type-safe IDs — prevents passing wordId where userId is expected
@JvmInline value class WordId(val value: Long)
@JvmInline value class UserId(val value: String)
@JvmInline value class TagId(val value: Long)
@JvmInline value class SessionId(val value: String)
```

**When to use value objects:**
- The raw type (Int, String, Float) has domain rules (e.g., 0..6, must be positive)
- The value has behavior (`.advance()`, `.isMastered()`)
- You need compile-time prevention of swapping similar types (WordId vs UserId)

**When NOT to use:**
- Simple data bags — use data class
- Things that are just stored and displayed — plain type is fine

---

## 3. Enumerations for Fixed Domains

```kotlin
enum class ReviewQuality { FORGOT, REMEMBERED }

enum class ReviewPreset(
    val successesToAdvance: Int,
    val forgotPenalty: Int,
) {
    EASY(successesToAdvance = 1, forgotPenalty = 1),
    BALANCED(successesToAdvance = 2, forgotPenalty = 2),
    RIGOROUS(successesToAdvance = 3, forgotPenalty = 2),
    EXPERT(successesToAdvance = 3, forgotPenalty = 3),
}

enum class SubscriptionStatus { FREE, TRIAL, ACTIVE, EXPIRED, CANCELLED }
```

---

## 4. Sealed Interfaces for State Machines

When a concept has mutually exclusive states with different data, use `sealed interface`:

```kotlin
// domain/src/commonMain/kotlin/domain/study/model/SessionState.kt
sealed interface SessionState {
    data object Idle : SessionState

    data class Loading(val filter: SessionFilter) : SessionState

    data class Active(
        val sessionId: SessionId,
        val queue: List<Word>,   // remaining cards
        val currentCard: Word,
        val phase: CardPhase,
        val progress: SessionProgress,
        val startedAt: Long,
    ) : SessionState

    data class Paused(val snapshot: Active) : SessionState

    data class Completed(
        val sessionId: SessionId,
        val summary: SessionSummary,
    ) : SessionState

    data class Failed(
        val error: DomainError,
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

## 5. Domain Events (Target Architecture)

Domain events capture "something happened" — they enable decoupled reactions without direct calls:

```kotlin
// domain/src/commonMain/kotlin/domain/event/DomainEvent.kt
sealed class DomainEvent {
    abstract val occurredAt: Long
    abstract val userId: UserId

    data class CardReviewed(
        override val userId: UserId,
        override val occurredAt: Long,
        val wordId: WordId,
        val sessionId: SessionId,
        val quality: ReviewQuality,
        val previousLevel: SRSLevel,
        val newLevel: SRSLevel,
        val responseTimeMs: Long,
    ) : DomainEvent()

    data class SessionStarted(
        override val userId: UserId,
        override val occurredAt: Long,
        val sessionId: SessionId,
        val filter: SessionFilter,
        val totalCards: Int,
    ) : DomainEvent()

    data class SessionCompleted(
        override val userId: UserId,
        override val occurredAt: Long,
        val sessionId: SessionId,
        val summary: SessionSummary,
    ) : DomainEvent()

    data class CardAdded(
        override val userId: UserId,
        override val occurredAt: Long,
        val wordId: WordId,
        val word: String,
        val translation: String,
    ) : DomainEvent()

    data class StreakUpdated(
        override val userId: UserId,
        override val occurredAt: Long,
        val newStreak: Int,
        val longestStreak: Int,
    ) : DomainEvent()
}
```

---

## 6. Repository Interfaces (in :domain)

Repository interfaces live in domain. They depend only on domain types:

```kotlin
// domain/src/commonMain/kotlin/domain/word/repository/IWordRepository.kt
interface IWordRepository {
    // Streams — reactive, never fail directly (errors surface in VM via .catch)
    fun getAllWords(): Flow<List<Word>>
    fun getDueCards(): Flow<List<Word>>

    // Commands — suspend, always Try<T>
    suspend fun updateWord(word: Word): Try<Word>
    suspend fun deleteWord(id: Int): Try<Unit>
    suspend fun insertWords(words: List<Word>): Try<Int>
    suspend fun syncWithRemote(): Try<Unit>

    // Non-fallible queries
    suspend fun getWordById(id: Int): Word?
    suspend fun getTotalCount(): Int
}
```

---

## 7. Data Source Interfaces (in :domain)

Data sources are also abstracted in domain so repositories depend on interfaces:

```kotlin
// domain/src/commonMain/kotlin/domain/word/datasource/IWordRemoteDataSource.kt
interface IWordRemoteDataSource {
    suspend fun fetchWords(): List<WordDto>
    suspend fun addWords(words: List<WordDto>): List<WordDto>
    suspend fun updateWord(word: WordDto): WordDto
    suspend fun deleteWord(id: Int)
    suspend fun deleteWords(ids: List<Int>)
}
```

**Note:** `WordDto` is in `:data` module. If the interface references it, it creates a coupling. For strict domain purity, define domain-level request/response types or use domain models directly in the interface. Check existing patterns in the codebase for current convention.

---

## 8. Mapper Extension Functions (in :data)

Mappers convert between data layer types (DTOs, DB entities) and domain models. Always in the data layer:

```kotlin
// data/src/commonMain/kotlin/data/word/mapper/WordMappers.kt

// DTO → Domain
fun WordDto.toDomain(): Word = Word(
    id = id,
    originalWord = originalWord,
    translation = translation,
    level = level.coerceIn(0, 6),        // sanitize at boundary
    easeFactor = easeFactor.coerceIn(1.3f, 2.5f),  // enforce invariants at entry
    interval = interval.coerceAtLeast(1), // never allow 0 interval
    // ...
)

// DB Entity → Domain
fun WordEntity.toDomain(): Word = Word(
    id = id.toInt(),
    originalWord = originalWord,
    translation = translation,
    level = level.toInt().coerceIn(0, 6),
    // ...
)

// Domain → DTO (for writes)
fun Word.toDto(): WordDto = WordDto(
    id = id,
    originalWord = originalWord,
    // ...
)
```

**Rules:**
- Always sanitize/coerce values at the boundary (DTO → Domain is the trust boundary)
- Mapper functions are extension functions, not classes
- Named `toDomain()`, `toDto()`, `toEntity()` — always consistent naming
- Never call mappers from domain layer — only from data layer

---

## File Organization

```
domain/src/commonMain/kotlin/domain/
├── word/
│   ├── model/
│   │   └── Word.kt
│   ├── repository/
│   │   └── IWordRepository.kt
│   ├── datasource/
│   │   └── IWordRemoteDataSource.kt
│   │   └── IWordLocalDataSource.kt
│   └── usecase/
│       ├── GetDueWordsUseCase.kt
│       ├── ReviewWordUseCase.kt
│       └── ...
├── study/
│   ├── model/
│   │   ├── SessionState.kt
│   │   └── SessionSummary.kt
│   └── ...
├── analytics/
│   ├── model/
│   │   └── StudyInsights.kt
│   └── ...
└── shared/
    ├── DomainError.kt
    └── DomainEvent.kt
```

---

## Checklist

1. Zero imports from: data, presentation, platforms, core, Ktor, SQLDelight, Koin, Compose
2. Domain models are plain `data class` with standard Kotlin types
3. Value objects use `@JvmInline value class` with `init { require(...) }`
4. Sealed interfaces for mutually exclusive states with different data
5. Repository interfaces: suspend → `Try<T>`, stream → `Flow<T>`, non-fallible → `T?`
6. Mappers are extension functions in `:data`, named `toDomain()` / `toDto()` / `toEntity()`
7. Sanitize values at the DTO→Domain boundary (coerce, clamp)
8. Domain events are `sealed class DomainEvent` with `occurredAt` and `userId`
