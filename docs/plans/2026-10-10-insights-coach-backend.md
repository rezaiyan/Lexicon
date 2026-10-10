# Insights Coach — Backend Implementation Plan

> **For agentic workers:** Use claude-kit-v2:implement to execute this plan task-by-task.

**Goal:** Add `GET /api/v1/analytics/insights-screen?tz=<IANA>` to `lexicon.server`, returning hero, ranked coach cards, deep-dive sections and locked teasers computed in the user's local time zone.

**Architecture:** One request loads a `LearnerSnapshot` (last 84 days of review facts + word level counts + streak + settings + Word Rush summary). Pure functions compute everything from it in the requested `ZoneId`. Coach cards come from independent `CoachRule` Spring beans evaluated by `CoachEngine`; each rule and each section builder is isolated so one failure never breaks the endpoint.

**Tech Stack:** Spring Boot 3.5, Kotlin, Spring Data JPA, JUnit 5, MockK (unit), Mockito `@MockitoBean` (controller tests, existing convention), Testcontainers PostgreSQL (integration).

**Spec:** `~/projects/Lexicon/docs/specs/2026-10-10-insights-coach-design.md`

**Deviations from spec (deliberate, simpler):**
- Timezone fix is done by computing from raw review facts in Kotlin with a `ZoneId`, instead of adding `ZoneId` to every existing SQL query. Old endpoints stay untouched (they only serve the old screen, which goes away after the client flag flips).
- Hero week is ISO Monday–Sunday (matches existing `WeeklyReportService`). Client renders weekday labels in the user's locale.
- `BEST_TIME` card text does not contain the hour; the client formats `action.hour` in device locale (display rule 4).

**Repo:** all paths below are relative to `~/projects/lexicon.server`. Build needs JDK 21: prefix Gradle commands with `JAVA_HOME=$(/usr/libexec/java_home -v 21)`. Docker must be running for integration tests.

Shorthand used below:
- `MAIN = src/main/kotlin/com/alirezaiyan/vokab/server`
- `TEST = src/test/kotlin/com/alirezaiyan/vokab/server`
- `PKG = com.alirezaiyan.vokab.server.analytics.insightsscreen`

---

## File map

| File | Responsibility |
|---|---|
| `MAIN/analytics/Projections.kt` (modify) | add `ReviewFactProjection` |
| `MAIN/analytics/ReviewEventRepository.kt` (modify) | add `findFactsByUserIdSince` |
| `MAIN/wordrush/WordRushGameRepository.kt` (modify) | add `findBestScore` |
| `MAIN/analytics/insightsscreen/LearnerSnapshot.kt` | snapshot + value types |
| `MAIN/analytics/insightsscreen/SnapshotMath.kt` | pure derived metrics, sample gates |
| `MAIN/analytics/insightsscreen/InsightsScreenDtos.kt` | response contract |
| `MAIN/analytics/insightsscreen/CoachRule.kt` | rule interface + card helper |
| `MAIN/analytics/insightsscreen/CoachEngine.kt` | evaluate, isolate, rank, cap |
| `MAIN/analytics/insightsscreen/rules/HabitRules.kt` | `StreakAtRiskRule`, `BestTimeRule` |
| `MAIN/analytics/insightsscreen/rules/WordRules.kt` | `SlippingWordsRule`, `DifficultWordsRule`, `LevelBottleneckRule`, `ComebackWinRule` |
| `MAIN/analytics/insightsscreen/rules/ProgressRules.kt` | `MilestoneNearRule`, `WeekTrendRule` |
| `MAIN/analytics/insightsscreen/HeroBuilder.kt` | hero block |
| `MAIN/analytics/insightsscreen/SectionBuilders.kt` | deep-dive sections + locked |
| `MAIN/analytics/insightsscreen/LearnerSnapshotLoader.kt` | DB → snapshot |
| `MAIN/analytics/insightsscreen/InsightsScreenService.kt` | assemble response |
| `MAIN/analytics/insightsscreen/InsightsScreenController.kt` | HTTP + tz parsing |
| `TEST/analytics/insightsscreen/SnapshotFixtures.kt` | test builders |
| `TEST/analytics/insightsscreen/*Test.kt` | one test class per unit |

---

### Task 1: Review fact query + Word Rush best score

**Files:**
- Modify: `MAIN/analytics/Projections.kt`
- Modify: `MAIN/analytics/ReviewEventRepository.kt`
- Modify: `MAIN/wordrush/WordRushGameRepository.kt`

Covered by the integration test in Task 10 (repository queries need a real DB).

- [ ] **Step 1: Add projection** — append to `Projections.kt`:

```kotlin
interface ReviewFactProjection {
    val wordId: Long
    val wordText: String
    val rating: Int
    val previousLevel: Int
    val newLevel: Int
    val reviewedAt: Long
}
```

- [ ] **Step 2: Add query** — inside `interface ReviewEventRepository`:

```kotlin
    /** Raw review facts since [sinceMs], oldest first. Feeds the zone-aware insights screen. */
    @Query(
        """SELECT e.wordId AS wordId, e.wordText AS wordText, e.rating AS rating,
                  e.previousLevel AS previousLevel, e.newLevel AS newLevel, e.reviewedAt AS reviewedAt
           FROM ReviewEvent e
           WHERE e.user.id = :userId AND e.reviewedAt >= :sinceMs
           ORDER BY e.reviewedAt"""
    )
    fun findFactsByUserIdSince(@Param("userId") userId: Long, @Param("sinceMs") sinceMs: Long): List<ReviewFactProjection>
```

- [ ] **Step 3: Add best score** — inside `interface WordRushGameRepository`:

```kotlin
    @Query("SELECT COALESCE(MAX(g.score), 0) FROM WordRushGame g WHERE g.user = :user")
    fun findBestScore(@Param("user") user: User): Int
```

(Add `import org.springframework.data.repository.query.Param` if missing.)

- [ ] **Step 4: Compile**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew compileKotlin -q`
Expected: BUILD SUCCESSFUL, no output.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/analytics/Projections.kt \
        src/main/kotlin/com/alirezaiyan/vokab/server/analytics/ReviewEventRepository.kt \
        src/main/kotlin/com/alirezaiyan/vokab/server/wordrush/WordRushGameRepository.kt
git commit -m "feat(analytics): add review fact and word rush best score queries"
```

---

### Task 2: Snapshot model + pure metrics

**Files:**
- Create: `MAIN/analytics/insightsscreen/LearnerSnapshot.kt`
- Create: `MAIN/analytics/insightsscreen/SnapshotMath.kt`
- Create: `TEST/analytics/insightsscreen/SnapshotFixtures.kt`
- Test: `TEST/analytics/insightsscreen/SnapshotMathTest.kt`

- [ ] **Step 1: Write fixtures** — `SnapshotFixtures.kt`:

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen

import java.time.ZoneId
import java.time.ZonedDateTime

/** Wednesday 2026-06-17 20:00 in Berlin. This week = Mon 06-15 … Sun 06-21. */
val BERLIN: ZoneId = ZoneId.of("Europe/Berlin")
val TEST_LOCAL_NOW: ZonedDateTime = ZonedDateTime.of(2026, 6, 17, 20, 0, 0, 0, BERLIN)

fun snapshot(
    now: ZonedDateTime = TEST_LOCAL_NOW,
    reviews: List<ReviewFact> = emptyList(),
    totalReviewsAllTime: Long = reviews.size.toLong(),
    wordsPerLevel: Map<Int, Long> = emptyMap(),
    masteredTotal: Long = wordsPerLevel[MASTERED_LEVEL] ?: 0,
    currentStreak: Int = 0,
    remindersEnabled: Boolean = false,
    difficultWords: List<WordAccuracy> = emptyList(),
    comebackWords: List<WordRef> = emptyList(),
    wordRush: WordRushSummary? = null,
) = LearnerSnapshot(
    zone = now.zone,
    now = now,
    reviews = reviews.sortedBy { it.reviewedAt },
    totalReviewsAllTime = totalReviewsAllTime,
    wordsPerLevel = wordsPerLevel,
    masteredTotal = masteredTotal,
    currentStreak = currentStreak,
    remindersEnabled = remindersEnabled,
    difficultWords = difficultWords,
    comebackWords = comebackWords,
    wordRush = wordRush,
)

/** [localDateTime] is ISO local date-time in [zone], e.g. "2026-06-16T09:00". */
fun review(
    localDateTime: String,
    wordId: Long = 1,
    correct: Boolean = true,
    previousLevel: Int = 1,
    newLevel: Int = if (correct) previousLevel + 1 else previousLevel - 1,
    zone: ZoneId = BERLIN,
    text: String = "word$wordId",
) = ReviewFact(
    wordId = wordId,
    wordText = text,
    correct = correct,
    previousLevel = previousLevel,
    newLevel = newLevel.coerceIn(0, MASTERED_LEVEL),
    reviewedAt = java.time.LocalDateTime.parse(localDateTime).atZone(zone).toInstant().toEpochMilli(),
)

/** [count] reviews at [localDateTime], [correctCount] of them correct, distinct word ids from [firstWordId]. */
fun reviews(localDateTime: String, count: Int, correctCount: Int, firstWordId: Long = 100, previousLevel: Int = 1): List<ReviewFact> =
    (0 until count).map { i ->
        review(localDateTime, wordId = firstWordId + i, correct = i < correctCount, previousLevel = previousLevel)
    }
```

- [ ] **Step 2: Write the failing test** — `SnapshotMathTest.kt`:

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class SnapshotMathTest {

    @Test
    fun `this week and last week are ISO weeks in the user's zone`() {
        val s = snapshot()
        assertEquals(Period(LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 21)), s.thisWeek())
        assertEquals(Period(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 14)), s.lastWeek())
    }

    @Test
    fun `a review late Sunday UTC counts as Monday in Tokyo`() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        // 2026-06-14T20:00Z = Mon 2026-06-15 05:00 in Tokyo
        val fact = review("2026-06-15T05:00", zone = tokyo)
        val s = snapshot(now = ZonedDateTime.of(2026, 6, 17, 12, 0, 0, 0, tokyo), reviews = listOf(fact))
        assertEquals(1, s.reviewsIn(s.thisWeek()).size)
        assertEquals(0, s.reviewsIn(s.lastWeek()).size)
    }

    @Test
    fun `accuracy is null below the sample gate and rounded above it`() {
        assertNull(reviews("2026-06-16T09:00", count = 19, correctCount = 19).gatedAccuracyPct())
        assertEquals(67, reviews("2026-06-16T09:00", count = 30, correctCount = 20).gatedAccuracyPct())
    }

    @Test
    fun `leveled up counts distinct words that moved up`() {
        val facts = listOf(
            review("2026-06-16T09:00", wordId = 1, correct = true),
            review("2026-06-16T10:00", wordId = 1, correct = true),
            review("2026-06-16T11:00", wordId = 2, correct = false),
        )
        assertEquals(1, facts.leveledUpWordCount())
    }

    @Test
    fun `reviews by date groups in local time`() {
        val s = snapshot(reviews = listOf(review("2026-06-16T23:30"), review("2026-06-17T00:30")))
        assertEquals(mapOf(LocalDate.of(2026, 6, 16) to 1, LocalDate.of(2026, 6, 17) to 1), s.reviewsByDate())
    }

    @Test
    fun `hour and weekday buckets use local time`() {
        val s = snapshot(reviews = listOf(review("2026-06-16T20:15")))
        assertEquals(setOf(20), s.hourBuckets().keys)
        assertEquals(setOf(DayOfWeek.TUESDAY), s.weekdayBuckets().keys)
    }

    @Test
    fun `next milestone uses fixed steps then every 500`() {
        assertEquals(50, nextMilestone(0))
        assertEquals(100, nextMilestone(50))
        assertEquals(1000, nextMilestone(999))
        assertEquals(1500, nextMilestone(1000))
        assertEquals(2000, nextMilestone(1700))
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*insightsscreen.SnapshotMathTest"`
Expected: FAIL — compilation errors, unresolved `LearnerSnapshot`, `Period`, etc.

- [ ] **Step 4: Implement** — `LearnerSnapshot.kt`:

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

const val MASTERED_LEVEL = 6

data class ReviewFact(
    val wordId: Long,
    val wordText: String,
    val correct: Boolean,
    val previousLevel: Int,
    val newLevel: Int,
    val reviewedAt: Long,
)

data class WordRef(val id: Long, val text: String)

data class WordAccuracy(val id: Long, val text: String, val accuracyPct: Int, val reviews: Int)

data class WordRushSummary(val gamesPlayed: Long, val bestScore: Int, val recentScores: List<Int>)

/** Everything the insights screen needs, loaded once per request. Times are interpreted in [zone]. */
data class LearnerSnapshot(
    val zone: ZoneId,
    val now: ZonedDateTime,
    /** Review facts from the last [HISTORY_DAYS] local days, oldest first. */
    val reviews: List<ReviewFact>,
    val totalReviewsAllTime: Long,
    val wordsPerLevel: Map<Int, Long>,
    val masteredTotal: Long,
    val currentStreak: Int,
    val remindersEnabled: Boolean,
    val difficultWords: List<WordAccuracy>,
    val comebackWords: List<WordRef>,
    val wordRush: WordRushSummary?,
) {
    val today: LocalDate get() = now.toLocalDate()
    val weekStart: LocalDate get() = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun localDate(fact: ReviewFact): LocalDate = Instant.ofEpochMilli(fact.reviewedAt).atZone(zone).toLocalDate()
    fun localHour(fact: ReviewFact): Int = Instant.ofEpochMilli(fact.reviewedAt).atZone(zone).hour

    companion object {
        /** 12 weeks: the heatmap window and the history every rule reads from. */
        const val HISTORY_DAYS = 84L
    }
}
```

`SnapshotMath.kt`:

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt

/** Minimum reviews before any percentage is shown. */
const val MIN_REVIEWS_FOR_PERCENT = 20

/** Minimum reviews in one hour/weekday bucket before claiming anything about it. */
const val MIN_REVIEWS_PER_BUCKET = 30

private val FIXED_MILESTONES = listOf(50L, 100L, 250L, 500L, 1000L)
private const val MILESTONE_STEP = 500L

data class Period(val start: LocalDate, val endInclusive: LocalDate) {
    operator fun contains(date: LocalDate): Boolean = date in start..endInclusive
}

fun LearnerSnapshot.thisWeek(): Period = Period(weekStart, weekStart.plusDays(6))

fun LearnerSnapshot.lastWeek(): Period = Period(weekStart.minusWeeks(1), weekStart.minusDays(1))

fun LearnerSnapshot.lastDays(days: Long): Period = Period(today.minusDays(days - 1), today)

fun LearnerSnapshot.reviewsIn(period: Period): List<ReviewFact> = reviews.filter { localDate(it) in period }

fun LearnerSnapshot.reviewsByDate(): Map<LocalDate, Int> = reviews.groupingBy { localDate(it) }.eachCount()

fun LearnerSnapshot.hourBuckets(): Map<Int, List<ReviewFact>> = reviews.groupBy { localHour(it) }

fun LearnerSnapshot.weekdayBuckets(): Map<DayOfWeek, List<ReviewFact>> = reviews.groupBy { localDate(it).dayOfWeek }

/** Reviews grouped by the stage the word was in when reviewed. */
fun LearnerSnapshot.stageBuckets(): Map<Int, List<ReviewFact>> = reviews.groupBy { it.previousLevel }

fun List<ReviewFact>.accuracyPct(): Int? =
    if (isEmpty()) null else (count { it.correct } * 100.0 / size).roundToInt()

fun List<ReviewFact>.gatedAccuracyPct(minReviews: Int = MIN_REVIEWS_FOR_PERCENT): Int? =
    if (size < minReviews) null else accuracyPct()

fun List<ReviewFact>.leveledUpWordCount(): Int = filter { it.newLevel > it.previousLevel }.map { it.wordId }.distinct().size

fun List<ReviewFact>.demotedWordCount(): Int = filter { it.newLevel < it.previousLevel }.map { it.wordId }.distinct().size

fun nextMilestone(mastered: Long): Long =
    FIXED_MILESTONES.firstOrNull { it > mastered } ?: ((mastered / MILESTONE_STEP) + 1) * MILESTONE_STEP
```

- [ ] **Step 5: Run test to verify it passes**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*insightsscreen.SnapshotMathTest"`
Expected: PASS (7 tests).

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/analytics/insightsscreen src/test/kotlin/com/alirezaiyan/vokab/server/analytics/insightsscreen
git commit -m "feat(insights-screen): add zone-aware learner snapshot and metrics"
```

---

### Task 3: Response contract

**Files:**
- Create: `MAIN/analytics/insightsscreen/InsightsScreenDtos.kt`

Pure data; exercised by every later test.

- [ ] **Step 1: Write DTOs**

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen

import com.fasterxml.jackson.annotation.JsonInclude

data class InsightsScreenResponse(
    val generatedAt: String,
    val totalReviews: Long,
    val hero: HeroDto,
    val coach: List<CoachCardDto>,
    val sections: SectionsDto,
    val locked: List<LockedSectionDto>,
)

data class MetricDto(val value: Int, val previous: Int)

data class DayCountDto(val date: String, val reviews: Int)

data class HeroDto(
    val headline: String,
    val reviews: MetricDto,
    /** Null below [MIN_REVIEWS_FOR_PERCENT] reviews this week. */
    val accuracyPct: MetricDto?,
    val leveledUp: MetricDto,
    val currentStreak: Int,
    /** Monday … Sunday of the current local week; future days have 0 reviews. */
    val week: List<DayCountDto>,
)

data class WordChipDto(val id: Long, val text: String)

object CoachActionKind {
    const val REVIEW_WORDS = "REVIEW_WORDS"
    const val START_REVIEW = "START_REVIEW"
    const val ENABLE_REMINDER = "ENABLE_REMINDER"
    const val START_WORD_RUSH = "START_WORD_RUSH"
    const val NONE = "NONE"
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CoachActionDto(
    val kind: String,
    val label: String? = null,
    val wordIds: List<Long> = emptyList(),
    val hour: Int? = null,
)

data class CoachCardDto(
    /** Stable per type per local day; the client uses it for dismissal. */
    val id: String,
    val type: String,
    val priority: Int,
    val title: String,
    val body: String,
    val words: List<WordChipDto>,
    val moreWordsCount: Int,
    val action: CoachActionDto,
)

data class SectionsDto(
    val mastery: MasterySectionDto?,
    val habits: HabitsSectionDto?,
    val words: WordsSectionDto?,
    val wordRush: WordRushSectionDto?,
)

data class LevelCountDto(val level: Int, val words: Long)

data class MasterySectionDto(
    val caption: String,
    val levels: List<LevelCountDto>,
    val promotedThisWeek: Int,
    val demotedThisWeek: Int,
)

data class BestHourDto(val hour: Int, val accuracyPct: Int)

data class WeekdayAccuracyDto(val isoDay: Int, val accuracyPct: Int, val reviews: Int)

data class HabitsSectionDto(
    val caption: String,
    /** Only days with at least one review; the client fills gaps. */
    val heatmap: List<DayCountDto>,
    val bestHour: BestHourDto?,
    val weekdays: List<WeekdayAccuracyDto>,
)

data class WordAccuracyDto(val id: Long, val text: String, val accuracyPct: Int)

data class WordsSectionDto(
    val caption: String,
    val hardest: List<WordAccuracyDto>,
    val comebacks: List<WordChipDto>,
)

data class WordRushSectionDto(
    val caption: String,
    val gamesPlayed: Long,
    val bestScore: Int,
    /** Oldest first, at most 5. */
    val recentScores: List<Int>,
)

object SectionKey {
    const val HABITS = "habits"
}

data class LockedSectionDto(val section: String, val reviewsNeeded: Int)
```

- [ ] **Step 2: Compile**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew compileKotlin -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/analytics/insightsscreen/InsightsScreenDtos.kt
git commit -m "feat(insights-screen): add response contract"
```

---

### Task 4: CoachRule + CoachEngine

**Files:**
- Create: `MAIN/analytics/insightsscreen/CoachRule.kt`
- Create: `MAIN/analytics/insightsscreen/CoachEngine.kt`
- Test: `TEST/analytics/insightsscreen/CoachEngineTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CoachEngineTest {

    private class FixedRule(override val type: String, override val priority: Int, private val fires: Boolean = true) : CoachRule {
        override fun evaluate(snapshot: LearnerSnapshot): CoachCardDto? =
            if (fires) card(snapshot, title = type, body = "", action = CoachActionDto(CoachActionKind.NONE)) else null
    }

    private class ThrowingRule : CoachRule {
        override val type = "BOOM"
        override val priority = 1000
        override fun evaluate(snapshot: LearnerSnapshot): CoachCardDto? = error("broken rule")
    }

    @Test
    fun `returns at most three cards ordered by priority`() {
        val engine = CoachEngine(listOf(FixedRule("LOW", 10), FixedRule("TOP", 100), FixedRule("MID", 50), FixedRule("MID2", 40)))
        assertEquals(listOf("TOP", "MID", "MID2"), engine.cardsFor(snapshot()).map { it.type })
    }

    @Test
    fun `skips rules that do not fire`() {
        val engine = CoachEngine(listOf(FixedRule("A", 10, fires = false), FixedRule("B", 5)))
        assertEquals(listOf("B"), engine.cardsFor(snapshot()).map { it.type })
    }

    @Test
    fun `a throwing rule is skipped, not fatal`() {
        val engine = CoachEngine(listOf(ThrowingRule(), FixedRule("OK", 1)))
        assertEquals(listOf("OK"), engine.cardsFor(snapshot()).map { it.type })
    }

    @Test
    fun `card id is type plus local date and words are capped at five`() {
        val rule = FixedRule("X", 1)
        val words = (1L..8L).map { WordRef(it, "w$it") }
        val card = rule.card(snapshot(), "t", "b", CoachActionDto(CoachActionKind.NONE), words)
        assertEquals("X:2026-06-17", card.id)
        assertEquals(5, card.words.size)
        assertEquals(3, card.moreWordsCount)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*insightsscreen.CoachEngineTest"`
Expected: FAIL — unresolved `CoachRule`, `CoachEngine`.

- [ ] **Step 3: Implement** — `CoachRule.kt`:

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen

private const val MAX_WORD_CHIPS = 5

/** One coaching insight. Implementations are stateless Spring beans; add a rule = add a class. */
interface CoachRule {
    val type: String
    /** Higher shows first. */
    val priority: Int
    fun evaluate(snapshot: LearnerSnapshot): CoachCardDto?
}

fun CoachRule.card(
    snapshot: LearnerSnapshot,
    title: String,
    body: String,
    action: CoachActionDto,
    words: List<WordRef> = emptyList(),
) = CoachCardDto(
    id = "$type:${snapshot.today}",
    type = type,
    priority = priority,
    title = title,
    body = body,
    words = words.take(MAX_WORD_CHIPS).map { WordChipDto(it.id, it.text) },
    moreWordsCount = (words.size - MAX_WORD_CHIPS).coerceAtLeast(0),
    action = action,
)

fun plural(count: Int, one: String, many: String = "${one}s"): String = "$count ${if (count == 1) one else many}"
```

`CoachEngine.kt`:

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class CoachEngine(private val rules: List<CoachRule>) {

    fun cardsFor(snapshot: LearnerSnapshot): List<CoachCardDto> = rules
        .sortedByDescending { it.priority }
        .mapNotNull { rule ->
            runCatching { rule.evaluate(snapshot) }
                .onFailure { logger.warn(it) { "Coach rule ${rule.type} failed; skipping" } }
                .getOrNull()
        }
        .take(MAX_CARDS)

    companion object {
        const val MAX_CARDS = 3
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*insightsscreen.CoachEngineTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/analytics/insightsscreen src/test/kotlin/com/alirezaiyan/vokab/server/analytics/insightsscreen
git commit -m "feat(insights-screen): add coach rule engine"
```

---

### Task 5: Habit rules (STREAK_AT_RISK, BEST_TIME)

**Files:**
- Create: `MAIN/analytics/insightsscreen/rules/HabitRules.kt`
- Test: `TEST/analytics/insightsscreen/rules/HabitRulesTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen.rules

import com.alirezaiyan.vokab.server.analytics.insightsscreen.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HabitRulesTest {

    private val streak = StreakAtRiskRule()
    private val bestTime = BestTimeRule()

    // TEST_LOCAL_NOW = Wed 2026-06-17 20:00 Berlin

    @Test
    fun `streak at risk fires in the evening when yesterday studied and today not`() {
        val card = streak.evaluate(snapshot(currentStreak = 6, reviews = listOf(review("2026-06-16T09:00"))))
        assertNotNull(card)
        assertEquals("Keep your 6-day streak alive", card!!.title)
        assertEquals(CoachActionKind.START_REVIEW, card.action.kind)
    }

    @Test
    fun `streak at risk is silent before 18h`() {
        val morning = TEST_LOCAL_NOW.withHour(10)
        assertNull(streak.evaluate(snapshot(now = morning, currentStreak = 6, reviews = listOf(review("2026-06-16T09:00")))))
    }

    @Test
    fun `streak at risk is silent when already studied today`() {
        val facts = listOf(review("2026-06-16T09:00"), review("2026-06-17T08:00"))
        assertNull(streak.evaluate(snapshot(currentStreak = 6, reviews = facts)))
    }

    @Test
    fun `streak at risk is silent without a streak`() {
        assertNull(streak.evaluate(snapshot(currentStreak = 0, reviews = listOf(review("2026-06-16T09:00")))))
    }

    @Test
    fun `best time fires for an hour 10pt above overall with reminders off`() {
        val facts = reviews("2026-06-10T20:00", count = 30, correctCount = 28) +       // 93% at 20h
            reviews("2026-06-11T09:00", count = 30, correctCount = 18, firstWordId = 500) // 60% at 9h
        val card = bestTime.evaluate(snapshot(reviews = facts, remindersEnabled = false))
        assertNotNull(card)
        assertEquals(20, card!!.action.hour)
        assertEquals(CoachActionKind.ENABLE_REMINDER, card.action.kind)
    }

    @Test
    fun `best time is silent when reminders already on`() {
        val facts = reviews("2026-06-10T20:00", count = 30, correctCount = 28) +
            reviews("2026-06-11T09:00", count = 30, correctCount = 18, firstWordId = 500)
        assertNull(bestTime.evaluate(snapshot(reviews = facts, remindersEnabled = true)))
    }

    @Test
    fun `best time ignores hours below the bucket gate`() {
        val facts = reviews("2026-06-10T20:00", count = 29, correctCount = 29) +
            reviews("2026-06-11T09:00", count = 40, correctCount = 24, firstWordId = 500)
        assertNull(bestTime.evaluate(snapshot(reviews = facts)))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*insightsscreen.rules.HabitRulesTest"`
Expected: FAIL — unresolved `StreakAtRiskRule`, `BestTimeRule`.

- [ ] **Step 3: Implement**

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen.rules

import com.alirezaiyan.vokab.server.analytics.insightsscreen.*
import org.springframework.stereotype.Component

@Component
class StreakAtRiskRule : CoachRule {
    override val type = "STREAK_AT_RISK"
    override val priority = 100

    override fun evaluate(snapshot: LearnerSnapshot): CoachCardDto? {
        if (snapshot.currentStreak < 1 || snapshot.now.hour < EVENING_HOUR) return null
        val byDate = snapshot.reviewsByDate()
        val studiedToday = (byDate[snapshot.today] ?: 0) > 0
        val studiedYesterday = (byDate[snapshot.today.minusDays(1)] ?: 0) > 0
        if (studiedToday || !studiedYesterday) return null
        return card(
            snapshot,
            title = "Keep your ${snapshot.currentStreak}-day streak alive",
            body = "A few minutes of review today keeps it going.",
            action = CoachActionDto(CoachActionKind.START_REVIEW, label = "Review now"),
        )
    }

    companion object {
        const val EVENING_HOUR = 18
    }
}

@Component
class BestTimeRule : CoachRule {
    override val type = "BEST_TIME"
    override val priority = 60

    override fun evaluate(snapshot: LearnerSnapshot): CoachCardDto? {
        if (snapshot.remindersEnabled) return null
        val overall = snapshot.reviews.gatedAccuracyPct() ?: return null
        val (hour, accuracy) = snapshot.hourBuckets()
            .mapNotNull { (hour, facts) -> facts.gatedAccuracyPct(MIN_REVIEWS_PER_BUCKET)?.let { hour to it } }
            .maxByOrNull { it.second } ?: return null
        if (accuracy - overall < MIN_LIFT_POINTS) return null
        return card(
            snapshot,
            title = "Your sharpest study hour",
            body = "You get $accuracy% right at this hour, compared with $overall% overall.",
            action = CoachActionDto(CoachActionKind.ENABLE_REMINDER, label = "Remind me daily", hour = hour),
        )
    }

    companion object {
        const val MIN_LIFT_POINTS = 10
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*insightsscreen.rules.HabitRulesTest"`
Expected: PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/analytics/insightsscreen/rules src/test/kotlin/com/alirezaiyan/vokab/server/analytics/insightsscreen/rules
git commit -m "feat(insights-screen): add streak-at-risk and best-time coach rules"
```

---

### Task 6: Word rules (SLIPPING_WORDS, DIFFICULT_WORDS, LEVEL_BOTTLENECK, COMEBACK_WIN)

**Files:**
- Create: `MAIN/analytics/insightsscreen/rules/WordRules.kt`
- Test: `TEST/analytics/insightsscreen/rules/WordRulesTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen.rules

import com.alirezaiyan.vokab.server.analytics.insightsscreen.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WordRulesTest {

    @Test
    fun `slipping words fires for three words whose latest review this week was a demotion`() {
        val facts = (1L..3L).map { review("2026-06-16T09:00", wordId = it, correct = false, previousLevel = 3) }
        val card = SlippingWordsRule().evaluate(snapshot(reviews = facts))
        assertNotNull(card)
        assertEquals("3 words need a refresh", card!!.title)
        assertEquals(listOf(1L, 2L, 3L), card.action.wordIds)
        assertEquals(CoachActionKind.REVIEW_WORDS, card.action.kind)
    }

    @Test
    fun `slipping words ignores words that recovered later`() {
        val facts = (1L..3L).map { review("2026-06-15T09:00", wordId = it, correct = false, previousLevel = 3) } +
            review("2026-06-16T09:00", wordId = 1, correct = true, previousLevel = 2)
        assertNull(SlippingWordsRule().evaluate(snapshot(reviews = facts)))
    }

    @Test
    fun `slipping words ignores demotions older than seven days`() {
        val facts = (1L..3L).map { review("2026-06-09T09:00", wordId = it, correct = false, previousLevel = 3) }
        assertNull(SlippingWordsRule().evaluate(snapshot(reviews = facts)))
    }

    @Test
    fun `difficult words fires for three words under 50 percent`() {
        val words = listOf(
            WordAccuracy(1, "a", 30, 5), WordAccuracy(2, "b", 40, 4), WordAccuracy(3, "c", 49, 3), WordAccuracy(4, "d", 80, 9),
        )
        val card = DifficultWordsRule().evaluate(snapshot(difficultWords = words))
        assertNotNull(card)
        assertEquals(listOf(1L, 2L, 3L), card!!.action.wordIds)
    }

    @Test
    fun `difficult words silent with fewer than three`() {
        val words = listOf(WordAccuracy(1, "a", 30, 5), WordAccuracy(2, "b", 40, 4))
        assertNull(DifficultWordsRule().evaluate(snapshot(difficultWords = words)))
    }

    @Test
    fun `level bottleneck fires for a stage 15pt below the others`() {
        val facts = reviews("2026-06-16T09:00", 20, 18, firstWordId = 100, previousLevel = 1) +
            reviews("2026-06-16T10:00", 20, 18, firstWordId = 200, previousLevel = 2) +
            reviews("2026-06-16T11:00", 20, 10, firstWordId = 300, previousLevel = 3)
        val card = LevelBottleneckRule().evaluate(snapshot(reviews = facts))
        assertNotNull(card)
        assertEquals("Stage 3 words are sticking less", card!!.title)
        assertEquals(20, card.action.wordIds.size)
    }

    @Test
    fun `level bottleneck silent when stages are close`() {
        val facts = reviews("2026-06-16T09:00", 20, 18, firstWordId = 100, previousLevel = 1) +
            reviews("2026-06-16T10:00", 20, 17, firstWordId = 200, previousLevel = 2) +
            reviews("2026-06-16T11:00", 20, 16, firstWordId = 300, previousLevel = 3)
        assertNull(LevelBottleneckRule().evaluate(snapshot(reviews = facts)))
    }

    @Test
    fun `comeback win fires only for comebacks mastered this past week`() {
        val facts = listOf(review("2026-06-16T09:00", wordId = 7, correct = true, previousLevel = 5, newLevel = 6))
        val card = ComebackWinRule().evaluate(
            snapshot(reviews = facts, comebackWords = listOf(WordRef(7, "ubiquitous"), WordRef(8, "old")))
        )
        assertNotNull(card)
        assertEquals("1 comeback word", card!!.title)
        assertEquals(listOf(WordChipDto(7, "ubiquitous")), card.words)
        assertEquals(CoachActionKind.NONE, card.action.kind)
    }

    @Test
    fun `comeback win silent when no recent mastery`() {
        assertNull(ComebackWinRule().evaluate(snapshot(comebackWords = listOf(WordRef(8, "old")))))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*insightsscreen.rules.WordRulesTest"`
Expected: FAIL — unresolved rule classes.

- [ ] **Step 3: Implement**

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen.rules

import com.alirezaiyan.vokab.server.analytics.insightsscreen.*
import org.springframework.stereotype.Component

private const val RECENT_DAYS = 7L
private const val MIN_WORDS = 3

private fun reviewWords(words: List<WordRef>) =
    CoachActionDto(CoachActionKind.REVIEW_WORDS, label = "Review ${plural(words.size, "word")}", wordIds = words.map { it.id })

@Component
class SlippingWordsRule : CoachRule {
    override val type = "SLIPPING_WORDS"
    override val priority = 90

    override fun evaluate(snapshot: LearnerSnapshot): CoachCardDto? {
        val slipping = snapshot.reviewsIn(snapshot.lastDays(RECENT_DAYS))
            .groupBy { it.wordId }
            .mapNotNull { (_, facts) -> facts.maxBy { it.reviewedAt }.takeIf { it.newLevel < it.previousLevel } }
            .sortedBy { it.reviewedAt }
            .map { WordRef(it.wordId, it.wordText) }
        if (slipping.size < MIN_WORDS) return null
        return card(
            snapshot,
            title = "${plural(slipping.size, "word")} need a refresh",
            body = "They dropped a stage this week. A quick review locks them back in.",
            action = reviewWords(slipping),
            words = slipping,
        )
    }
}

@Component
class DifficultWordsRule : CoachRule {
    override val type = "DIFFICULT_WORDS"
    override val priority = 80

    override fun evaluate(snapshot: LearnerSnapshot): CoachCardDto? {
        val hard = snapshot.difficultWords
            .filter { it.accuracyPct < MAX_ACCURACY_PCT && it.reviews >= MIN_REVIEWS }
            .sortedBy { it.accuracyPct }
            .map { WordRef(it.id, it.text) }
        if (hard.size < MIN_WORDS) return null
        return card(
            snapshot,
            title = "${plural(hard.size, "word")} keep tripping you up",
            body = "You get these right less than half the time. One focused session helps.",
            action = reviewWords(hard),
            words = hard,
        )
    }

    companion object {
        const val MAX_ACCURACY_PCT = 50
        const val MIN_REVIEWS = 3
    }
}

@Component
class LevelBottleneckRule : CoachRule {
    override val type = "LEVEL_BOTTLENECK"
    override val priority = 50

    override fun evaluate(snapshot: LearnerSnapshot): CoachCardDto? {
        val rates = snapshot.stageBuckets()
            .filterKeys { it < MASTERED_LEVEL }
            .mapNotNull { (stage, facts) -> facts.gatedAccuracyPct()?.let { stage to it } }
            .toMap()
        if (rates.size < MIN_STAGES) return null
        val (stage, rate) = rates.minBy { it.value }
        val others = rates.filterKeys { it != stage }.values.average().toInt()
        if (others - rate < MIN_GAP_POINTS) return null
        val words = snapshot.stageBuckets().getValue(stage)
            .sortedByDescending { it.reviewedAt }
            .distinctBy { it.wordId }
            .map { WordRef(it.wordId, it.wordText) }
        return card(
            snapshot,
            title = "Stage $stage words are sticking less",
            body = "You pass $rate% of reviews at this stage, compared with $others% at other stages.",
            action = reviewWords(words),
            words = words,
        )
    }

    companion object {
        const val MIN_STAGES = 3
        const val MIN_GAP_POINTS = 15
    }
}

@Component
class ComebackWinRule : CoachRule {
    override val type = "COMEBACK_WIN"
    override val priority = 40

    override fun evaluate(snapshot: LearnerSnapshot): CoachCardDto? {
        val masteredRecently = snapshot.reviewsIn(snapshot.lastDays(RECENT_DAYS))
            .filter { it.newLevel == MASTERED_LEVEL && it.previousLevel < MASTERED_LEVEL }
            .map { it.wordId }
            .toSet()
        val wins = snapshot.comebackWords.filter { it.id in masteredRecently }
        if (wins.isEmpty()) return null
        return card(
            snapshot,
            title = plural(wins.size, "comeback word"),
            body = "Words you once struggled with are now mastered. That's real progress.",
            action = CoachActionDto(CoachActionKind.NONE),
            words = wins,
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*insightsscreen.rules.WordRulesTest"`
Expected: PASS (9 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/analytics/insightsscreen/rules src/test/kotlin/com/alirezaiyan/vokab/server/analytics/insightsscreen/rules
git commit -m "feat(insights-screen): add word-focused coach rules"
```

---

### Task 7: Progress rules (MILESTONE_NEAR, WEEK_TREND)

**Files:**
- Create: `MAIN/analytics/insightsscreen/rules/ProgressRules.kt`
- Test: `TEST/analytics/insightsscreen/rules/ProgressRulesTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen.rules

import com.alirezaiyan.vokab.server.analytics.insightsscreen.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ProgressRulesTest {

    @Test
    fun `milestone near fires within ten words`() {
        val card = MilestoneNearRule().evaluate(snapshot(masteredTotal = 93))
        assertNotNull(card)
        assertEquals("7 words from 100 mastered", card!!.title)
    }

    @Test
    fun `milestone near silent when far`() {
        assertNull(MilestoneNearRule().evaluate(snapshot(masteredTotal = 60)))
    }

    @Test
    fun `milestone near silent for brand new users`() {
        assertNull(MilestoneNearRule().evaluate(snapshot(masteredTotal = 0)))
    }

    @Test
    fun `week trend always fires`() {
        val empty = WeekTrendRule().evaluate(snapshot())
        assertEquals("Start your week", empty?.title)

        val ahead = WeekTrendRule().evaluate(
            snapshot(reviews = reviews("2026-06-16T09:00", 12, 10) + reviews("2026-06-09T09:00", 5, 5, firstWordId = 500))
        )
        assertEquals("12 reviews this week", ahead?.title)
        assertEquals("That's 7 more than last week. Nice momentum.", ahead?.body)

        val behind = WeekTrendRule().evaluate(
            snapshot(reviews = reviews("2026-06-16T09:00", 3, 3) + reviews("2026-06-09T09:00", 10, 10, firstWordId = 500))
        )
        assertEquals("7 more to match last week", behind?.body)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*insightsscreen.rules.ProgressRulesTest"`
Expected: FAIL — unresolved rule classes.

- [ ] **Step 3: Implement**

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen.rules

import com.alirezaiyan.vokab.server.analytics.insightsscreen.*
import org.springframework.stereotype.Component

@Component
class MilestoneNearRule : CoachRule {
    override val type = "MILESTONE_NEAR"
    override val priority = 70

    override fun evaluate(snapshot: LearnerSnapshot): CoachCardDto? {
        if (snapshot.masteredTotal < MIN_MASTERED) return null
        val next = nextMilestone(snapshot.masteredTotal)
        val remaining = (next - snapshot.masteredTotal).toInt()
        if (remaining > MAX_REMAINING) return null
        return card(
            snapshot,
            title = "${plural(remaining, "word")} from $next mastered",
            body = "Words that reach the last stage count as mastered. A focused session could get you there.",
            action = CoachActionDto(CoachActionKind.START_REVIEW, label = "Keep going"),
        )
    }

    companion object {
        const val MIN_MASTERED = 1L
        const val MAX_REMAINING = 10
    }
}

/** Fallback card: always eligible, lowest priority. */
@Component
class WeekTrendRule : CoachRule {
    override val type = "WEEK_TREND"
    override val priority = 10

    override fun evaluate(snapshot: LearnerSnapshot): CoachCardDto {
        val thisWeek = snapshot.reviewsIn(snapshot.thisWeek()).size
        val lastWeek = snapshot.reviewsIn(snapshot.lastWeek()).size
        val action = CoachActionDto(CoachActionKind.START_REVIEW, label = "Review now")
        return when {
            thisWeek == 0 -> card(snapshot, "Start your week", "Your first review this week gets the ball rolling.", action)
            thisWeek >= lastWeek -> card(
                snapshot,
                title = "${plural(thisWeek, "review")} this week",
                body = if (lastWeek == 0) "Great start. Keep it up." else "That's ${thisWeek - lastWeek} more than last week. Nice momentum.",
                action = action,
            )
            else -> card(
                snapshot,
                title = "${plural(thisWeek, "review")} this week",
                body = "${lastWeek - thisWeek} more to match last week",
                action = action,
            )
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*insightsscreen.rules.ProgressRulesTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/analytics/insightsscreen/rules src/test/kotlin/com/alirezaiyan/vokab/server/analytics/insightsscreen/rules
git commit -m "feat(insights-screen): add milestone and weekly trend coach rules"
```

---

### Task 8: Hero + sections

**Files:**
- Create: `MAIN/analytics/insightsscreen/HeroBuilder.kt`
- Create: `MAIN/analytics/insightsscreen/SectionBuilders.kt`
- Test: `TEST/analytics/insightsscreen/HeroBuilderTest.kt`
- Test: `TEST/analytics/insightsscreen/SectionBuildersTest.kt`

- [ ] **Step 1: Write the failing tests** — `HeroBuilderTest.kt`:

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HeroBuilderTest {

    @Test
    fun `hero compares this week with last week`() {
        val facts = reviews("2026-06-16T09:00", 25, 20) + reviews("2026-06-09T09:00", 20, 10, firstWordId = 500)
        val hero = HeroBuilder.build(snapshot(reviews = facts, currentStreak = 3))

        assertEquals(MetricDto(25, 20), hero.reviews)
        assertEquals(MetricDto(80, 50), hero.accuracyPct)
        assertEquals(3, hero.currentStreak)
        assertEquals(7, hero.week.size)
        assertEquals("2026-06-15", hero.week.first().date)
        assertEquals(25, hero.week[1].reviews)
    }

    @Test
    fun `accuracy is hidden below the sample gate`() {
        assertNull(HeroBuilder.build(snapshot(reviews = reviews("2026-06-16T09:00", 5, 5))).accuracyPct)
    }

    @Test
    fun `headline reflects the week`() {
        assertEquals("A fresh week to learn", HeroBuilder.build(snapshot()).headline)
        val best = reviews("2026-06-16T09:00", 30, 30) + reviews("2026-06-09T09:00", 10, 10, firstWordId = 500)
        assertEquals("Your best week in 12 weeks", HeroBuilder.build(snapshot(reviews = best)).headline)
    }
}
```

`SectionBuildersTest.kt`:

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SectionBuildersTest {

    @Test
    fun `habits is locked with reviews needed below the gate`() {
        val result = SectionBuilders.build(snapshot(reviews = reviews("2026-06-16T09:00", 12, 10)))
        assertNull(result.sections.habits)
        assertEquals(listOf(LockedSectionDto(SectionKey.HABITS, 18)), result.locked)
    }

    @Test
    fun `habits unlocked shows best weekday caption`() {
        val facts = reviews("2026-06-16T09:00", 30, 27) + reviews("2026-06-11T09:00", 30, 15, firstWordId = 500)
        val habits = SectionBuilders.build(snapshot(reviews = facts)).sections.habits
        assertNotNull(habits)
        assertEquals("You're sharpest on Tuesdays", habits!!.caption)
        assertEquals(BestHourDto(9, 70), habits.bestHour)
        assertEquals(2, habits.heatmap.size)
    }

    @Test
    fun `mastery lists all seven stages and weekly movement`() {
        val facts = listOf(review("2026-06-16T09:00", wordId = 1, correct = true))
        val mastery = SectionBuilders.build(snapshot(reviews = facts, wordsPerLevel = mapOf(0 to 4L, 6 to 2L))).sections.mastery
        assertNotNull(mastery)
        assertEquals((0..6).toList(), mastery!!.levels.map { it.level })
        assertEquals(1, mastery.promotedThisWeek)
        assertEquals("1 word moved up a stage this week", mastery.caption)
    }

    @Test
    fun `empty sections are hidden, not locked`() {
        val result = SectionBuilders.build(snapshot(reviews = reviews("2026-06-16T09:00", 40, 30)))
        assertNull(result.sections.mastery)
        assertNull(result.sections.words)
        assertNull(result.sections.wordRush)
        assertTrue(result.locked.isEmpty())
    }

    @Test
    fun `word rush section shows recent scores`() {
        val wr = SectionBuilders.build(snapshot(wordRush = WordRushSummary(14, 2300, listOf(1800, 2100)))).sections.wordRush
        assertEquals(WordRushSectionDto("Best score 2300", 14, 2300, listOf(1800, 2100)), wr)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*insightsscreen.HeroBuilderTest" --tests "*insightsscreen.SectionBuildersTest"`
Expected: FAIL — unresolved `HeroBuilder`, `SectionBuilders`.

- [ ] **Step 3: Implement** — `HeroBuilder.kt`:

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen

object HeroBuilder {

    private const val HISTORY_WEEKS = 12L

    fun build(snapshot: LearnerSnapshot): HeroDto {
        val thisWeek = snapshot.reviewsIn(snapshot.thisWeek())
        val lastWeek = snapshot.reviewsIn(snapshot.lastWeek())
        val byDate = snapshot.reviewsByDate()
        val currentAccuracy = thisWeek.gatedAccuracyPct()

        return HeroDto(
            headline = headline(snapshot, thisWeek.size, lastWeek.size),
            reviews = MetricDto(thisWeek.size, lastWeek.size),
            accuracyPct = currentAccuracy?.let { MetricDto(it, lastWeek.accuracyPct() ?: 0) },
            leveledUp = MetricDto(thisWeek.leveledUpWordCount(), lastWeek.leveledUpWordCount()),
            currentStreak = snapshot.currentStreak,
            week = (0L..6L).map { offset ->
                val date = snapshot.weekStart.plusDays(offset)
                DayCountDto(date.toString(), byDate[date] ?: 0)
            },
        )
    }

    private fun headline(snapshot: LearnerSnapshot, thisWeek: Int, lastWeek: Int): String {
        if (thisWeek == 0) return "A fresh week to learn"
        val pastWeeks = (1L until HISTORY_WEEKS).map { back ->
            val start = snapshot.weekStart.minusWeeks(back)
            snapshot.reviewsIn(Period(start, start.plusDays(6))).size
        }
        return when {
            thisWeek > pastWeeks.max() -> "Your best week in $HISTORY_WEEKS weeks"
            thisWeek > lastWeek -> "Ahead of last week"
            else -> "Keep the rhythm going"
        }
    }
}
```

`SectionBuilders.kt`:

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private val logger = KotlinLogging.logger {}

data class SectionsResult(val sections: SectionsDto, val locked: List<LockedSectionDto>)

object SectionBuilders {

    private const val MAX_HARDEST = 10
    private const val MIN_WEEKDAYS_FOR_CLAIM = 2

    fun build(snapshot: LearnerSnapshot): SectionsResult {
        val locked = mutableListOf<LockedSectionDto>()
        val habitsGap = MIN_REVIEWS_PER_BUCKET - snapshot.reviews.size
        if (habitsGap > 0) locked += LockedSectionDto(SectionKey.HABITS, habitsGap)

        return SectionsResult(
            sections = SectionsDto(
                mastery = isolated("mastery") { mastery(snapshot) },
                habits = if (habitsGap > 0) null else isolated("habits") { habits(snapshot) },
                words = isolated("words") { words(snapshot) },
                wordRush = isolated("wordRush") { wordRush(snapshot) },
            ),
            locked = locked,
        )
    }

    private fun <T> isolated(name: String, block: () -> T?): T? = runCatching(block)
        .onFailure { logger.warn(it) { "Insights section $name failed; hiding it" } }
        .getOrNull()

    private fun mastery(snapshot: LearnerSnapshot): MasterySectionDto? {
        if (snapshot.wordsPerLevel.values.sum() == 0L) return null
        val week = snapshot.reviewsIn(snapshot.thisWeek())
        val promoted = week.leveledUpWordCount()
        val demoted = week.demotedWordCount()
        val caption = when {
            promoted > demoted -> "${plural(promoted, "word")} moved up a stage this week"
            demoted > 0 -> "${plural(demoted, "word")} slipped back. A review will catch them up."
            else -> "Your words across the learning stages"
        }
        return MasterySectionDto(
            caption = caption,
            levels = (0..MASTERED_LEVEL).map { LevelCountDto(it, snapshot.wordsPerLevel[it] ?: 0) },
            promotedThisWeek = promoted,
            demotedThisWeek = demoted,
        )
    }

    private fun habits(snapshot: LearnerSnapshot): HabitsSectionDto {
        val bestHour = snapshot.hourBuckets()
            .mapNotNull { (hour, facts) -> facts.gatedAccuracyPct(MIN_REVIEWS_PER_BUCKET)?.let { BestHourDto(hour, it) } }
            .maxByOrNull { it.accuracyPct }
        val weekdays = snapshot.weekdayBuckets()
            .mapNotNull { (day, facts) -> facts.gatedAccuracyPct()?.let { WeekdayAccuracyDto(day.value, it, facts.size) } }
            .sortedBy { it.isoDay }
        val heatmap = snapshot.reviewsByDate().toSortedMap().map { (date, count) -> DayCountDto(date.toString(), count) }
        val caption = if (weekdays.size >= MIN_WEEKDAYS_FOR_CLAIM) {
            val best = DayOfWeek.of(weekdays.maxBy { it.accuracyPct }.isoDay)
            "You're sharpest on ${best.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}s"
        } else {
            "${plural(heatmap.size, "active day")} in the last 12 weeks"
        }
        return HabitsSectionDto(caption, heatmap, bestHour, weekdays)
    }

    private fun words(snapshot: LearnerSnapshot): WordsSectionDto? {
        val hardest = snapshot.difficultWords.sortedBy { it.accuracyPct }.take(MAX_HARDEST)
            .map { WordAccuracyDto(it.id, it.text, it.accuracyPct) }
        val comebacks = snapshot.comebackWords.map { WordChipDto(it.id, it.text) }
        if (hardest.isEmpty() && comebacks.isEmpty()) return null
        val caption = if (hardest.isNotEmpty()) "Focus here for quick wins" else "Words you turned around"
        return WordsSectionDto(caption, hardest, comebacks)
    }

    private fun wordRush(snapshot: LearnerSnapshot): WordRushSectionDto? {
        val wr = snapshot.wordRush?.takeIf { it.gamesPlayed > 0 } ?: return null
        return WordRushSectionDto("Best score ${wr.bestScore}", wr.gamesPlayed, wr.bestScore, wr.recentScores)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*insightsscreen.HeroBuilderTest" --tests "*insightsscreen.SectionBuildersTest"`
Expected: PASS (8 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/analytics/insightsscreen src/test/kotlin/com/alirezaiyan/vokab/server/analytics/insightsscreen
git commit -m "feat(insights-screen): add hero and deep-dive section builders"
```

---

### Task 9: Snapshot loader + service

**Files:**
- Create: `MAIN/analytics/insightsscreen/LearnerSnapshotLoader.kt`
- Create: `MAIN/analytics/insightsscreen/InsightsScreenService.kt`
- Test: `TEST/analytics/insightsscreen/InsightsScreenServiceTest.kt`

- [ ] **Step 1: Write the failing test** (MockK, loader mocked — service only assembles):

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InsightsScreenServiceTest {

    private val loader: LearnerSnapshotLoader = mockk()
    private val service = InsightsScreenService(loader, CoachEngine(listOf(WeekTrendRuleStub())))

    private class WeekTrendRuleStub : CoachRule {
        override val type = "WEEK_TREND"
        override val priority = 10
        override fun evaluate(snapshot: LearnerSnapshot) = card(snapshot, "t", "b", CoachActionDto(CoachActionKind.NONE))
    }

    @Test
    fun `assembles hero, coach, sections and locked from one snapshot`() {
        val s = snapshot(reviews = reviews("2026-06-16T09:00", 10, 8), totalReviewsAllTime = 140)
        every { loader.load(1L, BERLIN) } returns s

        val response = service.build(1L, BERLIN)

        assertEquals(140, response.totalReviews)
        assertEquals(10, response.hero.reviews.value)
        assertEquals(listOf("WEEK_TREND"), response.coach.map { it.type })
        assertEquals(listOf(LockedSectionDto(SectionKey.HABITS, 20)), response.locked)
        assertEquals(TEST_LOCAL_NOW.toInstant().toString(), response.generatedAt)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*insightsscreen.InsightsScreenServiceTest"`
Expected: FAIL — unresolved `LearnerSnapshotLoader`, `InsightsScreenService`.

- [ ] **Step 3: Implement** — `LearnerSnapshotLoader.kt`:

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen

import com.alirezaiyan.vokab.server.analytics.ReviewEventRepository
import com.alirezaiyan.vokab.server.user.UserRepository
import com.alirezaiyan.vokab.server.user.UserSettingsRepository
import com.alirezaiyan.vokab.server.words.WordRepository
import com.alirezaiyan.vokab.server.wordrush.WordRushGameRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToInt

@Component
class LearnerSnapshotLoader(
    private val reviewEventRepository: ReviewEventRepository,
    private val wordRepository: WordRepository,
    private val userRepository: UserRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val wordRushGameRepository: WordRushGameRepository,
    private val clock: Clock,
) {
    /** Must run inside a read-only transaction (lazy user reference). */
    fun load(userId: Long, zone: ZoneId): LearnerSnapshot {
        val user = userRepository.getReferenceById(userId)
        val now = ZonedDateTime.now(clock.withZone(zone))
        val sinceMs = now.toLocalDate().minusDays(LearnerSnapshot.HISTORY_DAYS - 1)
            .atStartOfDay(zone).toInstant().toEpochMilli()

        val reviews = reviewEventRepository.findFactsByUserIdSince(userId, sinceMs).map {
            ReviewFact(it.wordId, it.wordText, it.rating >= 1, it.previousLevel, it.newLevel, it.reviewedAt)
        }
        val wordsPerLevel = wordRepository.findProgressRowsByUserId(userId, now.toInstant().toEpochMilli())
            .associate { it.getLevel() to it.getWordCount() }
        val difficult = reviewEventRepository
            .findDifficultWords(user, DIFFICULT_MIN_REVIEWS, PageRequest.of(0, DIFFICULT_LIMIT))
            .map {
                val accuracy = ((it.total - it.errors) * 100.0 / it.total).roundToInt()
                WordAccuracy(it.wordId, it.wordText, accuracy, it.total.toInt())
            }
        val comebacks = reviewEventRepository.findComebackWords(user).map { WordRef(it.wordId, it.wordText) }
        val gamesPlayed = wordRushGameRepository.countByUser(user)
        val wordRush = if (gamesPlayed == 0L) null else WordRushSummary(
            gamesPlayed = gamesPlayed,
            bestScore = wordRushGameRepository.findBestScore(user),
            recentScores = wordRushGameRepository.findTop20ByUserOrderByPlayedAtDesc(user)
                .take(RECENT_GAMES).map { it.score }.reversed(),
        )

        return LearnerSnapshot(
            zone = zone,
            now = now,
            reviews = reviews,
            totalReviewsAllTime = reviewEventRepository.countByUser(user),
            wordsPerLevel = wordsPerLevel,
            masteredTotal = wordsPerLevel[MASTERED_LEVEL] ?: 0,
            currentStreak = user.currentStreak,
            remindersEnabled = userSettingsRepository.findByUserId(userId)?.reviewRemindersEnabled ?: true,
            difficultWords = difficult,
            comebackWords = comebacks,
            wordRush = wordRush,
        )
    }

    private companion object {
        const val DIFFICULT_MIN_REVIEWS = 3
        const val DIFFICULT_LIMIT = 20
        const val RECENT_GAMES = 5
    }
}
```

`InsightsScreenService.kt`:

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId

@Service
class InsightsScreenService(
    private val loader: LearnerSnapshotLoader,
    private val coachEngine: CoachEngine,
) {
    @Transactional(readOnly = true)
    fun build(userId: Long, zone: ZoneId): InsightsScreenResponse {
        val snapshot = loader.load(userId, zone)
        val (sections, locked) = SectionBuilders.build(snapshot)
        return InsightsScreenResponse(
            generatedAt = snapshot.now.toInstant().toString(),
            totalReviews = snapshot.totalReviewsAllTime,
            hero = HeroBuilder.build(snapshot),
            coach = coachEngine.cardsFor(snapshot),
            sections = sections,
            locked = locked,
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*insightsscreen.InsightsScreenServiceTest"`
Expected: PASS (1 test).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/analytics/insightsscreen src/test/kotlin/com/alirezaiyan/vokab/server/analytics/insightsscreen
git commit -m "feat(insights-screen): add snapshot loader and screen service"
```

---

### Task 10: Controller + integration test

**Files:**
- Create: `MAIN/analytics/insightsscreen/InsightsScreenController.kt`
- Test: `TEST/analytics/insightsscreen/InsightsScreenControllerTest.kt`
- Test: `TEST/analytics/insightsscreen/InsightsScreenIntegrationTest.kt`

- [ ] **Step 1: Write the failing controller test** (Mockito, matching `AnalyticsControllerTest`):

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen

import com.alirezaiyan.vokab.server.shared.AuthUser
import com.alirezaiyan.vokab.server.shared.ControllerTestSecurityConfig
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZoneId
import java.time.ZoneOffset

@WebMvcTest(InsightsScreenController::class)
@ActiveProfiles("test")
@Import(ControllerTestSecurityConfig::class)
class InsightsScreenControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var service: InsightsScreenService

    private val auth = UsernamePasswordAuthenticationToken(AuthUser(1L), null, emptyList())
    private val response = InsightsScreenService(
        loader = io.mockk.mockk { io.mockk.every { load(any(), any()) } returns snapshot() },
        coachEngine = CoachEngine(emptyList()),
    ).build(1L, BERLIN)

    @Test
    fun `GET insights-screen passes the parsed zone`() {
        `when`(service.build(1L, ZoneId.of("Asia/Tokyo"))).thenReturn(response)

        mockMvc.perform(get("/api/v1/analytics/insights-screen").param("tz", "Asia/Tokyo").with(authentication(auth)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.hero.week.length()").value(7))
    }

    @Test
    fun `invalid or missing tz falls back to UTC`() {
        `when`(service.build(1L, ZoneOffset.UTC)).thenReturn(response)

        mockMvc.perform(get("/api/v1/analytics/insights-screen").param("tz", "Mars/Olympus").with(authentication(auth)))
            .andExpect(status().isOk)
        mockMvc.perform(get("/api/v1/analytics/insights-screen").with(authentication(auth)))
            .andExpect(status().isOk)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*insightsscreen.InsightsScreenControllerTest"`
Expected: FAIL — unresolved `InsightsScreenController`.

- [ ] **Step 3: Implement**

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen

import com.alirezaiyan.vokab.server.shared.ApiResponse
import com.alirezaiyan.vokab.server.shared.AuthUser
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.ZoneId
import java.time.ZoneOffset

@RestController
@RequestMapping("/api/v1/analytics")
class InsightsScreenController(private val service: InsightsScreenService) {

    @GetMapping("/insights-screen")
    fun getInsightsScreen(
        @AuthenticationPrincipal user: AuthUser,
        @RequestParam(required = false) tz: String?,
    ): ResponseEntity<ApiResponse<InsightsScreenResponse>> =
        ResponseEntity.ok(ApiResponse(success = true, data = service.build(user.id, parseZone(tz))))
}

internal fun parseZone(tz: String?): ZoneId =
    tz?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneOffset.UTC
```

- [ ] **Step 4: Run controller test**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*insightsscreen.InsightsScreenControllerTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Write integration test** (real DB, real queries; events "now" so they fall in this week):

```kotlin
package com.alirezaiyan.vokab.server.analytics.insightsscreen

import com.alirezaiyan.vokab.server.TestUserHelper
import com.alirezaiyan.vokab.server.analytics.ReviewEvent
import com.alirezaiyan.vokab.server.analytics.ReviewEventRepository
import com.alirezaiyan.vokab.server.analytics.StudySession
import com.alirezaiyan.vokab.server.analytics.StudySessionRepository
import com.alirezaiyan.vokab.server.user.User
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneOffset

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InsightsScreenIntegrationTest {

    @Autowired lateinit var service: InsightsScreenService
    @Autowired lateinit var studySessionRepository: StudySessionRepository
    @Autowired lateinit var reviewEventRepository: ReviewEventRepository
    @Autowired lateinit var testUserHelper: TestUserHelper

    lateinit var user: User

    @BeforeAll
    fun setupUser() {
        user = testUserHelper.saveAndCommit(User(email = "insights-screen@test.com", name = "Insights Screen"))
    }

    @AfterAll
    fun teardownUser() {
        testUserHelper.clearUserSessions(user.id!!)
        testUserHelper.deleteByEmail("insights-screen@test.com")
    }

    @Test
    fun `builds a screen from real review events`() {
        val now = System.currentTimeMillis()
        val session = studySessionRepository.save(
            StudySession(user = user, clientSessionId = "is-1", startedAt = now, totalCards = 3, correctCount = 2, incorrectCount = 1)
        )
        reviewEventRepository.saveAll((1L..3L).map { id ->
            ReviewEvent(
                session = session, user = user, wordId = id, wordText = "w$id",
                rating = if (id < 3) 1 else 0, previousLevel = 1, newLevel = if (id < 3) 2 else 0, reviewedAt = now,
            )
        })
        // The read-only service joins this transaction with flush mode MANUAL; flush so its queries see the rows.
        reviewEventRepository.flush()

        val response = service.build(user.id!!, ZoneOffset.UTC)

        assertEquals(3, response.hero.reviews.value)
        assertEquals(2, response.hero.leveledUp.value)
        assertEquals(3L, response.totalReviews)
        assertTrue(response.coach.isNotEmpty(), "fallback WEEK_TREND card always present")
        assertEquals(SectionKey.HABITS, response.locked.single().section)
    }
}
```

- [ ] **Step 6: Run integration test**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*insightsscreen.InsightsScreenIntegrationTest"`
Expected: PASS (1 test). If it fails on `StudySession` required constructor args, mirror the `createSession` helper in `AnalyticsIntegrationTest` exactly.

- [ ] **Step 7: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/analytics/insightsscreen src/test/kotlin/com/alirezaiyan/vokab/server/analytics/insightsscreen
git commit -m "feat(insights-screen): expose GET /analytics/insights-screen"
```

---

### Task 11: Full verification + deploy

- [ ] **Step 1: Full build with coverage gate**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew build`
Expected: BUILD SUCCESSFUL, 0 test failures, JaCoCo ≥ 80%.

- [ ] **Step 2: Run locally and call the endpoint**

Run (terminal 1): `./scripts/start-dev.sh`
Run (terminal 2), with a dev JWT obtained per `.claude/rules/lexicon-cross-project.md`:
`curl -s "http://localhost:8080/api/v1/analytics/insights-screen?tz=Europe/Berlin" -H "Authorization: Bearer $TOKEN" | jq '.data | {hero: .hero.headline, coach: [.coach[].type], locked}'`
Expected: JSON with `hero`, at least one coach card (`WEEK_TREND`), `locked` per data volume.

- [ ] **Step 3: Deploy backend** (before any client ships)

Run: `ali server` then `ali health`
Expected: health OK.

- [ ] **Step 4: Smoke production with a real account** — call the endpoint as in Step 2 against production URL from `.claude/infra.local.md`. Confirm `bestHour` and week dates match local expectations.
