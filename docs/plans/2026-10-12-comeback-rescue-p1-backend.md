# Comeback Rescue — P1 Backend Core Implementation Plan

> **For agentic workers:** Use claude-kit:implement to execute this plan task-by-task.

**Goal:** The server plans a personal 3-minute "comeback rescue" (8 AI-picked words + AI-written, fact-grounded push copy) every night for learners in an early lapse, and — behind a flag — sends it at their usual study hour with a deep link that carries the words.

**Architecture:** New package `notification.rescue` in `lexicon.server`. Pure units (policy, pool bucketing, fact sheet, validator, copy linter, fallback, push builder) wrapped by one I/O orchestrator (`ComebackRescuePlanner`) that runs in the existing 01:00 UTC job. `SmartNotificationDispatcher` sends a pending plan before any other push type. Ships in **shadow mode** (`app.comeback-rescue.send-enabled=false`): plans are built and stored, nothing is sent. P3 flips the flag after the taste test.

**Tech Stack:** Spring Boot 3.5 / Kotlin, Spring Data JPA, `NamedParameterJdbcTemplate`, Flyway, OpenRouter via `AiClient`, JUnit 5 + MockK, Testcontainers PostgreSQL (`@SpringBootTest` + `test` profile).

**Design doc:** `.claude/plans/2026-10-12-ai-comeback-rescue.md` (Lexicon repo). Part of 3 plans: **P1 backend core (this)** → P2 client → P3 admin taste test + funnel + enable sends.

**Repo:** all paths below are relative to `~/projects/lexicon.server` unless stated. Work on `main` (commit directly, per project convention).

**Gradle needs JDK 21:** prefix every Gradle command with `JAVA_HOME=$(/usr/libexec/java_home -v 21)`. Below this is abbreviated as `J21=...`; run e.g.
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.ComebackRescuePolicyTest"
```
Docker must be running for `@SpringBootTest` tests (Testcontainers).

---

## Refinements vs. design doc (decided while reading the code)

| Design said | P1 does | Why |
|---|---|---|
| Eligible when last study ≤ today − 2 | `daysAway` (today − last study) in **3..14** | Planning runs at 01:00 UTC, so `daysAway = 3` = two full days missed. > 14 days stays with the existing COLD/DORMANT flow. |
| Habit = ≥ 3 study days in last 14 | ≥ 3 study days in the 14 days **ending on the last study day** | Anchoring on "today" would disqualify every lapse longer than ~11 days. |
| One rescue per lapse | Counts only `SENT`, `COMPLETED`, `HOLDOUT` plans after the last study day | A plan that expired unsent (shadow mode, send hour already passed) must not block a rescue. |
| Priority in `NotificationTypeSelector` | Checked in `SmartNotificationDispatcher.dispatchForUser` | The payload comes from the stored plan, not from `NotificationContentBuilder`; also must outrank the COLD/DORMANT AI branch that runs before the selector. |
| Pool from `getDifficultWords` / `getComebackWords` | One SQL over `words` ⋈ `review_events` + pure bucketing (`RescuePool`) | Need level, due date and the last 5 results per word in one pass; existing queries lack level. |
| `opened_at` column | `notification_log_id` column; opens read from `notification_log.opened_at` | Open tracking already exists; no second source of truth. |
| Word ID mismatch risk | **Resolved:** client moves each uploaded word to its server id (`WordRepositoryImpl.kt:84` in Lexicon) | Server word ids == client ids once synced; un-synced words never reach the server pool. |

---

## File map

**Create** (`src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/`):
| File | Responsibility |
|---|---|
| `ComebackRescue.kt` | JPA entity + `RescueSource` + `RescueStatus` |
| `ComebackRescueRepository.kt` | Spring Data repository |
| `ComebackRescuePolicy.kt` | Pure eligibility rules |
| `RescueCandidates.kt` | `RescueCandidateRow`, `FirstWord`, `RescuePool` (pure bucketing) |
| `RescueCandidateQueries.kt` | SQL: candidate rows, first word, word count |
| `RescueFactSheet.kt` | `RescueFact`, `RescueFactSheet`, `RescueProfile`, `RescueFactSheetBuilder` |
| `RescueAiResponse.kt` | AI JSON response types + tolerant parser |
| `RescuePlanValidator.kt` | Pure word-pick validation |
| `RescueCopyLinter.kt` | Pure copy rules + best-candidate selection |
| `RescueFallback.kt` | Deterministic picks + template copy |
| `ComebackRescuePlanner.kt` | Orchestrates one user's plan (I/O) |
| `ComebackRescueScheduler.kt` | Nightly batch: expire stale, plan all |
| `ComebackRescueService.kt` | Sendable plan, mark sent, complete |
| `RescuePushBuilder.kt` | Plan → `NotificationPayload` with deep link |
| `ComebackRescueController.kt` | `POST /api/v1/notifications/rescue/{id}/complete` |

**Create:** `src/main/resources/db/migration/V46__add_comeback_rescue.sql`, `src/main/resources/prompts/comeback-rescue.v1.txt`

**Modify:** `ai/AiClient.kt` (enum), `ai/PromptTemplates.kt` (enum), `notification/NotificationTypeSelector.kt` (enum), `notification/NotificationContentBuilder.kt` (when branch), `notification/SmartNotificationDispatcher.kt`, `notification/NotificationScheduledTasks.kt`, `src/main/resources/application.yml`, `src/test/.../notification/SmartNotificationDispatcherTest.kt`

**Tests** (`src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/`): one file per unit above.

---

### Task 1: Migration, entity, repository

**Files:**
- Create: `src/main/resources/db/migration/V46__add_comeback_rescue.sql`
- Create: `src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescue.kt`
- Create: `src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescueRepository.kt`
- Test: `src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescueRepositoryTest.kt`

- [ ] **Step 1: Confirm next migration number**

Run: `ls src/main/resources/db/migration | sort -V | tail -1`
Expected: `V45__add_support_chat.sql` → this task creates `V46`. If a higher number exists, use the next one and rename below accordingly.

- [ ] **Step 2: Write the failing test**

```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import com.alirezaiyan.vokab.server.user.User
import com.alirezaiyan.vokab.server.user.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ComebackRescueRepositoryTest {

    @Autowired lateinit var repository: ComebackRescueRepository
    @Autowired lateinit var userRepository: UserRepository

    private val today = LocalDate.of(2026, 6, 17)

    private fun user(): User =
        userRepository.save(User(email = "rescue-${System.nanoTime()}@example.com", name = "Rescue User"))

    private fun rescue(userId: Long, day: LocalDate, status: RescueStatus) = ComebackRescue(
        userId = userId,
        plannedFor = day,
        wordIds = ComebackRescue.encodeWordIds(listOf(3L, 1L, 2L)),
        title = "t",
        body = "b",
        angle = "rematch",
        source = RescueSource.FALLBACK,
        status = status,
    )

    @Test
    fun `stores the ordered word ids and reads them back`() {
        val userId = requireNotNull(user().id)
        repository.saveAndFlush(rescue(userId, today, RescueStatus.PLANNED))

        val found = requireNotNull(repository.findByUserIdAndPlannedFor(userId, today))

        assertEquals(listOf(3L, 1L, 2L), found.wordIdList)
    }

    @Test
    fun `one rescue per lapse counts only sent, completed and holdout plans`() {
        val userId = requireNotNull(user().id)
        val lastStudy = today.minusDays(5)
        repository.saveAndFlush(rescue(userId, today.minusDays(1), RescueStatus.EXPIRED))

        assertFalse(repository.existsByUserIdAndPlannedForAfterAndStatusIn(userId, lastStudy, RescueStatus.CONSUMES_LAPSE))

        repository.saveAndFlush(rescue(userId, today.minusDays(2), RescueStatus.SENT))

        assertTrue(repository.existsByUserIdAndPlannedForAfterAndStatusIn(userId, lastStudy, RescueStatus.CONSUMES_LAPSE))
    }

    @Test
    fun `expires planned rescues from earlier days only`() {
        val userId = requireNotNull(user().id)
        repository.saveAndFlush(rescue(userId, today.minusDays(1), RescueStatus.PLANNED))
        repository.saveAndFlush(rescue(userId, today, RescueStatus.PLANNED))

        val expired = repository.expireStale(today, RescueStatus.PLANNED, RescueStatus.EXPIRED)

        assertEquals(1, expired)
        assertEquals(RescueStatus.PLANNED, repository.findByUserIdAndPlannedFor(userId, today)?.status)
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.ComebackRescueRepositoryTest"`
Expected: FAIL — compilation error `Unresolved reference: ComebackRescueRepository`.

- [ ] **Step 4: Write the migration**

`src/main/resources/db/migration/V46__add_comeback_rescue.sql`:
```sql
-- AI comeback rescue: at most one plan per user per day for learners in an early lapse.
-- word_ids is the ordered session (JSON array of word ids). candidates keeps every AI copy
-- candidate with its lint verdict so the copy can be tuned. Opens are read from
-- notification_log through notification_log_id.
CREATE TABLE comeback_rescue (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    planned_for         DATE      NOT NULL,
    word_ids            JSONB     NOT NULL,
    title               TEXT      NOT NULL,
    body                TEXT      NOT NULL,
    angle               TEXT      NOT NULL,
    outro_win           TEXT,
    outro_soft          TEXT,
    candidates          JSONB,
    picks_reasoning     JSONB,
    source              TEXT      NOT NULL,
    status              TEXT      NOT NULL,
    notification_log_id BIGINT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    sent_at             TIMESTAMP,
    completed_at        TIMESTAMP,
    CONSTRAINT uq_comeback_rescue_user_day UNIQUE (user_id, planned_for)
);

CREATE INDEX idx_comeback_rescue_status_day ON comeback_rescue (status, planned_for);
```

- [ ] **Step 5: Write the entity**

`ComebackRescue.kt`:
```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import com.alirezaiyan.vokab.server.shared.JpaEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.time.LocalDate

/** Where a rescue's words and copy came from. */
enum class RescueSource {
    /** AI picked the words and one of its copy candidates passed the linter. */
    AI,
    /** AI picks were valid, every AI copy candidate failed the linter: template copy. */
    AI_PICKS_TEMPLATE_COPY,
    /** AI unavailable or its picks invalid: deterministic picks and template copy. */
    FALLBACK,
}

enum class RescueStatus {
    PLANNED, HOLDOUT, SENT, COMPLETED, EXPIRED;

    companion object {
        /** Plans that use up the learner's one rescue for the current lapse. */
        val CONSUMES_LAPSE: Set<RescueStatus> = setOf(HOLDOUT, SENT, COMPLETED)
    }
}

/**
 * One learner's comeback rescue for one day: the ordered session words, the push copy and the
 * session outros, planned nightly by [ComebackRescuePlanner].
 */
@Entity
@Table(name = "comeback_rescue")
class ComebackRescue(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    override val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "planned_for", nullable = false)
    val plannedFor: LocalDate,

    /** JSON array in session order; read through [wordIdList]. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "word_ids", columnDefinition = "jsonb", nullable = false)
    val wordIds: String,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val body: String,

    @Column(nullable = false)
    val angle: String,

    @Column(name = "outro_win")
    val outroWin: String? = null,

    @Column(name = "outro_soft")
    val outroSoft: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    val candidates: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "picks_reasoning", columnDefinition = "jsonb")
    val picksReasoning: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val source: RescueSource,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: RescueStatus,

    @Column(name = "notification_log_id")
    var notificationLogId: Long? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "sent_at")
    var sentAt: Instant? = null,

    @Column(name = "completed_at")
    var completedAt: Instant? = null,
) : JpaEntity<Long>() {

    val wordIdList: List<Long>
        get() = wordIds.removeSurrounding("[", "]").split(",").mapNotNull { it.trim().toLongOrNull() }

    override fun toString(): String =
        "ComebackRescue(id=$id, userId=$userId, plannedFor=$plannedFor, status=$status, source=$source)"

    companion object {
        fun encodeWordIds(ids: List<Long>): String = ids.joinToString(",", "[", "]")
    }
}
```

- [ ] **Step 6: Write the repository**

`ComebackRescueRepository.kt`:
```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

interface ComebackRescueRepository : JpaRepository<ComebackRescue, Long> {

    fun findByUserIdAndPlannedFor(userId: Long, plannedFor: LocalDate): ComebackRescue?

    fun existsByUserIdAndPlannedForAfterAndStatusIn(
        userId: Long,
        plannedFor: LocalDate,
        statuses: Collection<RescueStatus>,
    ): Boolean

    /** The learner's latest plans, newest first: last angle and recent copy for variety checks. */
    fun findTop3ByUserIdOrderByPlannedForDesc(userId: Long): List<ComebackRescue>

    @Transactional
    @Modifying
    @Query("UPDATE ComebackRescue r SET r.status = :expired WHERE r.status = :planned AND r.plannedFor < :today")
    fun expireStale(
        @Param("today") today: LocalDate,
        @Param("planned") planned: RescueStatus,
        @Param("expired") expired: RescueStatus,
    ): Int
}
```

- [ ] **Step 7: Run test to verify it passes**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.ComebackRescueRepositoryTest"`
Expected: PASS (3 tests). Flyway applies V46 against Testcontainers PostgreSQL; `ddl-auto: validate` confirms entity ↔ table match.

- [ ] **Step 8: Commit**

```bash
git add src/main/resources/db/migration/V46__add_comeback_rescue.sql src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/
git commit -m "feat(rescue): add comeback_rescue table, entity and repository"
```

---

### Task 2: Eligibility policy

**Files:**
- Create: `src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescuePolicy.kt`
- Test: `src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescuePolicyTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import com.alirezaiyan.vokab.server.TEST_TODAY
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComebackRescuePolicyTest {

    private fun daysAgo(vararg days: Long) = days.map { TEST_TODAY.minusDays(it) }

    private fun signals(
        studyDates: List<java.time.LocalDate> = daysAgo(3, 4, 5),
        totalWords: Int = 40,
        alreadyRescued: Boolean = false,
    ) = LapseSignals(studyDates, totalWords, alreadyRescued)

    @Test
    fun `should rescue a learner with a habit who missed two full days`() {
        assertTrue(ComebackRescuePolicy.isEligible(signals(), TEST_TODAY))
    }

    @Test
    fun `should not rescue after only one missed day`() {
        assertFalse(ComebackRescuePolicy.isEligible(signals(studyDates = daysAgo(2, 3, 4)), TEST_TODAY))
    }

    @Test
    fun `should leave lapses longer than two weeks to the existing flow`() {
        assertFalse(ComebackRescuePolicy.isEligible(signals(studyDates = daysAgo(15, 16, 17)), TEST_TODAY))
        assertTrue(ComebackRescuePolicy.isEligible(signals(studyDates = daysAgo(14, 15, 16)), TEST_TODAY))
    }

    @Test
    fun `should require three study days in the two weeks before the lapse`() {
        assertFalse(ComebackRescuePolicy.isEligible(signals(studyDates = daysAgo(3, 4)), TEST_TODAY))
        // Day 17 is outside the 14 days ending on day 3
        assertFalse(ComebackRescuePolicy.isEligible(signals(studyDates = daysAgo(3, 4, 17)), TEST_TODAY))
    }

    @Test
    fun `should require enough words for a varied session`() {
        assertFalse(ComebackRescuePolicy.isEligible(signals(totalWords = ComebackRescuePolicy.MIN_WORDS - 1), TEST_TODAY))
    }

    @Test
    fun `should rescue once per lapse`() {
        assertFalse(ComebackRescuePolicy.isEligible(signals(alreadyRescued = true), TEST_TODAY))
    }

    @Test
    fun `should not rescue someone who never studied`() {
        assertFalse(ComebackRescuePolicy.isEligible(signals(studyDates = emptyList()), TEST_TODAY))
    }

    @Test
    fun `daysAway counts from the last study day`() {
        assertEquals(3L, ComebackRescuePolicy.daysAway(daysAgo(5, 3, 4), TEST_TODAY))
        assertNull(ComebackRescuePolicy.daysAway(emptyList(), TEST_TODAY))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.ComebackRescuePolicyTest"`
Expected: FAIL — `Unresolved reference: LapseSignals`.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** What the eligibility rules need to know about one learner. */
data class LapseSignals(
    /** Study days (UTC streak days) in roughly the last four weeks, any order. */
    val studyDates: List<LocalDate>,
    val totalWords: Int,
    /** A rescue was already sent (or held out) since the last study day. */
    val alreadyRescuedThisLapse: Boolean,
)

/**
 * Who gets a comeback rescue: a learner who had a habit and has now missed two full days,
 * caught before the habit dies. Longer lapses stay with the COLD/DORMANT flow.
 */
object ComebackRescuePolicy {
    /** Planning runs at 01:00 UTC, so 3 days since the last study day = two full days missed. */
    const val MIN_DAYS_AWAY = 3L
    const val MAX_DAYS_AWAY = 14L
    const val HABIT_WINDOW_DAYS = 14L
    const val MIN_HABIT_DAYS = 3
    const val MIN_WORDS = 10

    /** How far back study history must reach to judge both the lapse and the habit before it. */
    const val LOOKBACK_DAYS = MAX_DAYS_AWAY + HABIT_WINDOW_DAYS

    fun daysAway(studyDates: List<LocalDate>, today: LocalDate): Long? =
        studyDates.maxOrNull()?.let { ChronoUnit.DAYS.between(it, today) }

    fun isEligible(signals: LapseSignals, today: LocalDate): Boolean {
        val lastStudy = signals.studyDates.maxOrNull() ?: return false
        val daysAway = ChronoUnit.DAYS.between(lastStudy, today)
        if (daysAway !in MIN_DAYS_AWAY..MAX_DAYS_AWAY) return false

        val windowStart = lastStudy.minusDays(HABIT_WINDOW_DAYS - 1)
        val habitDays = signals.studyDates.distinct().count { !it.isBefore(windowStart) && !it.isAfter(lastStudy) }

        return habitDays >= MIN_HABIT_DAYS &&
            signals.totalWords >= MIN_WORDS &&
            !signals.alreadyRescuedThisLapse
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.ComebackRescuePolicyTest"`
Expected: PASS (8 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescuePolicy.kt src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescuePolicyTest.kt
git commit -m "feat(rescue): eligibility policy for early-lapse learners"
```

---

### Task 3: Candidate rows and pool bucketing (pure)

**Files:**
- Create: `src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueCandidates.kt`
- Test: `src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescuePoolTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal fun candidateRow(
    id: Long,
    text: String = "w$id",
    level: Int = 4,
    recent: String = "1111",
    total: Int = 4,
    errors: Int = 0,
    nextReviewDate: Long = 0L,
    source: String = "en",
    target: String = "de",
) = RescueCandidateRow(
    wordId = id, text = text, translation = "t$id", sourceLanguage = source, targetLanguage = target,
    level = level, nextReviewDate = nextReviewDate, totalReviews = total, errors = errors, recent = recent,
)

class RescuePoolTest {

    private val now = 1_000_000_000L

    @Test
    fun `near mastery needs level 3 to 5, good recent accuracy and to be due soon`() {
        val rows = listOf(
            candidateRow(1, level = 5, recent = "1111"),
            candidateRow(2, level = 2, recent = "1111"),                       // level too low
            candidateRow(3, level = 4, recent = "1010"),                       // 50% recent
            candidateRow(4, level = 4, recent = "1"),                          // too little history
            candidateRow(5, level = 3, recent = "1110", nextReviewDate = now + RescuePool.DUE_SOON_MS + 1), // not due soon
            candidateRow(6, level = 3, recent = "1110", nextReviewDate = now + RescuePool.DUE_SOON_MS),
        )

        val pool = requireNotNull(RescuePool.from(rows, now))

        assertEquals(listOf(1L, 6L), pool.nearMastery.map { it.wordId })
    }

    @Test
    fun `weak words are missed often but have been answered right at least once`() {
        val rows = listOf(
            candidateRow(10, level = 2, recent = "0101", total = 6, errors = 3),
            candidateRow(11, level = 1, recent = "0000", total = 4, errors = 4),  // never right
            candidateRow(12, level = 2, recent = "01", total = 2, errors = 1),    // too few reviews
            candidateRow(13, level = 2, recent = "1111", total = 10, errors = 2), // error rate 20%
        )

        val pool = requireNotNull(RescuePool.from(rows, now))

        assertEquals(listOf(10L), pool.weak.map { it.wordId })
    }

    @Test
    fun `weak words closest to cracking come first`() {
        val rows = listOf(
            candidateRow(20, level = 2, recent = "0001", total = 8, errors = 6),
            candidateRow(21, level = 2, recent = "1101", total = 8, errors = 4),
        )

        val pool = requireNotNull(RescuePool.from(rows, now))

        assertEquals(listOf(21L, 20L), pool.weak.map { it.wordId })
    }

    @Test
    fun `pool keeps only the language pair with the most candidates`() {
        val rows = listOf(
            candidateRow(1, target = "de"), candidateRow(2, target = "de"),
            candidateRow(3, target = "fr"),
        )

        val pool = requireNotNull(RescuePool.from(rows, now))

        assertEquals("de", pool.targetLanguage)
        assertEquals(listOf(1L, 2L), pool.nearMastery.map { it.wordId })
    }

    @Test
    fun `buckets are capped`() {
        val rows = (1L..30L).map { candidateRow(it) } +
            (100L..120L).map { candidateRow(it, level = 2, recent = "0101", total = 6, errors = 3) }

        val pool = requireNotNull(RescuePool.from(rows, now))

        assertEquals(RescuePool.MAX_NEAR_MASTERY, pool.nearMastery.size)
        assertEquals(RescuePool.MAX_WEAK, pool.weak.size)
    }

    @Test
    fun `a pool can plan with five near mastery words and one weak word`() {
        val near = (1L..5L).map { candidateRow(it) }
        val weak = candidateRow(50, level = 2, recent = "0101", total = 6, errors = 3)

        assertTrue(requireNotNull(RescuePool.from(near + weak, now)).canPlan)
        assertFalse(requireNotNull(RescuePool.from(near.drop(1) + weak, now)).canPlan)
        assertFalse(requireNotNull(RescuePool.from(near, now)).canPlan)
    }

    @Test
    fun `no rows means no pool`() {
        assertNull(RescuePool.from(emptyList(), now))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.RescuePoolTest"`
Expected: FAIL — `Unresolved reference: RescueCandidateRow`.

- [ ] **Step 3: Write minimal implementation**

`RescueCandidates.kt`:
```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import java.time.Duration
import java.time.Instant

/** One reviewed, not yet mastered word with its review history (see [RescueCandidateQueries]). */
data class RescueCandidateRow(
    val wordId: Long,
    val text: String,
    val translation: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val level: Int,
    /** Epoch millis. */
    val nextReviewDate: Long,
    val totalReviews: Int,
    val errors: Int,
    /** Last (up to) 5 results, newest first: '1' right, '0' wrong. */
    val recent: String,
) {
    val recentAccuracy: Double
        get() = if (recent.isEmpty()) 0.0 else recent.count { it == '1' }.toDouble() / recent.length

    val errorRate: Double
        get() = if (totalReviews == 0) 0.0 else errors.toDouble() / totalReviews

    val lastCorrect: Boolean
        get() = recent.firstOrNull() == '1'
}

/** The oldest word still in the deck, for the "callback" angle. */
data class FirstWord(val text: String, val translation: String, val addedAt: Instant)

/**
 * The words a rescue may use, in one language pair:
 * - [nearMastery]: confidence wins (level 3–5, ≥ 75% of recent answers right, due within [DUE_SOON_MS]);
 * - [weak]: words missed often (≥ 30%) that the learner has got right before, closest to cracking first.
 */
data class RescuePool(
    val sourceLanguage: String,
    val targetLanguage: String,
    val nearMastery: List<RescueCandidateRow>,
    val weak: List<RescueCandidateRow>,
) {
    val all: List<RescueCandidateRow> get() = nearMastery + weak

    /** Enough to open and close on easy wins with at least one real challenge in between. */
    val canPlan: Boolean get() = nearMastery.size >= MIN_NEAR_MASTERY && weak.isNotEmpty()

    companion object {
        const val MAX_NEAR_MASTERY = 15
        const val MAX_WEAK = 8
        const val MIN_NEAR_MASTERY = 5
        val DUE_SOON_MS: Long = Duration.ofDays(3).toMillis()

        private const val MIN_RECENT_ANSWERS = 2
        private const val NEAR_MASTERY_ACCURACY = 0.75
        private const val WEAK_MIN_REVIEWS = 3
        private const val WEAK_ERROR_RATE = 0.3

        fun from(rows: List<RescueCandidateRow>, nowMillis: Long): RescuePool? {
            val pair = rows.groupingBy { it.sourceLanguage to it.targetLanguage }.eachCount()
                .maxByOrNull { it.value }?.key ?: return null
            val inPair = rows.filter { (it.sourceLanguage to it.targetLanguage) == pair }

            val nearMastery = inPair
                .filter {
                    it.level in 3..5 &&
                        it.recent.length >= MIN_RECENT_ANSWERS &&
                        it.recentAccuracy >= NEAR_MASTERY_ACCURACY &&
                        it.nextReviewDate <= nowMillis + DUE_SOON_MS
                }
                .sortedWith(
                    compareByDescending<RescueCandidateRow> { it.level }
                        .thenByDescending { it.recentAccuracy }
                        .thenBy { it.wordId }
                )
                .take(MAX_NEAR_MASTERY)
            val nearIds = nearMastery.map { it.wordId }.toSet()

            val weak = inPair
                .filter {
                    it.wordId !in nearIds &&
                        it.totalReviews >= WEAK_MIN_REVIEWS &&
                        it.errorRate >= WEAK_ERROR_RATE &&
                        it.errors < it.totalReviews
                }
                .sortedWith(
                    compareByDescending<RescueCandidateRow> { it.recentAccuracy }
                        .thenByDescending { it.errorRate }
                        .thenBy { it.wordId }
                )
                .take(MAX_WEAK)

            return RescuePool(pair.first, pair.second, nearMastery, weak)
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.RescuePoolTest"`
Expected: PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueCandidates.kt src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescuePoolTest.kt
git commit -m "feat(rescue): bucket candidate words into near-mastery and weak pools"
```

---

### Task 4: Candidate SQL

**Files:**
- Create: `src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueCandidateQueries.kt`
- Test: `src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueCandidateQueriesTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import com.alirezaiyan.vokab.server.analytics.ReviewEvent
import com.alirezaiyan.vokab.server.analytics.ReviewEventRepository
import com.alirezaiyan.vokab.server.analytics.StudySession
import com.alirezaiyan.vokab.server.analytics.StudySessionRepository
import com.alirezaiyan.vokab.server.user.User
import com.alirezaiyan.vokab.server.user.UserRepository
import com.alirezaiyan.vokab.server.words.Word
import com.alirezaiyan.vokab.server.words.WordRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RescueCandidateQueriesTest {

    @Autowired lateinit var queries: RescueCandidateQueries
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var wordRepository: WordRepository
    @Autowired lateinit var sessionRepository: StudySessionRepository
    @Autowired lateinit var reviewEventRepository: ReviewEventRepository

    private fun user(): User =
        userRepository.save(User(email = "cand-${System.nanoTime()}@example.com", name = "Cand User"))

    private fun word(user: User, text: String, level: Int, createdAt: Instant = Instant.parse("2026-03-02T10:00:00Z")): Word =
        wordRepository.saveAndFlush(
            Word(
                user = user, originalWord = text, translation = "$text-t",
                sourceLanguage = "en", targetLanguage = "de", level = level, nextReviewDate = 123L,
                createdAt = createdAt,
            )
        )

    /** Reviews in time order; [ratings] oldest first, 0 = wrong. */
    private fun reviews(user: User, word: Word, vararg ratings: Int) {
        val session = sessionRepository.saveAndFlush(
            StudySession(user = user, clientSessionId = "s-${System.nanoTime()}", startedAt = 0L)
        )
        ratings.forEachIndexed { i, rating ->
            reviewEventRepository.saveAndFlush(
                ReviewEvent(
                    session = session, user = user, wordId = requireNotNull(word.id), rating = rating,
                    previousLevel = word.level, newLevel = word.level, reviewedAt = 1_000L + i,
                )
            )
        }
    }

    @Test
    fun `returns reviewed unmastered words with counts and the last five results newest first`() {
        val user = user()
        val hund = word(user, "Hund", level = 3)
        reviews(user, hund, 0, 0, 1, 1, 0, 1, 1)   // 7 reviews, 3 wrong; last five oldest→newest 1,1,0,1,1

        val row = queries.candidates(requireNotNull(user.id)).single()

        assertEquals(hund.id, row.wordId)
        assertEquals("Hund", row.text)
        assertEquals(3, row.level)
        assertEquals(123L, row.nextReviewDate)
        assertEquals(7, row.totalReviews)
        assertEquals(3, row.errors)
        assertEquals("11011", row.recent)
    }

    @Test
    fun `skips mastered, new and never reviewed words`() {
        val user = user()
        reviews(user, word(user, "Katze", level = 6), 1, 1)
        reviews(user, word(user, "Maus", level = 0), 1)
        word(user, "Vogel", level = 3)

        assertEquals(emptyList<RescueCandidateRow>(), queries.candidates(requireNotNull(user.id)))
    }

    @Test
    fun `counts only the learner's own reviews`() {
        val owner = user()
        val other = user()
        val baum = word(owner, "Baum", level = 4)
        reviews(owner, baum, 1, 1)
        reviews(other, baum, 0, 0, 0)   // another user's events for the same word id must not count

        assertEquals(2, queries.candidates(requireNotNull(owner.id)).single().totalReviews)
    }

    @Test
    fun `first word is the oldest word in the language pair`() {
        val user = user()
        word(user, "Apfel", level = 2, createdAt = Instant.parse("2026-01-01T00:00:00Z"))
        word(user, "Birne", level = 2, createdAt = Instant.parse("2026-02-01T00:00:00Z"))

        val first = queries.firstWord(requireNotNull(user.id), "en", "de")

        assertEquals("Apfel", first?.text)
        assertNull(queries.firstWord(requireNotNull(user.id), "en", "fr"))
    }

    @Test
    fun `counts all words in the deck`() {
        val user = user()
        word(user, "Apfel", level = 0)
        word(user, "Birne", level = 6)

        assertEquals(2, queries.totalWords(requireNotNull(user.id)))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.RescueCandidateQueriesTest"`
Expected: FAIL — `Unresolved reference: RescueCandidateQueries`.

- [ ] **Step 3: Write minimal implementation**

`RescueCandidateQueries.kt`:
```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp

/** Read-only SQL behind the comeback rescue: candidate words with their review history. */
@Service
class RescueCandidateQueries(private val jdbc: NamedParameterJdbcTemplate) {

    /** Every reviewed word at level 1–5 with its review counts and last five results. */
    @Transactional(readOnly = true)
    fun candidates(userId: Long): List<RescueCandidateRow> =
        jdbc.query(CANDIDATES_SQL, mapOf("userId" to userId)) { rs, _ ->
            RescueCandidateRow(
                wordId = rs.getLong("word_id"),
                text = rs.getString("original_word"),
                translation = rs.getString("translation"),
                sourceLanguage = rs.getString("source_language"),
                targetLanguage = rs.getString("target_language"),
                level = rs.getInt("level"),
                nextReviewDate = rs.getLong("next_review_date"),
                totalReviews = rs.getInt("total_reviews"),
                errors = rs.getInt("errors"),
                recent = rs.getString("recent") ?: "",
            )
        }

    @Transactional(readOnly = true)
    fun firstWord(userId: Long, sourceLanguage: String, targetLanguage: String): FirstWord? =
        jdbc.query(
            FIRST_WORD_SQL,
            mapOf("userId" to userId, "source" to sourceLanguage, "target" to targetLanguage),
        ) { rs, _ ->
            FirstWord(
                text = rs.getString("original_word"),
                translation = rs.getString("translation"),
                addedAt = rs.getObject("created_at", Timestamp::class.java).toInstant(),
            )
        }.firstOrNull()

    @Transactional(readOnly = true)
    fun totalWords(userId: Long): Int =
        jdbc.queryForObject("SELECT COUNT(*) FROM words WHERE user_id = :userId", mapOf("userId" to userId), Int::class.java) ?: 0

    private companion object {
        val CANDIDATES_SQL = """
            WITH ranked AS (
                SELECT e.word_id, e.rating, e.reviewed_at,
                       ROW_NUMBER() OVER (PARTITION BY e.word_id ORDER BY e.reviewed_at DESC) AS rn
                FROM review_events e
                WHERE e.user_id = :userId
            )
            SELECT w.id AS word_id, w.original_word, w.translation, w.source_language, w.target_language,
                   w.level, w.next_review_date,
                   COUNT(r.word_id) AS total_reviews,
                   SUM(CASE WHEN r.rating = 0 THEN 1 ELSE 0 END) AS errors,
                   STRING_AGG(
                       CASE WHEN r.rn <= 5 THEN CASE WHEN r.rating = 0 THEN '0' ELSE '1' END END,
                       '' ORDER BY r.reviewed_at DESC
                   ) AS recent
            FROM words w
            JOIN ranked r ON r.word_id = w.id
            WHERE w.user_id = :userId AND w.level BETWEEN 1 AND 5
            GROUP BY w.id, w.original_word, w.translation, w.source_language, w.target_language,
                     w.level, w.next_review_date
        """.trimIndent()

        val FIRST_WORD_SQL = """
            SELECT original_word, translation, created_at
            FROM words
            WHERE user_id = :userId AND source_language = :source AND target_language = :target
            ORDER BY created_at, id
            LIMIT 1
        """.trimIndent()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.RescueCandidateQueriesTest"`
Expected: PASS (5 tests). If `Word` rejects `createdAt` as a constructor arg or a column name differs (`original_word`, `next_review_date`), check `words/Word.kt` and the `words` table in `src/main/resources/db/migration` and fix the SQL/test to match — do not change the entity.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueCandidateQueries.kt src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueCandidateQueriesTest.kt
git commit -m "feat(rescue): candidate word query with last-five review history"
```

---

### Task 5: Fact sheet

**Files:**
- Create: `src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueFactSheet.kt`
- Test: `src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueFactSheetBuilderTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class RescueFactSheetBuilderTest {

    private val pool = RescuePool(
        sourceLanguage = "en",
        targetLanguage = "de",
        nearMastery = listOf(
            candidateRow(1, text = "Schmetterling", level = 5),
            candidateRow(2, text = "Apfel", level = 5),
            candidateRow(3, text = "Hund", level = 4),
        ),
        weak = listOf(candidateRow(10, text = "Gewitter", level = 2, recent = "10100", total = 9, errors = 5)),
    )

    private fun profile(longestStreak: Int = 19, firstWord: FirstWord? = null) =
        RescueProfile(daysAway = 3, totalWords = 212, longestStreak = longestStreak, firstWord = firstWord)

    @Test
    fun `facts are numbered in order and always start with the session size`() {
        val sheet = RescueFactSheetBuilder.build(profile(), pool)

        assertEquals("F1", sheet.facts.first().id)
        assertEquals("session: 8 words, about 3 minutes", sheet.facts.first().text)
        assertEquals(sheet.facts.indices.map { "F${it + 1}" }, sheet.facts.map { it.id })
    }

    @Test
    fun `weak word fact states its real recent record`() {
        val sheet = RescueFactSheetBuilder.build(profile(), pool)

        val weak = sheet.facts.single { "Gewitter" in it.words }
        assertEquals("weak word \"Gewitter\" (t10): missed 3 of last 5, right on the last try", weak.text)
    }

    @Test
    fun `level 5 words are one review from mastered`() {
        val sheet = RescueFactSheetBuilder.build(profile(), pool)

        assertTrue(sheet.facts.any { it.text == "words one review from mastered: 2" })
        assertTrue(sheet.facts.any { "Schmetterling" in it.words })
    }

    @Test
    fun `short streaks are not worth mentioning`() {
        val sheet = RescueFactSheetBuilder.build(profile(longestStreak = 2), pool)

        assertFalse(sheet.facts.any { it.text.startsWith("longest streak") })
    }

    @Test
    fun `first word is a fact only when it can be in the session`() {
        val inPool = FirstWord("Apfel", "apple", Instant.EPOCH)
        val notInPool = FirstWord("Brot", "bread", Instant.EPOCH)

        assertTrue(RescueFactSheetBuilder.build(profile(firstWord = inPool), pool).facts.any { it.text.startsWith("first word") })
        assertFalse(RescueFactSheetBuilder.build(profile(firstWord = notInPool), pool).facts.any { it.text.startsWith("first word") })
    }

    @Test
    fun `render lists one fact per line`() {
        val sheet = RescueFactSheet(listOf(RescueFact("F1", "a"), RescueFact("F2", "b")))

        assertEquals("F1 a\nF2 b", sheet.render())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.RescueFactSheetBuilderTest"`
Expected: FAIL — `Unresolved reference: RescueProfile`.

- [ ] **Step 3: Write minimal implementation**

`RescueFactSheet.kt`:
```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

/** A true, checkable statement about the learner; [words] are target-language words it names. */
data class RescueFact(val id: String, val text: String, val words: Set<String> = emptySet())

/**
 * Everything the AI may say about the learner. Copy must cite the facts it uses, and the linter
 * rejects any number or quoted word not found in them — the AI cannot invent stats.
 */
data class RescueFactSheet(val facts: List<RescueFact>) {
    val allWords: Set<String> get() = facts.flatMap { it.words }.toSet()

    fun byIds(ids: Collection<String>): List<RescueFact> = facts.filter { it.id in ids }

    fun render(): String = facts.joinToString("\n") { "${it.id} ${it.text}" }
}

data class RescueProfile(
    val daysAway: Long,
    val totalWords: Int,
    val longestStreak: Int,
    val firstWord: FirstWord?,
)

object RescueFactSheetBuilder {
    const val SESSION_SIZE = 8
    const val EST_MINUTES = 3

    private const val MIN_STREAK_WORTH_MENTIONING = 3
    private const val MAX_WORD_FACTS_PER_BUCKET = 3

    fun build(profile: RescueProfile, pool: RescuePool): RescueFactSheet {
        val facts = mutableListOf<RescueFact>()
        fun add(text: String, words: Set<String> = emptySet()) {
            facts += RescueFact("F${facts.size + 1}", text, words)
        }

        add("session: $SESSION_SIZE words, about $EST_MINUTES minutes")
        add("days since last study: ${profile.daysAway}")
        add("words in deck: ${profile.totalWords} (${pool.targetLanguage})")
        if (profile.longestStreak >= MIN_STREAK_WORTH_MENTIONING) {
            add("longest streak ever: ${profile.longestStreak} days")
        }

        val levelFive = pool.nearMastery.filter { it.level == 5 }
        if (levelFive.size >= 2) add("words one review from mastered: ${levelFive.size}")
        levelFive.take(MAX_WORD_FACTS_PER_BUCKET).forEach {
            add("near-mastery word \"${it.text}\" (${it.translation}): one review from mastered", setOf(it.text))
        }

        pool.weak.take(MAX_WORD_FACTS_PER_BUCKET).forEach {
            val misses = it.recent.count { r -> r == '0' }
            val lastTry = if (it.lastCorrect) ", right on the last try" else ""
            add("weak word \"${it.text}\" (${it.translation}): missed $misses of last ${it.recent.length}$lastTry", setOf(it.text))
        }

        profile.firstWord
            ?.takeIf { first -> pool.all.any { it.text == first.text } }
            ?.let { add("first word ever added: \"${it.text}\" (${it.translation})", setOf(it.text)) }

        return RescueFactSheet(facts)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.RescueFactSheetBuilderTest"`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueFactSheet.kt src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueFactSheetBuilderTest.kt
git commit -m "feat(rescue): grounded fact sheet for AI copy"
```

---

### Task 6: Prompt template

**Files:**
- Create: `src/main/resources/prompts/comeback-rescue.v1.txt`
- Modify: `src/main/kotlin/com/alirezaiyan/vokab/server/ai/PromptTemplates.kt` (enum `Prompt`)
- Modify: `src/main/kotlin/com/alirezaiyan/vokab/server/ai/AiClient.kt` (enum `AiOperation`)
- Test: `src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescuePromptTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import com.alirezaiyan.vokab.server.ai.Prompt
import com.alirezaiyan.vokab.server.ai.PromptTemplates
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComebackRescuePromptTest {

    @Test
    fun `renders with every rescue input`() {
        val prompt = PromptTemplates().render(
            Prompt.COMEBACK_RESCUE,
            mapOf(
                "uiLanguage" to "en",
                "targetLanguage" to "de",
                "sessionSize" to 8,
                "avoidAngle" to "rematch",
                "facts" to "F1 session: 8 words, about 3 minutes",
                "nearMastery" to "id=1 \"Hund\" (dog) level=4 recent=1111 errorRate=0.00",
                "weak" to "id=10 \"Gewitter\" (thunderstorm) level=2 recent=10100 errorRate=0.56",
            ),
        )

        assertTrue("F1 session: 8 words" in prompt)
        assertTrue("id=10 \"Gewitter\"" in prompt)
        assertTrue("Do not use the angle: rematch" in prompt)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.ComebackRescuePromptTest"`
Expected: FAIL — `Unresolved reference: COMEBACK_RESCUE`.

- [ ] **Step 3: Add enum entries**

In `ai/PromptTemplates.kt`, enum `Prompt`, after `NOTIFICATION_ADVICE("notification-advice.v1"),` add:
```kotlin
    COMEBACK_RESCUE("comeback-rescue.v1"),
```
In `ai/AiClient.kt`, enum `AiOperation`, after `NOTIFICATION_ADVICE("notification advice"),` add:
```kotlin
    COMEBACK_RESCUE("comeback rescue"),
```

- [ ] **Step 4: Write the template**

`src/main/resources/prompts/comeback-rescue.v1.txt` (the gold examples below are a first draft; P3 replaces them with Ali's hand-written set as `comeback-rescue.v2.txt`):
```text
You plan a 3-minute comeback session for a vocabulary learner who has missed a few days of study, and write the push notification that invites them back.

## Part 1: choose the words
Choose exactly {{sessionSize}} word ids from the candidates below.
- 5 or 6 from NEAR MASTERY, 2 or 3 from WEAK.
- Order them as a session arc: the first two are easy near-mastery wins, the weak words sit in the middle, the last word is an easy near-mastery win.
- Never put two easily confused words in one session (look-alikes, same root, near-synonyms).
- Prefer weak words with recent right answers over hopeless ones.
- Mix topics and parts of speech.
- Give a short reason for every pick.

NEAR MASTERY:
{{nearMastery}}

WEAK:
{{weak}}

## Part 2: write the push
FACTS (the only things you may say about the learner):
{{facts}}

Write 3 candidates, each with a different angle:
- rematch: a weak word they recently got right
- almost_there: a word one review from mastered
- callback: their first word, only if a fact names it
- progress_fact: a real total from the facts
- curiosity: ask if they still know a weak word's meaning
Do not use the angle: {{avoidAngle}}

Rules:
- Write in the language with code "{{uiLanguage}}". Target words stay in "{{targetLanguage}}", exactly as written in the facts.
- Name at least one word from your chosen session.
- Every number and every quoted word must come from the facts you cite in factIds.
- State the real cost: the number of words and about how many minutes.
- Title at most 40 characters, body at most 110 characters.
- Warm, dry, brief, a little playful. Sound like a friend who knows their deck.
- No guilt, no streak pressure, no "we miss you". At most a light nod to the time away.
- No hype words (journey, unlock, crush, level up, boost, supercharge), no "ready to", no "let's go", no em dashes, no emoji, at most one exclamation mark.

Good examples:
- rematch | Gewitter, round 3 | You got it right last time. 8 words, about 3 min, mostly ones you already know.
- almost_there | Schmetterling is one review from mastered | 4 more are right behind it. About 3 min.
- callback | Remember Apfel? | Your first German word. It's still yours, along with 7 others worth 3 minutes today.
- progress_fact | You know 212 German words | Keep them warm: 8 short ones, about 3 min.
- curiosity | Still know what Gewitter means? | One way to find out. 8 words, 3 min.

Bad examples:
- "Ready to crush your German goals? 🚀" (generic hype, no fact)
- "Don't let your progress slip away!" (guilt)
- "You've mastered 500 words!" (number not in the facts)

## Part 3: session outros
Write two lines shown when the session ends, same rules, at most 90 characters each:
- outroWin: they got the weak word(s) right
- outroSoft: they missed a weak word; kind, no pressure

Respond with ONLY valid JSON, no markdown:
{"wordIds":[...],"reasons":{"<id>":"..."},"candidates":[{"angle":"...","title":"...","body":"...","factIds":["F1"],"rank":1}],"outroWin":"...","outroSoft":"..."}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.ComebackRescuePromptTest"`
Expected: PASS. (`PromptTemplates` loads every template at construction, so a missing file or placeholder mismatch fails here.)

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/prompts/comeback-rescue.v1.txt src/main/kotlin/com/alirezaiyan/vokab/server/ai/PromptTemplates.kt src/main/kotlin/com/alirezaiyan/vokab/server/ai/AiClient.kt src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescuePromptTest.kt
git commit -m "feat(rescue): comeback rescue prompt with gold and anti examples"
```

---

### Task 7: AI response parsing and pick validation

**Files:**
- Create: `src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueAiResponse.kt`
- Create: `src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescuePlanValidator.kt`
- Test: `src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueAiResponseTest.kt`
- Test: `src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescuePlanValidatorTest.kt`

- [ ] **Step 1: Write the failing tests**

`RescueAiResponseTest.kt`:
```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RescueAiResponseTest {

    private val objectMapper = ObjectMapper()

    private val json = """
        {"wordIds":[1,2,10,3],"reasons":{"10":"weak, right last time"},
         "candidates":[{"angle":"rematch","title":"Gewitter, round 3","body":"8 words, about 3 min.","factIds":["F1","F5"],"rank":1}],
         "outroWin":"Gewitter: cracked.","outroSoft":"Gewitter will come round again.","extra":"ignored"}
    """.trimIndent()

    @Test
    fun `parses a full response and ignores unknown fields`() {
        val response = requireNotNull(RescueAiResponse.parse(json, objectMapper))

        assertEquals(listOf(1L, 2L, 10L, 3L), response.wordIds)
        assertEquals("weak, right last time", response.reasons["10"])
        assertEquals(listOf("F1", "F5"), response.candidates.single().factIds)
        assertEquals("Gewitter: cracked.", response.outroWin)
    }

    @Test
    fun `tolerates a markdown code fence`() {
        val fenced = "```json\n$json\n```"

        assertEquals(listOf(1L, 2L, 10L, 3L), RescueAiResponse.parse(fenced, objectMapper)?.wordIds)
    }

    @Test
    fun `returns null for text that is not JSON`() {
        assertNull(RescueAiResponse.parse("Sure! Here is your plan.", objectMapper))
    }
}
```

`RescuePlanValidatorTest.kt`:
```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RescuePlanValidatorTest {

    private val pool = RescuePool(
        sourceLanguage = "en",
        targetLanguage = "de",
        nearMastery = (1L..7L).map { candidateRow(it) },
        weak = listOf(candidateRow(20, level = 2), candidateRow(21, level = 2)),
    )

    @Test
    fun `accepts a well-formed session arc`() {
        val picks = listOf(1L, 2L, 20L, 3L, 21L, 4L, 5L, 6L)

        assertEquals(picks, RescuePlanValidator.validPicks(picks, pool))
    }

    @Test
    fun `rejects ids outside the pool`() {
        assertNull(RescuePlanValidator.validPicks(listOf(1L, 2L, 20L, 3L, 4L, 999L, 5L), pool))
    }

    @Test
    fun `rejects duplicates`() {
        assertNull(RescuePlanValidator.validPicks(listOf(1L, 1L, 20L, 3L, 4L, 5L), pool))
    }

    @Test
    fun `rejects too few or too many words`() {
        assertNull(RescuePlanValidator.validPicks(listOf(1L, 20L, 2L, 3L, 4L), pool))
        assertNull(RescuePlanValidator.validPicks((1L..7L).toList() + listOf(20L, 21L) + (1L..2L), pool))
    }

    @Test
    fun `needs at least one weak word and four near mastery words`() {
        assertNull(RescuePlanValidator.validPicks(listOf(1L, 2L, 3L, 4L, 5L, 6L), pool))          // no weak word
        val threeNear = RescuePool("en", "de", (1L..3L).map { candidateRow(it) }, (20L..22L).map { candidateRow(it, level = 2) })
        assertNull(RescuePlanValidator.validPicks(listOf(1L, 20L, 21L, 22L, 2L, 3L), threeNear)) // only 3 near mastery
    }

    @Test
    fun `opens and closes on near mastery words`() {
        assertNull(RescuePlanValidator.validPicks(listOf(20L, 1L, 2L, 3L, 4L, 5L), pool))
        assertNull(RescuePlanValidator.validPicks(listOf(1L, 2L, 3L, 4L, 5L, 20L), pool))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.RescueAiResponseTest" --tests "*.RescuePlanValidatorTest"`
Expected: FAIL — `Unresolved reference: RescueAiResponse`.

- [ ] **Step 3: Write minimal implementation**

`RescueAiResponse.kt`:
```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

@JsonIgnoreProperties(ignoreUnknown = true)
data class RescueCopyCandidate(
    val angle: String = "",
    val title: String = "",
    val body: String = "",
    val factIds: List<String> = emptyList(),
    /** The model's own preference, 1 = best. */
    val rank: Int = Int.MAX_VALUE,
)

/** The model's answer to the comeback-rescue prompt. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class RescueAiResponse(
    val wordIds: List<Long> = emptyList(),
    val reasons: Map<String, String> = emptyMap(),
    val candidates: List<RescueCopyCandidate> = emptyList(),
    val outroWin: String? = null,
    val outroSoft: String? = null,
) {
    companion object {
        /** Null when [raw] is not the expected JSON; the planner then falls back. */
        fun parse(raw: String, objectMapper: ObjectMapper): RescueAiResponse? {
            val json = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            return runCatching { objectMapper.readValue(json, RescueAiResponse::class.java) }
                .onFailure { logger.warn { "Unparsable comeback rescue response: ${it.message}" } }
                .getOrNull()
        }
    }
}
```

`RescuePlanValidator.kt`:
```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

/** Checks the AI's word picks; anything off and the planner uses [RescueFallback] instead. */
object RescuePlanValidator {
    val SIZE_RANGE = 6..10
    private const val MIN_WEAK = 1
    private const val MIN_NEAR_MASTERY = 4

    /** [ids] unchanged when they form a valid session arc from [pool], otherwise null. */
    fun validPicks(ids: List<Long>, pool: RescuePool): List<Long>? {
        if (ids.size !in SIZE_RANGE || ids.toSet().size != ids.size) return null

        val near = pool.nearMastery.map { it.wordId }.toSet()
        val weak = pool.weak.map { it.wordId }.toSet()
        if (ids.any { it !in near && it !in weak }) return null
        if (ids.count { it in weak } < MIN_WEAK || ids.count { it in near } < MIN_NEAR_MASTERY) return null
        if (ids.first() !in near || ids.last() !in near) return null

        return ids
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.RescueAiResponseTest" --tests "*.RescuePlanValidatorTest"`
Expected: PASS (3 + 6 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueAiResponse.kt src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescuePlanValidator.kt src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueAiResponseTest.kt src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescuePlanValidatorTest.kt
git commit -m "feat(rescue): parse AI plan and validate the word picks"
```

---

### Task 8: Copy linter

**Files:**
- Create: `src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueCopyLinter.kt`
- Test: `src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueCopyLinterTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RescueCopyLinterTest {

    private val sheet = RescueFactSheet(
        listOf(
            RescueFact("F1", "session: 8 words, about 3 minutes"),
            RescueFact("F2", "words in deck: 212 (de)"),
            RescueFact("F3", "weak word \"Gewitter\" (thunderstorm): missed 3 of last 5", setOf("Gewitter")),
        )
    )
    private val ctx = RescueCopyContext(
        factSheet = sheet,
        sessionWords = setOf("Gewitter", "Hund"),
        lastAngle = null,
        recentCopies = emptyList(),
    )

    private fun candidate(
        title: String = "Gewitter, one more go",
        body: String = "You were close last time. 8 words, about 3 min.",
        angle: String = "rematch",
        factIds: List<String> = listOf("F1", "F3"),
        rank: Int = 1,
    ) = RescueCopyCandidate(angle, title, body, factIds, rank)

    @Test
    fun `good copy has no violations`() {
        assertEquals(emptyList<String>(), RescueCopyLinter.lint(candidate(), ctx))
    }

    @Test
    fun `flags length by graphemes`() {
        val longTitle = "G".repeat(RescueCopyLinter.TITLE_MAX + 1)
        assertTrue(RescueCopyLinter.lint(candidate(title = "Gewitter $longTitle"), ctx).any { "title" in it })
        // 40 Persian letters are 40 graphemes even though they are multi-byte
        assertEquals(40, RescueCopyLinter.graphemes("س".repeat(40)))
    }

    @Test
    fun `must name a session word`() {
        assertTrue(RescueCopyLinter.lint(candidate(title = "One more go"), ctx).contains("names no session word"))
    }

    @Test
    fun `numbers must come from cited facts`() {
        val v = RescueCopyLinter.lint(candidate(body = "You know 500 words. 8 words, about 3 min."), ctx)
        assertTrue(v.contains("ungrounded number 500"))
        // 212 exists but F2 is not cited
        assertTrue(RescueCopyLinter.lint(candidate(body = "212 words! 8 words, about 3 min."), ctx).contains("ungrounded number 212"))
    }

    @Test
    fun `quoted words must come from the fact sheet`() {
        val v = RescueCopyLinter.lint(candidate(body = "Gewitter and \"Blitz\" wait. 8 words, about 3 min."), ctx)
        assertTrue(v.contains("ungrounded word Blitz"))
    }

    @Test
    fun `cited fact ids must exist`() {
        assertTrue(RescueCopyLinter.lint(candidate(factIds = listOf("F1", "F9")), ctx).contains("cites unknown fact F9"))
    }

    @Test
    fun `bans hype, guilt and AI tells`() {
        assertTrue(RescueCopyLinter.lint(candidate(title = "Ready to crush Gewitter?"), ctx).any { it.startsWith("banned phrase") })
        assertTrue(RescueCopyLinter.lint(candidate(body = "Gewitter — 8 words, about 3 min."), ctx).contains("em dash"))
        assertTrue(RescueCopyLinter.lint(candidate(body = "Go! Now! 8 words, about 3 min."), ctx).contains("too many exclamation marks"))
        assertTrue(RescueCopyLinter.lint(candidate(body = "8 words, about 3 min 🚀"), ctx).contains("emoji"))
        assertTrue(RescueCopyLinter.lint(candidate(body = "We miss you. 8 words, about 3 min."), ctx).any { it.startsWith("banned phrase") })
    }

    @Test
    fun `angle must be known and differ from the last one`() {
        assertTrue(RescueCopyLinter.lint(candidate(angle = "hype"), ctx).contains("unknown angle hype"))
        assertTrue(RescueCopyLinter.lint(candidate(), ctx.copy(lastAngle = "rematch")).contains("repeats last angle"))
    }

    @Test
    fun `must not repeat a recent copy`() {
        val recent = ctx.copy(recentCopies = listOf("gewitter, ONE more go!! You were close last time. 8 words, about 3 min"))
        assertTrue(RescueCopyLinter.lint(candidate(), recent).contains("repeats a recent copy"))
    }

    @Test
    fun `best picks the highest ranked candidate without violations`() {
        val bad = candidate(title = "Ready to crush it?", rank = 1)
        val good = candidate(angle = "curiosity", title = "Still know what Gewitter means?", body = "8 words, about 3 min.", rank = 2)

        val linted = RescueCopyLinter.lintAll(listOf(bad, good), ctx)

        assertEquals(good, RescueCopyLinter.best(linted)?.candidate)
        assertNull(RescueCopyLinter.best(RescueCopyLinter.lintAll(listOf(bad), ctx)))
    }

    @Test
    fun `outros follow the same tone rules`() {
        assertTrue(RescueCopyLinter.outroOk("Gewitter: cracked. 8 for 8.", sheet))
        assertTrue(!RescueCopyLinter.outroOk("Don't lose your streak!", sheet))
        assertTrue(!RescueCopyLinter.outroOk("You beat 99 learners.", sheet))
        assertTrue(!RescueCopyLinter.outroOk(null, sheet))
        assertTrue(!RescueCopyLinter.outroOk("x".repeat(RescueCopyLinter.OUTRO_MAX + 1), sheet))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.RescueCopyLinterTest"`
Expected: FAIL — `Unresolved reference: RescueCopyContext`.

- [ ] **Step 3: Write minimal implementation**

`RescueCopyLinter.kt`:
```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import java.text.BreakIterator

data class RescueCopyContext(
    val factSheet: RescueFactSheet,
    /** Target-language text of the words in this session. */
    val sessionWords: Set<String>,
    val lastAngle: String?,
    /** "title body" of the learner's recent rescues. */
    val recentCopies: List<String>,
)

data class LintedCandidate(val candidate: RescueCopyCandidate, val violations: List<String>) {
    val ok: Boolean get() = violations.isEmpty()
}

/**
 * Hard rules every rescue copy must pass: grounded in the fact sheet, names a session word, short,
 * and free of the hype, guilt and tics that make a push read as machine-written.
 */
object RescueCopyLinter {
    const val TITLE_MAX = 40
    const val BODY_MAX = 110
    const val OUTRO_MAX = 90

    val ANGLES = setOf("rematch", "almost_there", "callback", "progress_fact", "curiosity")

    val BANNED_PHRASES = listOf(
        "journey", "unlock", "crush", "level up", "boost", "supercharge",
        "don't lose", "don't break", "we miss you", "ready to", "let's go", "hey there", "oops",
    )

    private val NUMBER = Regex("""\d+""")
    private val QUOTED = Regex("""["“”«»]([^"“”«»]{1,40})["“”«»]""")

    fun lintAll(candidates: List<RescueCopyCandidate>, ctx: RescueCopyContext): List<LintedCandidate> =
        candidates.map { LintedCandidate(it, lint(it, ctx)) }

    /** The model's highest-ranked candidate that passed every rule, or null. */
    fun best(linted: List<LintedCandidate>): LintedCandidate? =
        linted.filter { it.ok }.minByOrNull { it.candidate.rank }

    fun lint(c: RescueCopyCandidate, ctx: RescueCopyContext): List<String> {
        val v = mutableListOf<String>()
        val text = "${c.title} ${c.body}"

        if (c.angle !in ANGLES) v += "unknown angle ${c.angle}"
        if (c.angle == ctx.lastAngle) v += "repeats last angle"
        if (c.title.isBlank() || graphemes(c.title) > TITLE_MAX) v += "title length"
        if (c.body.isBlank() || graphemes(c.body) > BODY_MAX) v += "body length"
        if (ctx.sessionWords.none { text.contains(it, ignoreCase = true) }) v += "names no session word"

        val unknownFacts = c.factIds.filter { id -> ctx.factSheet.facts.none { it.id == id } }
        unknownFacts.forEach { v += "cites unknown fact $it" }
        val citedNumbers = ctx.factSheet.byIds(c.factIds).flatMap { NUMBER.findAll(it.text).map { m -> m.value }.toList() }.toSet()
        NUMBER.findAll(text).map { it.value }.distinct().filter { it !in citedNumbers }.forEach { v += "ungrounded number $it" }
        QUOTED.findAll(text).map { it.groupValues[1] }.filter { it !in ctx.factSheet.allWords }.forEach { v += "ungrounded word $it" }

        v += toneViolations(text)

        val normalized = normalize(text)
        if (ctx.recentCopies.any { normalize(it) == normalized }) v += "repeats a recent copy"

        return v
    }

    /** An outro may be shown only if it is short, calm and invents no numbers. */
    fun outroOk(outro: String?, sheet: RescueFactSheet): Boolean {
        if (outro.isNullOrBlank() || graphemes(outro) > OUTRO_MAX) return false
        val knownNumbers = sheet.facts.flatMap { NUMBER.findAll(it.text).map { m -> m.value }.toList() }.toSet()
        if (NUMBER.findAll(outro).any { it.value !in knownNumbers }) return false
        return toneViolations(outro).isEmpty()
    }

    fun graphemes(s: String): Int {
        val it = BreakIterator.getCharacterInstance().apply { setText(s) }
        var count = 0
        while (it.next() != BreakIterator.DONE) count++
        return count
    }

    private fun toneViolations(text: String): List<String> {
        val v = mutableListOf<String>()
        val lower = text.lowercase().replace('’', '\'')
        BANNED_PHRASES.filter { it in lower }.forEach { v += "banned phrase $it" }
        if ('—' in text) v += "em dash"
        if (text.count { it == '!' } > 1) v += "too many exclamation marks"
        if (text.codePoints().anyMatch(::isEmoji)) v += "emoji"
        return v
    }

    private fun isEmoji(cp: Int): Boolean = cp >= 0x1F000 || cp in 0x2600..0x27BF

    private fun normalize(s: String): String = s.lowercase().filter { it.isLetterOrDigit() }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.RescueCopyLinterTest"`
Expected: PASS (11 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueCopyLinter.kt src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueCopyLinterTest.kt
git commit -m "feat(rescue): copy linter for grounded, human-sounding pushes"
```

---

### Task 9: Deterministic fallback

**Files:**
- Create: `src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueFallback.kt`
- Test: `src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueFallbackTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RescueFallbackTest {

    private val pool = RescuePool(
        sourceLanguage = "en",
        targetLanguage = "de",
        nearMastery = listOf(
            candidateRow(1, recent = "1111"), candidateRow(2, recent = "1110"), candidateRow(3, recent = "1111"),
            candidateRow(4, recent = "1100"), candidateRow(5, recent = "1111"), candidateRow(6, recent = "1111"),
            candidateRow(7, recent = "1111"),
        ),
        weak = listOf(
            candidateRow(20, text = "Gewitter", level = 2), candidateRow(21, level = 2), candidateRow(22, level = 2),
        ),
    )

    @Test
    fun `picks eight words as an arc with two weak words in the middle`() {
        val picks = RescueFallback.picks(pool)

        assertEquals(8, picks.size)
        assertEquals(listOf(20L, 21L), picks.subList(2, 4))
        assertTrue(picks.first() in pool.nearMastery.map { it.wordId })
        assertTrue(picks.last() in pool.nearMastery.map { it.wordId })
        assertNotNull(RescuePlanValidator.validPicks(picks, pool))
    }

    @Test
    fun `a minimal pool still makes a valid session`() {
        val minimal = pool.copy(nearMastery = pool.nearMastery.take(5), weak = pool.weak.take(1))

        val picks = RescueFallback.picks(minimal)

        assertEquals(6, picks.size)
        assertNotNull(RescuePlanValidator.validPicks(picks, minimal))
    }

    @Test
    fun `template copy names the first weak word and states the cost`() {
        val copy = RescueFallback.copy(pool, RescueFallback.picks(pool))

        assertEquals("rematch", copy.angle)
        assertEquals("Gewitter wants a rematch", copy.title)
        assertEquals("8 words, about 3 min. Most of them you already know.", copy.body)
        assertEquals("Gewitter: cracked. Nice comeback.", copy.outroWin)
    }

    @Test
    fun `long weak words get a generic title that fits`() {
        val long = pool.copy(weak = listOf(candidateRow(20, text = "Donaudampfschifffahrtsgesellschaft", level = 2)))

        val copy = RescueFallback.copy(long, RescueFallback.picks(long))

        assertTrue(RescueCopyLinter.graphemes(copy.title) <= RescueCopyLinter.TITLE_MAX)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.RescueFallbackTest"`
Expected: FAIL — `Unresolved reference: RescueFallback`.

- [ ] **Step 3: Write minimal implementation**

`RescueFallback.kt`:
```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

/** Copy for a rescue, from the AI or a template. */
data class RescueCopy(
    val angle: String,
    val title: String,
    val body: String,
    val outroWin: String?,
    val outroSoft: String?,
)

/**
 * The rescue without AI: the surest near-mastery words around the weak words closest to cracking,
 * and plain template copy. Used when the model is unavailable or its answer fails validation.
 */
object RescueFallback {
    private const val MAX_WEAK = 2

    /** Requires [RescuePool.canPlan]. Two easy wins, the weak words, then the remaining easy wins. */
    fun picks(pool: RescuePool): List<Long> {
        val weak = pool.weak.take(MAX_WEAK)
        val near = pool.nearMastery
            .sortedWith(compareByDescending<RescueCandidateRow> { it.recentAccuracy }.thenByDescending { it.level }.thenBy { it.wordId })
            .take(RescueFactSheetBuilder.SESSION_SIZE - weak.size)
        return (near.take(2) + weak + near.drop(2)).map { it.wordId }
    }

    fun copy(pool: RescuePool, picks: List<Long>): RescueCopy {
        val weakWord = pool.weak.first { it.wordId in picks }.text
        val title = "$weakWord wants a rematch".takeIf { RescueCopyLinter.graphemes(it) <= RescueCopyLinter.TITLE_MAX }
            ?: "A rematch is waiting"
        return RescueCopy(
            angle = "rematch",
            title = title,
            body = "${picks.size} words, about ${RescueFactSheetBuilder.EST_MINUTES} min. Most of them you already know.",
            outroWin = "$weakWord: cracked. Nice comeback.",
            outroSoft = "${picks.size} words back in rotation. $weakWord will come round again soon.",
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.RescueFallbackTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueFallback.kt src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescueFallbackTest.kt
git commit -m "feat(rescue): deterministic fallback picks and template copy"
```

---

### Task 10: Planner (orchestration)

**Files:**
- Create: `src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescuePlanner.kt`
- Test: `src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescuePlannerTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import com.alirezaiyan.vokab.server.TEST_TODAY
import com.alirezaiyan.vokab.server.ai.AiClient
import com.alirezaiyan.vokab.server.ai.AiOperation
import com.alirezaiyan.vokab.server.ai.PromptTemplates
import com.alirezaiyan.vokab.server.fixedClock
import com.alirezaiyan.vokab.server.shared.UpstreamServiceException
import com.alirezaiyan.vokab.server.study.DailyActivity
import com.alirezaiyan.vokab.server.study.DailyActivityRepository
import com.alirezaiyan.vokab.server.user.User
import com.alirezaiyan.vokab.server.user.UserSettingsRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ComebackRescuePlannerTest {

    private val rescueRepository: ComebackRescueRepository = mockk()
    private val candidateQueries: RescueCandidateQueries = mockk()
    private val dailyActivityRepository: DailyActivityRepository = mockk()
    private val userSettingsRepository: UserSettingsRepository = mockk()
    private val aiClient: AiClient = mockk()

    private lateinit var planner: ComebackRescuePlanner

    private val near = (1L..7L).map { candidateRow(it) }
    private val weak = listOf(
        candidateRow(20, level = 2, recent = "0101", total = 6, errors = 3),
        candidateRow(21, level = 2, recent = "0101", total = 6, errors = 3),
    )

    private val validAi = """
        {"wordIds":[1,2,20,3,21,4,5,6],"reasons":{"20":"weak, right last time"},
         "candidates":[
           {"angle":"rematch","title":"Ready to crush it?","body":"Let's go!","factIds":["F1"],"rank":1},
           {"angle":"curiosity","title":"Still know what w20 means?","body":"One way to find out. 8 words, about 3 min.","factIds":["F1"],"rank":2}
         ],
         "outroWin":"w20: cracked. 8 for 8.","outroSoft":"8 words back in rotation."}
    """.trimIndent()

    private fun user(id: Long = 1L) = User(id = id, email = "u$id@example.com", name = "U$id", longestStreak = 0)

    @BeforeEach
    fun setUp() {
        planner = ComebackRescuePlanner(
            rescueRepository, candidateQueries, dailyActivityRepository, userSettingsRepository,
            aiClient, PromptTemplates(), ObjectMapper(), fixedClock(),
        )
        every { rescueRepository.findByUserIdAndPlannedFor(any(), TEST_TODAY) } returns null
        every { rescueRepository.existsByUserIdAndPlannedForAfterAndStatusIn(any(), any(), any()) } returns false
        every { rescueRepository.findTop3ByUserIdOrderByPlannedForDesc(any()) } returns emptyList()
        every { rescueRepository.save(any()) } answers { firstArg() }
        every { candidateQueries.candidates(any()) } returns near + weak
        every { candidateQueries.totalWords(any()) } returns 40
        every { candidateQueries.firstWord(any(), any(), any()) } returns null
        every { userSettingsRepository.findByUserId(any()) } returns null
        studiedDaysAgo(3, 4, 5)
    }

    private fun studiedDaysAgo(vararg days: Long) {
        every { dailyActivityRepository.findRecentActivities(any(), any()) } answers {
            days.map { DailyActivity(user = firstArg(), activityDate = TEST_TODAY.minusDays(it)) }
        }
    }

    @Test
    fun `plans with AI picks and the best copy that passes the linter`() {
        every { aiClient.complete(any(), AiOperation.COMEBACK_RESCUE) } returns validAi

        val rescue = requireNotNull(planner.planIfEligible(user()))

        assertEquals(RescueSource.AI, rescue.source)
        assertEquals(RescueStatus.PLANNED, rescue.status)
        assertEquals(listOf(1L, 2L, 20L, 3L, 21L, 4L, 5L, 6L), rescue.wordIdList)
        assertEquals("curiosity", rescue.angle)
        assertEquals("Still know what w20 means?", rescue.title)
        assertEquals("w20: cracked. 8 for 8.", rescue.outroWin)
        assertEquals(TEST_TODAY, rescue.plannedFor)
    }

    @Test
    fun `falls back when the AI is unavailable`() {
        every { aiClient.complete(any(), any()) } throws UpstreamServiceException("down")

        val rescue = requireNotNull(planner.planIfEligible(user()))

        assertEquals(RescueSource.FALLBACK, rescue.source)
        assertEquals(RescueFallback.picks(requireNotNull(RescuePool.from(near + weak, fixedClock().millis()))), rescue.wordIdList)
    }

    @Test
    fun `falls back when the AI picks words outside the pool`() {
        every { aiClient.complete(any(), any()) } returns validAi.replace("[1,2,20,3,21,4,5,6]", "[1,2,20,3,999,4,5,6]")

        assertEquals(RescueSource.FALLBACK, planner.planIfEligible(user())?.source)
    }

    @Test
    fun `keeps AI picks with template copy when every candidate fails the linter`() {
        every { aiClient.complete(any(), any()) } returns validAi.replace("Still know what w20 means?", "Ready to crush w20?")

        val rescue = requireNotNull(planner.planIfEligible(user()))

        assertEquals(RescueSource.AI_PICKS_TEMPLATE_COPY, rescue.source)
        assertEquals(listOf(1L, 2L, 20L, 3L, 21L, 4L, 5L, 6L), rescue.wordIdList)
        assertEquals("w20 wants a rematch", rescue.title)
    }

    @Test
    fun `drops an AI outro that fails the tone rules`() {
        every { aiClient.complete(any(), any()) } returns validAi.replace("w20: cracked. 8 for 8.", "Don't lose w20!")

        assertNull(planner.planIfEligible(user())?.outroWin)
    }

    @Test
    fun `holdout learners get a plan that is never sent`() {
        every { aiClient.complete(any(), any()) } returns validAi

        assertEquals(RescueStatus.HOLDOUT, planner.planIfEligible(user(id = ComebackRescuePlanner.HOLDOUT_BUCKET.toLong()))?.status)
    }

    @Test
    fun `does nothing for an ineligible learner`() {
        studiedDaysAgo(1, 2, 3)

        assertNull(planner.planIfEligible(user()))
        verify(exactly = 0) { aiClient.complete(any(), any()) }
    }

    @Test
    fun `does nothing when the pool is too thin`() {
        every { candidateQueries.candidates(any()) } returns near.take(4) + weak

        assertNull(planner.planIfEligible(user()))
        verify(exactly = 0) { aiClient.complete(any(), any()) }
    }

    @Test
    fun `does nothing when today's plan already exists`() {
        every { rescueRepository.findByUserIdAndPlannedFor(1L, TEST_TODAY) } returns mockk()

        assertNull(planner.planIfEligible(user()))
        verify(exactly = 0) { rescueRepository.save(any()) }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.ComebackRescuePlannerTest"`
Expected: FAIL — `Unresolved reference: ComebackRescuePlanner`.

- [ ] **Step 3: Write minimal implementation**

`ComebackRescuePlanner.kt`:
```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import com.alirezaiyan.vokab.server.ai.AiClient
import com.alirezaiyan.vokab.server.ai.AiOperation
import com.alirezaiyan.vokab.server.ai.Prompt
import com.alirezaiyan.vokab.server.ai.PromptTemplates
import com.alirezaiyan.vokab.server.shared.UpstreamServiceException
import com.alirezaiyan.vokab.server.study.DailyActivityRepository
import com.alirezaiyan.vokab.server.user.User
import com.alirezaiyan.vokab.server.user.UserSettingsRepository
import com.alirezaiyan.vokab.server.user.requireId
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

/**
 * Builds today's comeback rescue for one learner: checks eligibility, gathers the candidate pool
 * and fact sheet, asks the model for picks and copy, validates both and falls back where needed,
 * then stores the plan. Called from the nightly batch; the AI call runs outside any transaction.
 */
@Service
class ComebackRescuePlanner(
    private val rescueRepository: ComebackRescueRepository,
    private val candidateQueries: RescueCandidateQueries,
    private val dailyActivityRepository: DailyActivityRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val aiClient: AiClient,
    private val promptTemplates: PromptTemplates,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {
    /** The stored plan, or null when the learner is not eligible or has too few usable words. */
    fun planIfEligible(user: User): ComebackRescue? {
        val userId = user.requireId()
        val today = LocalDate.now(clock)
        if (rescueRepository.findByUserIdAndPlannedFor(userId, today) != null) return null

        val studyDates = dailyActivityRepository
            .findRecentActivities(user, today.minusDays(ComebackRescuePolicy.LOOKBACK_DAYS))
            .map { it.activityDate }
        val lastStudy = studyDates.maxOrNull() ?: return null
        val totalWords = candidateQueries.totalWords(userId)
        val alreadyRescued = rescueRepository.existsByUserIdAndPlannedForAfterAndStatusIn(
            userId, lastStudy, RescueStatus.CONSUMES_LAPSE,
        )
        if (!ComebackRescuePolicy.isEligible(LapseSignals(studyDates, totalWords, alreadyRescued), today)) return null

        val pool = RescuePool.from(candidateQueries.candidates(userId), clock.millis())
            ?.takeIf { it.canPlan } ?: return null
        val profile = RescueProfile(
            daysAway = requireNotNull(ComebackRescuePolicy.daysAway(studyDates, today)),
            totalWords = totalWords,
            longestStreak = user.longestStreak,
            firstWord = candidateQueries.firstWord(userId, pool.sourceLanguage, pool.targetLanguage),
        )
        val sheet = RescueFactSheetBuilder.build(profile, pool)
        val recent = rescueRepository.findTop3ByUserIdOrderByPlannedForDesc(userId)
        val uiLanguage = userSettingsRepository.findByUserId(userId)?.languageCode ?: DEFAULT_UI_LANGUAGE

        val draft = aiDraft(userId, pool, sheet, recent, uiLanguage) ?: fallbackDraft(pool)
        val status = if (isHoldout(userId)) RescueStatus.HOLDOUT else RescueStatus.PLANNED

        return rescueRepository.save(
            ComebackRescue(
                userId = userId,
                plannedFor = today,
                wordIds = ComebackRescue.encodeWordIds(draft.wordIds),
                title = draft.copy.title,
                body = draft.copy.body,
                angle = draft.copy.angle,
                outroWin = draft.copy.outroWin,
                outroSoft = draft.copy.outroSoft,
                candidates = draft.candidatesJson,
                picksReasoning = draft.reasoningJson,
                source = draft.source,
                status = status,
            )
        ).also { logger.info { "Comeback rescue planned for user=$userId source=${it.source} status=${it.status}" } }
    }

    private data class Draft(
        val wordIds: List<Long>,
        val copy: RescueCopy,
        val source: RescueSource,
        val candidatesJson: String? = null,
        val reasoningJson: String? = null,
    )

    private fun aiDraft(
        userId: Long,
        pool: RescuePool,
        sheet: RescueFactSheet,
        recent: List<ComebackRescue>,
        uiLanguage: String,
    ): Draft? {
        val lastAngle = recent.firstOrNull()?.angle
        val raw = try {
            aiClient.complete(prompt(pool, sheet, lastAngle, uiLanguage), AiOperation.COMEBACK_RESCUE)
        } catch (e: UpstreamServiceException) {
            logger.warn { "Comeback rescue AI unavailable for user=$userId: ${e.message}" }
            null
        } ?: return null
        val response = RescueAiResponse.parse(raw, objectMapper) ?: return null
        val picks = RescuePlanValidator.validPicks(response.wordIds, pool) ?: run {
            logger.warn { "Comeback rescue AI picks rejected for user=$userId: ${response.wordIds}" }
            return null
        }

        val sessionWords = pool.all.filter { it.wordId in picks }.map { it.text }.toSet()
        val ctx = RescueCopyContext(sheet, sessionWords, lastAngle, recent.map { "${it.title} ${it.body}" })
        val linted = RescueCopyLinter.lintAll(response.candidates, ctx)
        val best = RescueCopyLinter.best(linted)?.candidate

        val copy = if (best != null) {
            RescueCopy(
                angle = best.angle,
                title = best.title,
                body = best.body,
                outroWin = response.outroWin?.takeIf { RescueCopyLinter.outroOk(it, sheet) },
                outroSoft = response.outroSoft?.takeIf { RescueCopyLinter.outroOk(it, sheet) },
            )
        } else {
            RescueFallback.copy(pool, picks)
        }

        return Draft(
            wordIds = picks,
            copy = copy,
            source = if (best != null) RescueSource.AI else RescueSource.AI_PICKS_TEMPLATE_COPY,
            candidatesJson = objectMapper.writeValueAsString(
                linted.map { mapOf("candidate" to it.candidate, "violations" to it.violations) }
            ),
            reasoningJson = objectMapper.writeValueAsString(response.reasons),
        )
    }

    private fun fallbackDraft(pool: RescuePool): Draft {
        val picks = RescueFallback.picks(pool)
        return Draft(picks, RescueFallback.copy(pool, picks), RescueSource.FALLBACK)
    }

    private fun prompt(pool: RescuePool, sheet: RescueFactSheet, lastAngle: String?, uiLanguage: String): String =
        promptTemplates.render(
            Prompt.COMEBACK_RESCUE,
            mapOf(
                "uiLanguage" to uiLanguage,
                "targetLanguage" to pool.targetLanguage,
                "sessionSize" to RescueFactSheetBuilder.SESSION_SIZE,
                "avoidAngle" to (lastAngle ?: "none"),
                "facts" to sheet.render(),
                "nearMastery" to pool.nearMastery.joinToString("\n", transform = ::candidateLine),
                "weak" to pool.weak.joinToString("\n", transform = ::candidateLine),
            ),
        )

    private fun candidateLine(r: RescueCandidateRow): String =
        "id=${r.wordId} \"${r.text}\" (${r.translation}) level=${r.level} recent=${r.recent} " +
            "errorRate=${"%.2f".format(r.errorRate)}"

    companion object {
        /** Learners whose id ends in this digit (10%) form the holdout group: planned, never sent. */
        const val HOLDOUT_BUCKET = 7
        private const val DEFAULT_UI_LANGUAGE = "en"

        fun isHoldout(userId: Long): Boolean = Math.floorMod(userId, 10L) == HOLDOUT_BUCKET.toLong()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.ComebackRescuePlannerTest"`
Expected: PASS (9 tests). If `UpstreamServiceException("down")` doesn't compile, check its constructor in `shared/` and pass the arguments it needs.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescuePlanner.kt src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescuePlannerTest.kt
git commit -m "feat(rescue): plan a learner's comeback session with AI and fallback"
```

---

### Task 11: Nightly scheduling

**Files:**
- Create: `src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescueScheduler.kt`
- Modify: `src/main/kotlin/com/alirezaiyan/vokab/server/notification/NotificationScheduledTasks.kt`
- Test: `src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescueSchedulerTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import com.alirezaiyan.vokab.server.TEST_TODAY
import com.alirezaiyan.vokab.server.fixedClock
import com.alirezaiyan.vokab.server.user.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class ComebackRescueSchedulerTest {

    private val planner: ComebackRescuePlanner = mockk()
    private val repository: ComebackRescueRepository = mockk()
    private val scheduler = ComebackRescueScheduler(planner, repository, fixedClock())

    private fun user(id: Long) = User(id = id, email = "u$id@example.com", name = "U$id")

    @Test
    fun `expires yesterday's unsent plans, then plans every user`() {
        every { repository.expireStale(TEST_TODAY, RescueStatus.PLANNED, RescueStatus.EXPIRED) } returns 2
        every { planner.planIfEligible(any()) } returns null

        scheduler.planAll(listOf(user(1), user(2)))

        verify(exactly = 1) { repository.expireStale(TEST_TODAY, RescueStatus.PLANNED, RescueStatus.EXPIRED) }
        verify(exactly = 2) { planner.planIfEligible(any()) }
    }

    @Test
    fun `one user's failure does not stop the batch`() {
        every { repository.expireStale(any(), any(), any()) } returns 0
        every { planner.planIfEligible(match { it.id == 1L }) } throws IllegalStateException("boom")
        every { planner.planIfEligible(match { it.id == 2L }) } returns null

        scheduler.planAll(listOf(user(1), user(2)))

        verify(exactly = 1) { planner.planIfEligible(match { it.id == 2L }) }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.ComebackRescueSchedulerTest"`
Expected: FAIL — `Unresolved reference: ComebackRescueScheduler`.

- [ ] **Step 3: Write minimal implementation**

`ComebackRescueScheduler.kt`:
```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import com.alirezaiyan.vokab.server.user.User
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

/**
 * Nightly batch: retires yesterday's unsent plans and plans today's rescues.
 * Invoked by NotificationScheduledTasks — not a scheduler itself.
 */
@Service
class ComebackRescueScheduler(
    private val planner: ComebackRescuePlanner,
    private val repository: ComebackRescueRepository,
    private val clock: Clock,
) {
    fun planAll(users: List<User>) {
        val expired = repository.expireStale(LocalDate.now(clock), RescueStatus.PLANNED, RescueStatus.EXPIRED)
        var planned = 0
        var failed = 0

        for (user in users) {
            runCatching { planner.planIfEligible(user) }
                .onSuccess { if (it != null) planned++ }
                .onFailure { e ->
                    failed++
                    logger.warn(e) { "Comeback rescue planning failed for user=${user.id}" }
                }
        }

        logger.info { "Comeback rescue planning complete — users=${users.size}, planned=$planned, failed=$failed, expired=$expired" }
    }
}
```

In `NotificationScheduledTasks.kt`, add the constructor parameter and a call after the segment refresh inside `refreshEngagementSegments`:
```kotlin
// constructor: add after engagementSegmentScheduler
    private val comebackRescueScheduler: com.alirezaiyan.vokab.server.notification.rescue.ComebackRescueScheduler,
```
(Use a proper import `import com.alirezaiyan.vokab.server.notification.rescue.ComebackRescueScheduler` and the short name.)

Replace `refreshEngagementSegments` with:
```kotlin
    @Scheduled(cron = "0 0 1 * * *")           // 01:00 UTC nightly (after timing refresh)
    fun refreshEngagementSegments() {
        logger.info { "Starting nightly engagement segment refresh" }
        val activeUsers = try {
            userRepository.findAllActiveUsersWithPushTokens()
        } catch (e: Exception) {
            logger.error(e) { "Error loading users for nightly engagement refresh" }
            return
        }
        try {
            engagementSegmentScheduler.refreshAll(activeUsers)
        } catch (e: Exception) {
            logger.error(e) { "Error in engagement segment refresh" }
        }
        // After segments, so a rescue is planned against today's state; independent failure
        try {
            comebackRescueScheduler.planAll(activeUsers)
        } catch (e: Exception) {
            logger.error(e) { "Error in comeback rescue planning" }
        }
    }
```

- [ ] **Step 4: Check for other constructions of NotificationScheduledTasks**

Run: `grep -rn "NotificationScheduledTasks(" src/test src/main`
Expected: only the class declaration. If a test constructs it, add `comebackRescueScheduler = mockk(relaxed = true)` there.

- [ ] **Step 5: Run tests to verify they pass**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.ComebackRescueSchedulerTest"`
Expected: PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescueScheduler.kt src/main/kotlin/com/alirezaiyan/vokab/server/notification/NotificationScheduledTasks.kt src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescueSchedulerTest.kt
git commit -m "feat(rescue): plan comeback rescues in the nightly job"
```

---

### Task 12: Rescue service and push payload

**Files:**
- Create: `src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescueService.kt`
- Create: `src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescuePushBuilder.kt`
- Modify: `src/main/kotlin/com/alirezaiyan/vokab/server/notification/NotificationTypeSelector.kt` (enum)
- Modify: `src/main/kotlin/com/alirezaiyan/vokab/server/notification/NotificationContentBuilder.kt` (`when` branch)
- Modify: `src/main/resources/application.yml`
- Test: `src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescueServiceTest.kt`
- Test: `src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/RescuePushBuilderTest.kt`

- [ ] **Step 1: Write the failing tests**

`RescuePushBuilderTest.kt`:
```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import com.alirezaiyan.vokab.server.notification.NotificationTypeSelector.NotificationType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RescuePushBuilderTest {

    private fun rescue(outroWin: String? = "Gewitter: cracked.", outroSoft: String? = "Soon.") = ComebackRescue(
        id = 42L,
        userId = 1L,
        plannedFor = LocalDate.of(2026, 6, 17),
        wordIds = "[5, 1, 20, 3]",
        title = "Gewitter, round 3",
        body = "8 words, about 3 min.",
        angle = "rematch",
        outroWin = outroWin,
        outroSoft = outroSoft,
        source = RescueSource.AI,
        status = RescueStatus.PLANNED,
    )

    @Test
    fun `carries the ordered words and rescue id in the deep link`() {
        val payload = RescuePushBuilder.build(rescue())

        assertEquals(NotificationType.COMEBACK_RESCUE, payload.type)
        assertEquals("Gewitter, round 3", payload.title)
        assertEquals("comeback_rescue", payload.data["type"])
        assertEquals("vokab://review/rescue?ids=5,1,20,3&rid=42", payload.data["deep_link"])
        assertEquals("42", payload.data["rid"])
        assertEquals("Gewitter: cracked.", payload.data["outro_win"])
        assertEquals("Soon.", payload.data["outro_soft"])
    }

    @Test
    fun `omits missing outros`() {
        val payload = RescuePushBuilder.build(rescue(outroWin = null, outroSoft = null))

        assertFalse("outro_win" in payload.data)
        assertFalse("outro_soft" in payload.data)
    }
}
```

`ComebackRescueServiceTest.kt`:
```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import com.alirezaiyan.vokab.server.TEST_NOW
import com.alirezaiyan.vokab.server.TEST_TODAY
import com.alirezaiyan.vokab.server.fixedClock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Optional

class ComebackRescueServiceTest {

    private val repository: ComebackRescueRepository = mockk()

    private fun service(sendEnabled: Boolean = true) = ComebackRescueService(repository, fixedClock(), sendEnabled)

    private fun rescue(status: RescueStatus, userId: Long = 1L) = ComebackRescue(
        id = 42L, userId = userId, plannedFor = TEST_TODAY, wordIds = "[1]", title = "t", body = "b",
        angle = "rematch", source = RescueSource.AI, status = status,
    )

    @Test
    fun `today's planned rescue is sendable when sending is enabled`() {
        every { repository.findByUserIdAndPlannedFor(1L, TEST_TODAY) } returns rescue(RescueStatus.PLANNED)

        assertEquals(42L, service().sendablePlan(1L)?.id)
    }

    @Test
    fun `nothing is sendable in shadow mode`() {
        every { repository.findByUserIdAndPlannedFor(1L, TEST_TODAY) } returns rescue(RescueStatus.PLANNED)

        assertNull(service(sendEnabled = false).sendablePlan(1L))
    }

    @Test
    fun `holdout, sent and expired plans are not sendable`() {
        listOf(RescueStatus.HOLDOUT, RescueStatus.SENT, RescueStatus.EXPIRED).forEach { status ->
            every { repository.findByUserIdAndPlannedFor(1L, TEST_TODAY) } returns rescue(status)
            assertNull(service().sendablePlan(1L), "status=$status")
        }
    }

    @Test
    fun `marking sent records the time and notification log`() {
        val r = rescue(RescueStatus.PLANNED)
        every { repository.findById(42L) } returns Optional.of(r)
        every { repository.save(r) } returns r

        service().markSent(42L, notificationLogId = 7L)

        assertEquals(RescueStatus.SENT, r.status)
        assertEquals(TEST_NOW, r.sentAt)
        assertEquals(7L, r.notificationLogId)
    }

    @Test
    fun `completing a sent rescue is idempotent`() {
        val r = rescue(RescueStatus.SENT)
        every { repository.findById(42L) } returns Optional.of(r)
        every { repository.save(r) } returns r

        assertTrue(service().complete(userId = 1L, rescueId = 42L))
        assertTrue(service().complete(userId = 1L, rescueId = 42L))

        assertEquals(RescueStatus.COMPLETED, r.status)
        assertEquals(TEST_NOW, r.completedAt)
        verify(exactly = 1) { repository.save(r) }
    }

    @Test
    fun `another user's or a missing rescue cannot be completed`() {
        every { repository.findById(42L) } returns Optional.of(rescue(RescueStatus.SENT, userId = 2L))
        every { repository.findById(43L) } returns Optional.empty()

        assertFalse(service().complete(userId = 1L, rescueId = 42L))
        assertFalse(service().complete(userId = 1L, rescueId = 43L))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.RescuePushBuilderTest" --tests "*.ComebackRescueServiceTest"`
Expected: FAIL — `Unresolved reference: COMEBACK_RESCUE` / `ComebackRescueService`.

- [ ] **Step 3: Add the notification type**

In `NotificationTypeSelector.kt`, enum `NotificationType`, after `LISTENING,` add:
```kotlin
        COMEBACK_RESCUE, // Early-lapse quick-win session planned nightly; see notification.rescue
```

In `NotificationContentBuilder.build`, add a branch before `NONE`:
```kotlin
            COMEBACK_RESCUE    -> error("Comeback rescue pushes are built from their plan by RescuePushBuilder")
```

Then find any other exhaustive `when` over `NotificationType`:
Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew compileKotlin 2>&1 | grep -E "^e:" | head`
Expected: no errors. If a `when` is reported non-exhaustive (e.g. `PracticeNudgeCopy.kt`, `NotificationCategory.kt`), add `COMEBACK_RESCUE` to its "else"/default group matching how `COMEBACK_ALERT` is treated there.

- [ ] **Step 4: Write the push builder and service**

`RescuePushBuilder.kt`:
```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import com.alirezaiyan.vokab.server.notification.NotificationPayload
import com.alirezaiyan.vokab.server.notification.NotificationTypeSelector.NotificationType

/**
 * The push for a planned rescue. The deep link carries the words in session order and the rescue
 * id; clients that don't know the link fall back to the due review (`vokab://review/` prefix).
 */
object RescuePushBuilder {
    const val PUSH_TYPE = "comeback_rescue"

    fun build(rescue: ComebackRescue): NotificationPayload {
        val rid = requireNotNull(rescue.id) { "Rescue must be saved before it is sent" }
        return NotificationPayload(
            title = rescue.title,
            body = rescue.body,
            data = buildMap {
                put("type", PUSH_TYPE)
                put("deep_link", "vokab://review/rescue?ids=${rescue.wordIdList.joinToString(",")}&rid=$rid")
                put("rid", rid.toString())
                rescue.outroWin?.let { put("outro_win", it) }
                rescue.outroSoft?.let { put("outro_soft", it) }
            },
            type = NotificationType.COMEBACK_RESCUE,
        )
    }
}
```

`ComebackRescueService.kt`:
```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/** Rescue lifecycle after planning: what to send today, sent, completed. */
@Service
class ComebackRescueService(
    private val repository: ComebackRescueRepository,
    private val clock: Clock,
    /** Shadow mode while false: plans are built nightly but never sent. */
    @Value("\${app.comeback-rescue.send-enabled:false}") private val sendEnabled: Boolean,
) {
    @Transactional(readOnly = true)
    fun sendablePlan(userId: Long): ComebackRescue? {
        if (!sendEnabled) return null
        return repository.findByUserIdAndPlannedFor(userId, LocalDate.now(clock))
            ?.takeIf { it.status == RescueStatus.PLANNED }
    }

    @Transactional
    fun markSent(rescueId: Long, notificationLogId: Long?) {
        repository.findById(rescueId).ifPresent { rescue ->
            rescue.status = RescueStatus.SENT
            rescue.sentAt = Instant.now(clock)
            rescue.notificationLogId = notificationLogId
            repository.save(rescue)
        }
    }

    /** False when the rescue doesn't exist or isn't the user's; repeated calls are no-ops. */
    @Transactional
    fun complete(userId: Long, rescueId: Long): Boolean {
        val rescue = repository.findById(rescueId).orElse(null) ?: return false
        if (rescue.userId != userId) return false
        if (rescue.status == RescueStatus.SENT) {
            rescue.status = RescueStatus.COMPLETED
            rescue.completedAt = Instant.now(clock)
            repository.save(rescue)
        }
        return true
    }
}
```

In `src/main/resources/application.yml`, under the existing top-level `app:` key (line ~78), add:
```yaml
  comeback-rescue:
    # Shadow mode until the P3 taste test passes: plans are built nightly, never sent
    send-enabled: ${COMEBACK_RESCUE_SEND_ENABLED:false}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.RescuePushBuilderTest" --tests "*.ComebackRescueServiceTest"`
Expected: PASS (2 + 6 tests).

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/notification/ src/main/resources/application.yml src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/
git commit -m "feat(rescue): rescue push payload with deep link and send-enabled flag"
```

---

### Task 13: Send rescues from the smart dispatcher

**Files:**
- Modify: `src/main/kotlin/com/alirezaiyan/vokab/server/notification/SmartNotificationDispatcher.kt`
- Modify: `src/test/kotlin/com/alirezaiyan/vokab/server/notification/SmartNotificationDispatcherTest.kt`

- [ ] **Step 1: Update existing test setup (mock audit) and write the failing tests**

In `SmartNotificationDispatcherTest.kt`:
1. Add field: `private val comebackRescueService: com.alirezaiyan.vokab.server.notification.rescue.ComebackRescueService = mockk()`
2. In `setUp()`, add: `every { comebackRescueService.sendablePlan(any()) } returns null` and `justRun { comebackRescueService.markSent(any(), any()) }`
3. In `dispatcherAt(...)`, pass `comebackRescueService` right after `dailyInsightService,` (match the constructor order in Step 3).
4. Add these tests:

```kotlin
    // ── Comeback rescue ───────────────────────────────────────────────────────────

    private fun plannedRescue(userId: Long) = com.alirezaiyan.vokab.server.notification.rescue.ComebackRescue(
        id = 42L, userId = userId, plannedFor = com.alirezaiyan.vokab.server.TEST_TODAY,
        wordIds = "[5,1,20]", title = "Gewitter, round 3", body = "8 words, about 3 min.", angle = "rematch",
        source = com.alirezaiyan.vokab.server.notification.rescue.RescueSource.AI,
        status = com.alirezaiyan.vokab.server.notification.rescue.RescueStatus.PLANNED,
    )

    @Test
    fun `should send today's comeback rescue before any other push`() {
        val user     = testUser(id = 5L)
        val schedule = testSchedule(user)
        val data     = slot<Map<String, String>>()

        every { notificationScheduleRepository.findUsersToNotifyAtHour(any()) } returns listOf(schedule)
        every { comebackRescueService.sendablePlan(5L) } returns plannedRescue(5L)
        every { notificationEngagementService.saveLog(any(), any(), any(), any(), any()) } returns 88L
        every { pushNotificationService.sendNotificationToUser(userId = 5L, title = "Gewitter, round 3", body = any(), data = capture(data)) } returns
            listOf(NotificationResponse(success = true))
        justRun { notificationEngagementService.recordSend(schedule, NotificationType.COMEBACK_RESCUE.name, 88L) }

        dispatcher.dispatchForCurrentHour()

        assertEquals("vokab://review/rescue?ids=5,1,20&rid=42", data.captured["deep_link"])
        verify(exactly = 1) { comebackRescueService.markSent(42L, 88L) }
        verify(exactly = 0) { notificationTypeSelector.selectType(any(), any()) }
    }

    @Test
    fun `should not send a rescue to a learner who studied today`() {
        val user     = testUser(id = 6L)
        val schedule = testSchedule(user)

        every { notificationScheduleRepository.findUsersToNotifyAtHour(any()) } returns listOf(schedule)
        every { notificationTypeSelector.studiedToday(user) } returns true
        every { notificationTypeSelector.selectType(user, schedule) } returns NotificationType.NONE

        dispatcher.dispatchForCurrentHour()

        verify(exactly = 0) { comebackRescueService.sendablePlan(any()) }
        verify(exactly = 0) { pushNotificationService.sendNotificationToUser(any(), any(), any(), any()) }
    }

    @Test
    fun `should not mark a rescue sent when delivery fails`() {
        val user     = testUser(id = 7L)
        val schedule = testSchedule(user)

        every { notificationScheduleRepository.findUsersToNotifyAtHour(any()) } returns listOf(schedule)
        every { comebackRescueService.sendablePlan(7L) } returns plannedRescue(7L)
        every { notificationEngagementService.saveLog(any(), any(), any(), any(), any()) } returns 89L
        every { pushNotificationService.sendNotificationToUser(any(), any(), any(), any()) } returns
            listOf(NotificationResponse(success = false))
        justRun { notificationEngagementService.deleteLog(89L) }

        dispatcher.dispatchForCurrentHour()

        verify(exactly = 0) { comebackRescueService.markSent(any(), any()) }
    }
```

Note: if `testUser(...)` in this file sets a COLD/DORMANT segment or AI action by default, the rescue must still win — the dispatcher checks it first.

- [ ] **Step 2: Run tests to verify they fail**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.SmartNotificationDispatcherTest"`
Expected: FAIL — compilation error, constructor has no `comebackRescueService` parameter.

- [ ] **Step 3: Implement in the dispatcher**

In `SmartNotificationDispatcher.kt`:

Add imports:
```kotlin
import com.alirezaiyan.vokab.server.notification.rescue.ComebackRescue
import com.alirezaiyan.vokab.server.notification.rescue.ComebackRescueService
import com.alirezaiyan.vokab.server.notification.rescue.RescuePushBuilder
```

Add constructor parameter after `dailyInsightService`:
```kotlin
    private val comebackRescueService: ComebackRescueService,
```

In `dispatchForUser`, directly after `val studiedToday = notificationTypeSelector.studiedToday(user)`, insert:
```kotlin
        // A learner in an early lapse gets the rescue planned for them tonight. It outranks every
        // other push, the COLD/DORMANT AI decision included: it exists for exactly this day.
        if (!studiedToday) {
            comebackRescueService.sendablePlan(userId)?.let { rescue ->
                sendRescue(schedule, rescue)
                return
            }
        }
```

Replace `sendAndRecord` with these three functions (the delivery body is unchanged, only extracted):
```kotlin
    private fun sendAndRecord(
        schedule: NotificationSchedule,
        type: NotificationType,
        contentHint: String?
    ) {
        val payload = notificationContentBuilder.build(schedule.user, type, contentHint)
        if (deliver(schedule, payload) != null && type == NotificationType.PROGRESS_MILESTONE) {
            milestoneDetector.recordMilestoneSnapshot(schedule.user)
        }
    }

    private fun sendRescue(schedule: NotificationSchedule, rescue: ComebackRescue) {
        val delivery = deliver(schedule, RescuePushBuilder.build(rescue)) ?: return
        comebackRescueService.markSent(requireNotNull(rescue.id), delivery.logId)
    }

    private data class Delivery(val logId: Long?)

    /** Logs, pushes and records [payload]; the delivery when a device got it, otherwise null. */
    private fun deliver(schedule: NotificationSchedule, payload: NotificationPayload): Delivery? {
        val user   = schedule.user
        val userId = user.requireId()
        val type   = payload.type

        // The log is created before the push so its id can ride in the payload and come back
        // with the tap (POST /notifications/{id}/opened). Without a log we still send — only
        // open tracking for this one notification is lost.
        val logId = runCatching {
            notificationEngagementService.saveLog(
                userId           = userId,
                notificationType = type.name,
                title            = payload.title,
                body             = payload.body,
                data             = payload.data,
            )
        }.onFailure { e ->
            logger.error(e) { "Notification log persist failed for user=$userId — sending without open tracking" }
        }.getOrNull()

        val data = if (logId != null) payload.data + (NOTIFICATION_LOG_ID_KEY to logId.toString()) else payload.data
        val results = runCatching {
            pushNotificationService.sendNotificationToUser(
                userId = userId,
                title  = payload.title,
                body   = payload.body,
                data   = data
            )
        }.onFailure { discardLog(logId) }.getOrThrow()

        if (results.none { it.success }) {
            discardLog(logId)
            meterRegistry.counter("notifications.failed", "type", type.name).increment()
            logger.warn { "Push delivery failed for user=$userId, type=$type" }
            return null
        }

        notificationEngagementService.recordSend(schedule, type.name, currentLogId = logId)
        meterRegistry.counter("notifications.sent", "type", type.name).increment()
        logger.info { "Sent $type (segment=${schedule.engagementSegment}) to user=$userId" }
        return Delivery(logId)
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.SmartNotificationDispatcherTest"`
Expected: PASS — all existing tests plus 3 new ones. Existing milestone-snapshot tests must still pass unchanged.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/notification/SmartNotificationDispatcher.kt src/test/kotlin/com/alirezaiyan/vokab/server/notification/SmartNotificationDispatcherTest.kt
git commit -m "feat(rescue): send planned comeback rescues before other pushes"
```

---

### Task 14: Complete endpoint

**Files:**
- Create: `src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescueController.kt`
- Test: `src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescueControllerTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

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
import org.springframework.test.web.servlet.post

@WebMvcTest(ComebackRescueController::class)
@ActiveProfiles("test")
@Import(ControllerTestSecurityConfig::class)
class ComebackRescueControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var service: ComebackRescueService

    private val auth = UsernamePasswordAuthenticationToken(AuthUser(1L), null, emptyList())

    @Test
    fun `completes the user's rescue`() {
        `when`(service.complete(1L, 42L)).thenReturn(true)

        mockMvc.post("/api/v1/notifications/rescue/42/complete") { with(authentication(auth)) }
            .andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
            }
    }

    @Test
    fun `unknown or foreign rescue is not found`() {
        `when`(service.complete(1L, 43L)).thenReturn(false)

        mockMvc.post("/api/v1/notifications/rescue/43/complete") { with(authentication(auth)) }
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `requires authentication`() {
        mockMvc.post("/api/v1/notifications/rescue/42/complete")
            .andExpect { status { is4xxClientError() } }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.ComebackRescueControllerTest"`
Expected: FAIL — `Unresolved reference: ComebackRescueController`.

- [ ] **Step 3: Write minimal implementation**

`ComebackRescueController.kt`:
```kotlin
package com.alirezaiyan.vokab.server.notification.rescue

import com.alirezaiyan.vokab.server.shared.ApiResponse
import com.alirezaiyan.vokab.server.shared.AuthUser
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications/rescue")
class ComebackRescueController(private val service: ComebackRescueService) {

    /** The client finished the rescue session. Idempotent; unknown and foreign ids both 404. */
    @PostMapping("/{rescueId}/complete")
    fun complete(
        @AuthenticationPrincipal user: AuthUser,
        @PathVariable rescueId: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        if (!service.complete(user.id, rescueId)) throw NoSuchElementException("Rescue not found")
        return ResponseEntity.ok(ApiResponse(success = true))
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "*.ComebackRescueControllerTest"`
Expected: PASS (3 tests). If `requires authentication` returns 200 because `ControllerTestSecurityConfig` permits all, delete that test (security for `/api/v1/notifications/**` is covered by the app's real `SecurityConfig`) and note it in the commit message.

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescueController.kt src/test/kotlin/com/alirezaiyan/vokab/server/notification/rescue/ComebackRescueControllerTest.kt
git commit -m "feat(rescue): endpoint to mark a rescue session completed"
```

---

### Task 15: Full verification

- [ ] **Step 1: Full test suite + coverage**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew build`
Expected: `BUILD SUCCESSFUL`, 0 test failures, JaCoCo ≥ 80%. Fix any failure, including pre-existing ones.

- [ ] **Step 2: Boot locally and check the migration and health**

Run: `JAVA_HOME=$(/usr/libexec/java_home -v 21) ./scripts/start-dev.sh` (background), then
`curl -s http://localhost:8080/api/v1/health`
Expected: healthy response; startup log shows Flyway `Successfully applied ... v46` (or "Schema ... is up to date").

- [ ] **Step 3: Exercise the planner against a real model once**

With the local server running and `OPENROUTER_*` env set, trigger planning for a seeded lapsed user through a Spring Boot test runner or by temporarily calling `ComebackRescueScheduler.planAll` from a `@SpringBootTest` scratch test (do not commit it). Inspect the row:
```sql
SELECT source, status, angle, title, body, outro_win, word_ids, candidates FROM comeback_rescue ORDER BY id DESC LIMIT 1;
```
Expected: `source = AI` (or `AI_PICKS_TEMPLATE_COPY` with violations listed in `candidates`), copy names a real word, numbers match the facts. Paste the row into the P1 report.

- [ ] **Step 4: Confirm shadow mode**

Expected: `app.comeback-rescue.send-enabled` resolves to `false` in prod config (no `COMEBACK_RESCUE_SEND_ENABLED` set). Plans are built, nothing is sent.

- [ ] **Step 5: Push and deploy**

```bash
git push
ali server
ali health
```
Expected: deploy succeeds, health OK. Next morning: `ali logs` shows `Comeback rescue planning complete — users=..., planned=...`.

---

## Self-review

**Spec coverage (design sections → tasks):**
- §1 Trigger & scheduling → Tasks 2 (policy), 11 (nightly), 13 (dispatch priority), refinements table.
- §2 Pool, prompt, validation, fallback → Tasks 3, 4, 6, 7, 9, 10.
- §2b Content craft: fact sheet → 5; angles, gold/anti examples → 6; generate-3-select-1 + linter → 8, 10; outros → 6, 8, 10; locale (`languageCode`) → 10; graphemes/RTL → 8. Taste test, shadow week UI, per-angle funnel → **P3**.
- §3 Push & deep link → Task 12; old-client fallback documented in `RescuePushBuilder`.
- §4 Client → **P2**.
- §5 Backend API & data → Tasks 1, 14. `rescue_copy_rating` table → **P3** (it only serves the admin page).
- §6 Metrics: holdout → Task 10; `notification_log_id` for opens → Tasks 1, 13; funnel dashboard → **P3**.
- §7 Testing → each task; E2E with a real model → Task 15.

**Known follow-ups for P2/P3:** client parses `vokab://review/rescue?ids=…&rid=…`, `outro_win`, `outro_soft`, calls `POST /api/v1/notifications/rescue/{rid}/complete`; P3 adds `comeback-rescue.v2.txt` with Ali's gold examples, admin page, funnel, then sets `COMEBACK_RESCUE_SEND_ENABLED=true`.
