<!-- plan-type: feature -->
# Learning Focus Implementation Plan

> **For agentic workers:** Use claude-kit:implement to execute this plan task-by-task.

**Goal:** Let users with words in several learning languages study one "focus" language at a time: smart default, one-tap switcher, nudge for waiting languages, "All languages" opt-out.

**Architecture:** A pure `LearningFocusPolicy` (domain) resolves focus from the word list plus a locally stored preference and computes scoped summaries/stats in memory. Use cases expose it as Flows. The review queue, Word Rush, widget and import default consume `ObserveLearningFocusUseCase`. The Study screen consumes one combined `ObserveStudyFocusUseCase` stream. The preference lives in a new single-row SQLDelight table, wiped together with settings.

**Tech Stack:** Kotlin Multiplatform, SQLDelight 2 (async), Koin 4, Compose Multiplatform, kotlin-test + coroutines-test.

**Spec:** `docs/specs/2026-10-02-learning-focus-design.md`
**Branch:** `feat/learning-focus`

**Status:** Done 11 / Left 1 (Task 12 device walkthrough pending: needs a signed-in device)

---

## Decisions locked while reading the code

- **≤1 language resolves to `LearningFocus.All`** (not `Single`). The behavior is identical, and it means a "Single" focus always implies 2+ languages, so the Word Manager and nudge need no extra language count.
- **Nudge dismissal is global per day.** It is stored as an epoch-day `Long` (UTC), not per language.
- **Separate `LearningFocusEntity` table, not new `SettingsEntity` columns.** `insertSettings` is `INSERT OR REPLACE` with an explicit column list, so any column missing from it is reset on every settings save. That is a pre-existing bug for `word_sync_timestamp`, tracked separately and out of scope here.
- **Deferred:**
  - "Added to Español" import snackbar
  - focus badge in the Review / Word Rush session header
  - translations of the new strings (they fall back to English)
- **`nowMillis: () -> Long` default params** on time-aware use cases. Those classes are registered in Koin with explicit lambdas, because `singleOf(::X)` would try to resolve the function type.

## File Map

| Action | Path |
|---|---|
| Create | `domain/src/commonMain/kotlin/domain/focus/model/LearningFocus.kt` |
| Create | `domain/src/commonMain/kotlin/domain/focus/LearningFocusPolicy.kt` |
| Create | `domain/src/commonMain/kotlin/domain/focus/repository/ILearningFocusRepository.kt` |
| Create | `domain/src/commonMain/kotlin/domain/focus/usecase/ObserveLearningFocusUseCase.kt` |
| Create | `domain/src/commonMain/kotlin/domain/focus/usecase/ObserveStudyFocusUseCase.kt` |
| Create | `domain/src/commonMain/kotlin/domain/focus/usecase/FocusCommandUseCases.kt` |
| Create | `data/src/commonMain/kotlin/data/focus/local/LearningFocusLocalDataSource.kt` |
| Create | `data/src/commonMain/kotlin/data/focus/repository/LearningFocusRepositoryImpl.kt` |
| Create | `data/src/commonMain/sqldelight/data/core/database/13.sqm` |
| Create | `design-system/src/commonMain/kotlin/components/LanguageBadge.kt` |
| Create | `feature/study/src/commonMain/kotlin/feature/study/ui/focus/LearningFocusComponents.kt` |
| Create | `test/src/commonMain/kotlin/fakes/FakeLearningFocusRepository.kt` |
| Modify | `data/src/commonMain/sqldelight/data/core/database/Lexicon.sq` |
| Modify | `data/src/commonMain/kotlin/data/settings/local/SettingsLocalDataSourceImpl.kt` |
| Modify | `domain/.../word/usecase/LoadReviewQueueUseCase.kt`, `GetWordRushWordsUseCase.kt`, `GetSourceLanguageUseCase.kt` |
| Modify | `domain/.../widget/usecase/GetDailyWidgetDataUseCase.kt` |
| Modify | `feature/study/.../StudyProgressViewModel.kt`, `feature/study/.../di/StudyModule.kt` |
| Modify | `feature/words/.../WordManagerViewModel.kt`, `feature/words/.../di/WordsModule.kt` |
| Modify | `composeApp/src/commonMain/kotlin/di/SettingsModule.kt`, `WordModule.kt` |
| Modify | `presentation/src/commonMain/kotlin/presentation/ui/screens/StudyScreen.kt` |
| Modify | `resources/src/commonMain/composeResources/values/strings.xml` |
| Modify | `test/src/commonMain/kotlin/fakes/FakeWordRepository.kt`, `FakeTagRepository.kt`, `FakeWidgetRefresher.kt` |
| Tests | `composeApp/src/commonTest/kotlin/domain/focus/*`, `data/focus/*`, plus updated existing tests |

Single-test command pattern: `./gradlew composeApp:testDebugUnitTest --tests "<fqcn>"`

---

### Task 1: Domain models + pure policy

**Files:**
- Create: `domain/src/commonMain/kotlin/domain/focus/model/LearningFocus.kt`
- Create: `domain/src/commonMain/kotlin/domain/focus/LearningFocusPolicy.kt`
- Test: `composeApp/src/commonTest/kotlin/domain/focus/LearningFocusPolicyTest.kt`

- [x] **Step 1: Write the failing test**

```kotlin
package domain.focus

import domain.focus.model.LanguageSummary
import domain.focus.model.LearningFocus
import domain.tag.model.Tag
import domain.word.model.Word
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LearningFocusPolicyTest {

    private val now = 1_000_000_000L
    private val today = LearningFocusPolicy.epochDay(now)

    private fun word(
        id: Int,
        language: Language,
        due: Boolean = true,
        level: Int = 0,
        lastReview: Long = 0L,
        tags: List<Long> = emptyList(),
    ) = Word(
        id = id,
        originalWord = "w$id",
        translation = "t$id",
        description = "",
        sourceLanguage = language,
        targetLanguage = Language.ENGLISH,
        level = level,
        lastReviewDate = lastReview,
        nextReviewDate = if (due) now - 1 else now + 1,
        tagIds = tags,
    )

    private fun tag(id: Long, name: String) = Tag(id = id, name = name, wordCount = 0L, createdAt = 0L, updatedAt = 0L)

    @Test
    fun `resolve when no words returns All`() {
        assertEquals(LearningFocus.All, LearningFocusPolicy.resolve(emptyList(), null, now))
    }

    @Test
    fun `resolve when one language returns All even with stored preference`() {
        val words = listOf(word(1, Language.GERMAN))
        val result = LearningFocusPolicy.resolve(words, LearningFocus.Single(Language.GERMAN), now)
        assertEquals(LearningFocus.All, result)
    }

    @Test
    fun `resolve when stored language still present returns it`() {
        val words = listOf(word(1, Language.GERMAN, lastReview = 9), word(2, Language.SPANISH))
        val result = LearningFocusPolicy.resolve(words, LearningFocus.Single(Language.SPANISH), now)
        assertEquals(LearningFocus.Single(Language.SPANISH), result)
    }

    @Test
    fun `resolve when stored All returns All`() {
        val words = listOf(word(1, Language.GERMAN), word(2, Language.SPANISH))
        assertEquals(LearningFocus.All, LearningFocusPolicy.resolve(words, LearningFocus.All, now))
    }

    @Test
    fun `resolve when stored language missing falls back to most recently reviewed`() {
        val words = listOf(word(1, Language.GERMAN, lastReview = 5), word(2, Language.SPANISH, lastReview = 9))
        val result = LearningFocusPolicy.resolve(words, LearningFocus.Single(Language.FRENCH), now)
        assertEquals(LearningFocus.Single(Language.SPANISH), result)
    }

    @Test
    fun `resolve without preference picks most recently reviewed language`() {
        val words = listOf(word(1, Language.GERMAN, lastReview = 50), word(2, Language.SPANISH, lastReview = 9))
        assertEquals(LearningFocus.Single(Language.GERMAN), LearningFocusPolicy.resolve(words, null, now))
    }

    @Test
    fun `resolve when nothing reviewed picks language with most due words`() {
        val words = listOf(
            word(1, Language.GERMAN, due = false),
            word(2, Language.SPANISH),
            word(3, Language.SPANISH),
        )
        assertEquals(LearningFocus.Single(Language.SPANISH), LearningFocusPolicy.resolve(words, null, now))
    }

    @Test
    fun `resolve when due counts tie picks language with most words`() {
        val words = listOf(
            word(1, Language.GERMAN),
            word(2, Language.GERMAN, due = false),
            word(3, Language.SPANISH),
        )
        assertEquals(LearningFocus.Single(Language.GERMAN), LearningFocusPolicy.resolve(words, null, now))
    }

    @Test
    fun `filterBy Single keeps only that source language`() {
        val words = listOf(word(1, Language.GERMAN), word(2, Language.SPANISH))
        assertEquals(listOf(1), words.filterBy(LearningFocus.Single(Language.GERMAN)).map { it.id })
        assertEquals(listOf(1, 2), words.filterBy(LearningFocus.All).map { it.id })
    }

    @Test
    fun `summaries put active language first then by due count`() {
        val words = listOf(
            word(1, Language.GERMAN, due = false),
            word(2, Language.SPANISH), word(3, Language.SPANISH),
            word(4, Language.FRENCH),
        )
        val result = LearningFocusPolicy.summaries(words, LearningFocus.Single(Language.GERMAN), now)
        assertEquals(
            listOf(
                LanguageSummary(Language.GERMAN, wordCount = 1, dueCount = 0),
                LanguageSummary(Language.SPANISH, wordCount = 2, dueCount = 2),
                LanguageSummary(Language.FRENCH, wordCount = 1, dueCount = 1),
            ),
            result,
        )
    }

    @Test
    fun `nudge returns other language with most due cards above threshold`() {
        val summaries = listOf(
            LanguageSummary(Language.GERMAN, 30, 30),
            LanguageSummary(Language.SPANISH, 20, 12),
            LanguageSummary(Language.FRENCH, 20, 15),
        )
        val result = LearningFocusPolicy.nudge(summaries, LearningFocus.Single(Language.GERMAN), null, now)
        assertEquals(Language.FRENCH, result?.language)
    }

    @Test
    fun `nudge is null below threshold`() {
        val summaries = listOf(LanguageSummary(Language.GERMAN, 5, 1), LanguageSummary(Language.SPANISH, 9, 9))
        assertNull(LearningFocusPolicy.nudge(summaries, LearningFocus.Single(Language.GERMAN), null, now))
    }

    @Test
    fun `nudge is null when dismissed today`() {
        val summaries = listOf(LanguageSummary(Language.GERMAN, 5, 1), LanguageSummary(Language.SPANISH, 20, 20))
        assertNull(LearningFocusPolicy.nudge(summaries, LearningFocus.Single(Language.GERMAN), today, now))
    }

    @Test
    fun `nudge reappears the day after dismissal`() {
        val summaries = listOf(LanguageSummary(Language.GERMAN, 5, 1), LanguageSummary(Language.SPANISH, 20, 20))
        val result = LearningFocusPolicy.nudge(summaries, LearningFocus.Single(Language.GERMAN), today - 1, now)
        assertEquals(Language.SPANISH, result?.language)
    }

    @Test
    fun `nudge is null in All focus`() {
        val summaries = listOf(LanguageSummary(Language.GERMAN, 5, 1), LanguageSummary(Language.SPANISH, 20, 20))
        assertNull(LearningFocusPolicy.nudge(summaries, LearningFocus.All, null, now))
    }

    @Test
    fun `progressStats counts levels and due words`() {
        val words = listOf(
            word(1, Language.GERMAN, level = 0),
            word(2, Language.GERMAN, level = 6, due = false),
            word(3, Language.GERMAN, level = 6),
        )
        val stats = LearningFocusPolicy.progressStats(words, now)
        assertEquals(1, stats.level0Count)
        assertEquals(2, stats.level6Count)
        assertEquals(3, stats.totalWords)
        assertEquals(2, stats.dueCards)
    }

    @Test
    fun `tagStats in Single focus drops tags without focused words`() {
        val tags = listOf(tag(1, "Food"), tag(2, "Travel"))
        val focused = listOf(word(1, Language.GERMAN, level = 2, tags = listOf(1L)))
        val stats = LearningFocusPolicy.tagStats(tags, focused, LearningFocus.Single(Language.GERMAN), now)
        assertEquals(listOf(1L), stats.tags.map { it.id })
        assertEquals(1L, stats.tags.single().wordCount)
        assertEquals(listOf(1L), stats.dueTags.map { it.id })
        assertEquals(mapOf(2 to listOf(1L)), stats.tagsByLevel.mapValues { (_, v) -> v.map { it.id } })
    }

    @Test
    fun `tagStats in All focus keeps empty tags but not in due or level maps`() {
        val tags = listOf(tag(1, "Food"), tag(2, "Travel"))
        val words = listOf(word(1, Language.GERMAN, due = false, tags = listOf(1L)))
        val stats = LearningFocusPolicy.tagStats(tags, words, LearningFocus.All, now)
        assertEquals(listOf(1L to 1L, 2L to 0L), stats.tags.map { it.id to it.wordCount })
        assertEquals(emptyList(), stats.dueTags)
        assertEquals(listOf(0), stats.tagsByLevel.keys.toList())
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `./gradlew composeApp:testDebugUnitTest --tests "domain.focus.LearningFocusPolicyTest"`
Expected: FAIL with "Unresolved reference: LearningFocusPolicy"

- [x] **Step 3: Write the models**

`domain/src/commonMain/kotlin/domain/focus/model/LearningFocus.kt`:

```kotlin
package domain.focus.model

import domain.tag.model.Tag
import domain.word.model.ProgressStats
import utils.Language

/** Which learning language (Word.sourceLanguage) the user is currently studying. */
sealed interface LearningFocus {
    data class Single(val language: Language) : LearningFocus
    data object All : LearningFocus
}

data class LanguageSummary(
    val language: Language,
    val wordCount: Int,
    val dueCount: Int,
)

data class FocusedTagStats(
    val tags: List<Tag> = emptyList(),
    val dueTags: List<Tag> = emptyList(),
    val tagsByLevel: Map<Int, List<Tag>> = emptyMap(),
)

/** Everything the Study screen needs, computed from one pass over the word list. */
data class StudyFocusOverview(
    val focus: LearningFocus,
    val languages: List<LanguageSummary>,
    val nudge: LanguageSummary?,
    val showIntro: Boolean,
    val progressStats: ProgressStats,
    val tagStats: FocusedTagStats,
)
```

- [x] **Step 4: Write the policy**

`domain/src/commonMain/kotlin/domain/focus/LearningFocusPolicy.kt`:

```kotlin
package domain.focus

import domain.focus.model.FocusedTagStats
import domain.focus.model.LanguageSummary
import domain.focus.model.LearningFocus
import domain.tag.model.Tag
import domain.word.model.ProgressStats
import domain.word.model.Word
import utils.Language

fun Word.isDue(nowMillis: Long): Boolean = nextReviewDate <= nowMillis

fun List<Word>.filterBy(focus: LearningFocus): List<Word> = when (focus) {
    LearningFocus.All -> this
    is LearningFocus.Single -> filter { it.sourceLanguage == focus.language }
}

/** Pure rules for the learning focus feature. No I/O, no clock — time is passed in. */
object LearningFocusPolicy {

    const val NUDGE_THRESHOLD = 10
    private const val MILLIS_PER_DAY = 86_400_000L

    fun epochDay(nowMillis: Long): Long = nowMillis / MILLIS_PER_DAY

    fun resolve(words: List<Word>, preference: LearningFocus?, nowMillis: Long): LearningFocus {
        val languages = words.mapTo(mutableSetOf()) { it.sourceLanguage }
        return when {
            languages.size <= 1 -> LearningFocus.All
            preference == LearningFocus.All -> LearningFocus.All
            preference is LearningFocus.Single && preference.language in languages -> preference
            else -> LearningFocus.Single(smartDefault(words, nowMillis))
        }
    }

    fun summaries(words: List<Word>, focus: LearningFocus, nowMillis: Long): List<LanguageSummary> {
        val active = (focus as? LearningFocus.Single)?.language
        return words.groupBy { it.sourceLanguage }
            .map { (language, list) ->
                LanguageSummary(language, wordCount = list.size, dueCount = list.count { it.isDue(nowMillis) })
            }
            .sortedWith(
                compareByDescending<LanguageSummary> { it.language == active }
                    .thenByDescending { it.dueCount }
                    .thenByDescending { it.wordCount }
                    .thenBy { it.language.ordinal }
            )
    }

    fun nudge(
        summaries: List<LanguageSummary>,
        focus: LearningFocus,
        dismissedOnDay: Long?,
        nowMillis: Long,
    ): LanguageSummary? {
        if (focus !is LearningFocus.Single) return null
        if (dismissedOnDay == epochDay(nowMillis)) return null
        return summaries
            .filter { it.language != focus.language && it.dueCount >= NUDGE_THRESHOLD }
            .maxByOrNull { it.dueCount }
    }

    fun progressStats(words: List<Word>, nowMillis: Long): ProgressStats {
        val byLevel = words.groupingBy { it.level }.eachCount()
        return ProgressStats(
            level0Count = byLevel[0] ?: 0,
            level1Count = byLevel[1] ?: 0,
            level2Count = byLevel[2] ?: 0,
            level3Count = byLevel[3] ?: 0,
            level4Count = byLevel[4] ?: 0,
            level5Count = byLevel[5] ?: 0,
            level6Count = byLevel[6] ?: 0,
            totalWords = words.size,
            dueCards = words.count { it.isDue(nowMillis) },
        )
    }

    /**
     * Mirrors the SQL tag queries (getAllTagsWithWordCount / getDueTagWordCounts /
     * getTagWordCountsByLevel) over an already-focused word list. Keeps [tags] input order.
     */
    fun tagStats(
        tags: List<Tag>,
        focusedWords: List<Word>,
        focus: LearningFocus,
        nowMillis: Long,
    ): FocusedTagStats {
        val wordsByTag: Map<Long, List<Word>> = focusedWords
            .flatMap { word -> word.tagIds.map { tagId -> tagId to word } }
            .groupBy({ it.first }, { it.second })

        fun countedTags(predicate: (Word) -> Boolean): List<Tag> = tags.mapNotNull { tag ->
            val count = wordsByTag[tag.id].orEmpty().count(predicate)
            if (count > 0) tag.copy(wordCount = count.toLong()) else null
        }

        val allTags = tags
            .map { tag -> tag.copy(wordCount = wordsByTag[tag.id].orEmpty().size.toLong()) }
            .filter { focus == LearningFocus.All || it.wordCount > 0 }

        val byLevel = focusedWords.map { it.level }.distinct().sorted()
            .associateWith { level -> countedTags { it.level == level } }
            .filterValues { it.isNotEmpty() }

        return FocusedTagStats(
            tags = allTags,
            dueTags = countedTags { it.isDue(nowMillis) },
            tagsByLevel = byLevel,
        )
    }

    private fun smartDefault(words: List<Word>, nowMillis: Long): Language =
        words.groupBy { it.sourceLanguage }
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<Language, List<Word>>> { entry -> entry.value.maxOf { it.lastReviewDate } }
                    .thenByDescending { entry -> entry.value.count { it.isDue(nowMillis) } }
                    .thenByDescending { entry -> entry.value.size }
                    .thenBy { entry -> entry.key.ordinal }
            )
            .first()
            .key
}
```

- [x] **Step 5: Run test to verify it passes**

Run: `./gradlew composeApp:testDebugUnitTest --tests "domain.focus.LearningFocusPolicyTest"`
Expected: PASS (18 tests)

- [x] **Step 6: Commit**

```bash
git add domain/src/commonMain/kotlin/domain/focus composeApp/src/commonTest/kotlin/domain/focus
git commit -m "feat(focus): add learning focus models and pure policy"
```

---

### Task 2: Repository contract + shared fakes

**Files:**
- Create: `domain/src/commonMain/kotlin/domain/focus/repository/ILearningFocusRepository.kt`
- Create: `test/src/commonMain/kotlin/fakes/FakeLearningFocusRepository.kt`
- Modify: `test/src/commonMain/kotlin/fakes/FakeWordRepository.kt`
- Modify: `test/src/commonMain/kotlin/fakes/FakeTagRepository.kt`

No test of its own (contract and fakes only). It's exercised from Task 3 onwards.

- [x] **Step 1: Write the interface**

```kotlin
package domain.focus.repository

import core.common.Try
import domain.focus.model.LearningFocus
import kotlinx.coroutines.flow.Flow

/** Local-only preference storage for the learning focus feature. */
interface ILearningFocusRepository {
    /** null = the user never picked a focus. */
    fun observePreference(): Flow<LearningFocus?>
    suspend fun setPreference(focus: LearningFocus): Try<Unit>

    /** Epoch day (UTC) the nudge was last dismissed, or null. */
    fun observeNudgeDismissedDay(): Flow<Long?>
    suspend fun dismissNudge(epochDay: Long): Try<Unit>

    fun observeIntroAcknowledged(): Flow<Boolean>
    suspend fun acknowledgeIntro(): Try<Unit>
}
```

- [x] **Step 2: Write the fake**

```kotlin
package fakes

import core.common.Try
import domain.focus.model.LearningFocus
import domain.focus.repository.ILearningFocusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeLearningFocusRepository(
    initialPreference: LearningFocus? = null,
) : ILearningFocusRepository {
    val preference = MutableStateFlow(initialPreference)
    val nudgeDismissedDay = MutableStateFlow<Long?>(null)
    val introAcknowledged = MutableStateFlow(false)

    override fun observePreference(): Flow<LearningFocus?> = preference
    override suspend fun setPreference(focus: LearningFocus): Try<Unit> {
        preference.value = focus
        return Try.success(Unit)
    }

    override fun observeNudgeDismissedDay(): Flow<Long?> = nudgeDismissedDay
    override suspend fun dismissNudge(epochDay: Long): Try<Unit> {
        nudgeDismissedDay.value = epochDay
        return Try.success(Unit)
    }

    override fun observeIntroAcknowledged(): Flow<Boolean> = introAcknowledged
    override suspend fun acknowledgeIntro(): Try<Unit> {
        introAcknowledged.value = true
        return Try.success(Unit)
    }
}
```

- [x] **Step 3: Make the shared fakes configurable (defaults unchanged)**

In `FakeWordRepository.kt` add a property and change `getDueCards`:

```kotlin
    var dueWords: List<Word> = emptyList()
    // ...
    override fun getDueCards(): Flow<List<Word>> = flowOf(dueWords)
```

In `FakeTagRepository.kt` add a property and change `getTags`:

```kotlin
    var tags: List<Tag> = emptyList()
    // ...
    override fun getTags(): Flow<List<Tag>> = flowOf(tags)
```

(`flowOf(...)` is evaluated per call, so tests can set the property before building the use case.)

- [x] **Step 4: Compile**

Run: `./gradlew composeApp:compileTestKotlinAndroid` (or `composeApp:testDebugUnitTest --tests "domain.focus.*"`)
Expected: BUILD SUCCESSFUL

- [x] **Step 5: Commit**

```bash
git add domain/src/commonMain/kotlin/domain/focus/repository test/src/commonMain/kotlin/fakes
git commit -m "feat(focus): add learning focus repository contract and fakes"
```

---

### Task 3: Focus use cases

**Files:**
- Create: `domain/src/commonMain/kotlin/domain/focus/usecase/ObserveLearningFocusUseCase.kt`
- Create: `domain/src/commonMain/kotlin/domain/focus/usecase/ObserveStudyFocusUseCase.kt`
- Create: `domain/src/commonMain/kotlin/domain/focus/usecase/FocusCommandUseCases.kt`
- Test: `composeApp/src/commonTest/kotlin/domain/focus/FocusUseCasesTest.kt`

- [x] **Step 1: Write the failing test**

```kotlin
package domain.focus

import core.common.getOrThrow
import domain.focus.model.LearningFocus
import domain.focus.usecase.AcknowledgeFocusIntroUseCase
import domain.focus.usecase.DismissFocusNudgeUseCase
import domain.focus.usecase.ObserveLearningFocusUseCase
import domain.focus.usecase.ObserveStudyFocusUseCase
import domain.focus.usecase.SetLearningFocusUseCase
import domain.tag.model.Tag
import domain.word.model.Word
import fakes.FakeLearningFocusRepository
import fakes.FakeTagRepository
import fakes.FakeWordRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FocusUseCasesTest {

    private val now = 1_000_000_000L

    private fun word(id: Int, language: Language, tags: List<Long> = emptyList()) = Word(
        id = id, originalWord = "w$id", translation = "t$id", description = "",
        sourceLanguage = language, targetLanguage = Language.ENGLISH,
        nextReviewDate = now - 1, tagIds = tags,
    )

    private val words = buildList {
        add(word(1, Language.GERMAN, tags = listOf(7L)))
        repeat(10) { add(word(100 + it, Language.SPANISH)) }
    }

    private val wordRepo = FakeWordRepository().apply { storedWords = words.toMutableList() }
    private val tagRepo = FakeTagRepository().apply {
        tags = listOf(Tag(id = 7L, name = "Food", wordCount = 0L, createdAt = 0L, updatedAt = 0L))
    }

    @Test
    fun `observe learning focus resolves stored preference`() = runTest {
        val focusRepo = FakeLearningFocusRepository(LearningFocus.Single(Language.GERMAN))
        val useCase = ObserveLearningFocusUseCase(wordRepo, focusRepo) { now }
        assertEquals(LearningFocus.Single(Language.GERMAN), useCase().first())
    }

    @Test
    fun `study focus overview scopes stats tags and exposes nudge and intro`() = runTest {
        val focusRepo = FakeLearningFocusRepository(LearningFocus.Single(Language.GERMAN))
        val overview = ObserveStudyFocusUseCase(wordRepo, tagRepo, focusRepo) { now }().first()

        assertEquals(LearningFocus.Single(Language.GERMAN), overview.focus)
        assertEquals(1, overview.progressStats.totalWords)
        assertEquals(listOf(7L), overview.tagStats.tags.map { it.id })
        assertEquals(Language.SPANISH, overview.nudge?.language)
        assertTrue(overview.showIntro)
        assertEquals(2, overview.languages.size)
    }

    @Test
    fun `set focus then overview re-emits with new scope`() = runTest {
        val focusRepo = FakeLearningFocusRepository(LearningFocus.Single(Language.GERMAN))
        SetLearningFocusUseCase(focusRepo)(LearningFocus.Single(Language.SPANISH)).getOrThrow()
        val overview = ObserveStudyFocusUseCase(wordRepo, tagRepo, focusRepo) { now }().first()
        assertEquals(10, overview.progressStats.totalWords)
        assertNull(overview.nudge)
    }

    @Test
    fun `dismiss nudge stores today and hides nudge`() = runTest {
        val focusRepo = FakeLearningFocusRepository(LearningFocus.Single(Language.GERMAN))
        DismissFocusNudgeUseCase(focusRepo) { now }().getOrThrow()
        assertEquals(LearningFocusPolicy.epochDay(now), focusRepo.nudgeDismissedDay.value)
        val overview = ObserveStudyFocusUseCase(wordRepo, tagRepo, focusRepo) { now }().first()
        assertNull(overview.nudge)
    }

    @Test
    fun `acknowledge intro hides intro`() = runTest {
        val focusRepo = FakeLearningFocusRepository()
        AcknowledgeFocusIntroUseCase(focusRepo)().getOrThrow()
        val overview = ObserveStudyFocusUseCase(wordRepo, tagRepo, focusRepo) { now }().first()
        assertFalse(overview.showIntro)
    }

    @Test
    fun `single language never shows intro`() = runTest {
        wordRepo.storedWords = mutableListOf(word(1, Language.GERMAN))
        val overview = ObserveStudyFocusUseCase(wordRepo, tagRepo, FakeLearningFocusRepository()) { now }().first()
        assertEquals(LearningFocus.All, overview.focus)
        assertFalse(overview.showIntro)
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `./gradlew composeApp:testDebugUnitTest --tests "domain.focus.FocusUseCasesTest"`
Expected: FAIL with "Unresolved reference: ObserveLearningFocusUseCase"

- [x] **Step 3: Implement**

`ObserveLearningFocusUseCase.kt`:

```kotlin
package domain.focus.usecase

import core.common.NoParamFlowUseCase
import domain.focus.LearningFocusPolicy
import domain.focus.model.LearningFocus
import domain.focus.repository.ILearningFocusRepository
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Clock

/** Resolved focus (preference validated against the words that actually exist). */
class ObserveLearningFocusUseCase(
    private val wordRepository: IWordRepository,
    private val focusRepository: ILearningFocusRepository,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : NoParamFlowUseCase<LearningFocus> {

    operator fun invoke(): Flow<LearningFocus> =
        combine(wordRepository.getAllWords(), focusRepository.observePreference()) { words, preference ->
            LearningFocusPolicy.resolve(words, preference, nowMillis())
        }.distinctUntilChanged()

    override operator fun invoke(params: Unit) = invoke()
}
```

`ObserveStudyFocusUseCase.kt`:

```kotlin
package domain.focus.usecase

import core.common.NoParamFlowUseCase
import domain.focus.LearningFocusPolicy
import domain.focus.filterBy
import domain.focus.model.StudyFocusOverview
import domain.focus.repository.ILearningFocusRepository
import domain.tag.repository.ITagRepository
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.time.Clock

/** One stream for the Study screen: focus, language list, nudge, intro, scoped stats + tags. */
class ObserveStudyFocusUseCase(
    private val wordRepository: IWordRepository,
    private val tagRepository: ITagRepository,
    private val focusRepository: ILearningFocusRepository,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : NoParamFlowUseCase<StudyFocusOverview> {

    operator fun invoke(): Flow<StudyFocusOverview> = combine(
        wordRepository.getAllWords(),
        tagRepository.getTags(),
        focusRepository.observePreference(),
        focusRepository.observeNudgeDismissedDay(),
        focusRepository.observeIntroAcknowledged(),
    ) { words, tags, preference, dismissedDay, introAcknowledged ->
        val now = nowMillis()
        val focus = LearningFocusPolicy.resolve(words, preference, now)
        val languages = LearningFocusPolicy.summaries(words, focus, now)
        val focusedWords = words.filterBy(focus)
        StudyFocusOverview(
            focus = focus,
            languages = languages,
            nudge = LearningFocusPolicy.nudge(languages, focus, dismissedDay, now),
            showIntro = languages.size >= 2 && !introAcknowledged,
            progressStats = LearningFocusPolicy.progressStats(focusedWords, now),
            tagStats = LearningFocusPolicy.tagStats(tags, focusedWords, focus, now),
        )
    }.distinctUntilChanged()

    override operator fun invoke(params: Unit) = invoke()
}
```

`FocusCommandUseCases.kt`:

```kotlin
package domain.focus.usecase

import core.common.NoParamUseCase
import core.common.Try
import core.common.UseCase
import domain.focus.LearningFocusPolicy
import domain.focus.model.LearningFocus
import domain.focus.repository.ILearningFocusRepository
import kotlin.time.Clock

class SetLearningFocusUseCase(
    private val focusRepository: ILearningFocusRepository,
) : UseCase<LearningFocus, Unit> {
    override suspend operator fun invoke(params: LearningFocus): Try<Unit> =
        focusRepository.setPreference(params)
}

class DismissFocusNudgeUseCase(
    private val focusRepository: ILearningFocusRepository,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : NoParamUseCase<Unit> {
    suspend operator fun invoke(): Try<Unit> =
        focusRepository.dismissNudge(LearningFocusPolicy.epochDay(nowMillis()))

    override suspend operator fun invoke(params: Unit) = invoke()
}

class AcknowledgeFocusIntroUseCase(
    private val focusRepository: ILearningFocusRepository,
) : NoParamUseCase<Unit> {
    suspend operator fun invoke(): Try<Unit> = focusRepository.acknowledgeIntro()

    override suspend operator fun invoke(params: Unit) = invoke()
}
```

If `kotlin.time.Clock` doesn't resolve in `:domain`, use the fully qualified `kotlin.time.Clock.System` as `Word.kt` does.

- [x] **Step 4: Run test to verify it passes**

Run: `./gradlew composeApp:testDebugUnitTest --tests "domain.focus.*"`
Expected: PASS

- [x] **Step 5: Commit**

```bash
git add domain/src/commonMain/kotlin/domain/focus/usecase composeApp/src/commonTest/kotlin/domain/focus
git commit -m "feat(focus): add focus observe and command use cases"
```

---

### Task 4: Scope the review queue

**Files:**
- Modify: `domain/src/commonMain/kotlin/domain/word/usecase/LoadReviewQueueUseCase.kt`
- Modify: `composeApp/src/commonTest/kotlin/domain/word/usecase/LoadReviewQueueUseCaseTest.kt`
- Modify: `composeApp/src/commonTest/kotlin/presentation/feature/study/ReviewViewModelTest.kt`

- [x] **Step 1: Verify `getAllWords()` carries `tagIds`**

Read `data/src/commonMain/kotlin/data/word/local/WordLocalDataSource.kt:65-95` and confirm that the mapping attaches tag ids (the Study focus tag stats depend on it). If it doesn't, stop and flag it. Don't work around it.

- [x] **Step 2: Write the failing tests**

In `LoadReviewQueueUseCaseTest`:
- Give `testWord` a `language: Language = Language.ENGLISH` param, used as `sourceLanguage`.
- Give `ConfigurableWordRepository` an `allWords: List<Word> = emptyList()` constructor param, and make `getAllWords()` return `flowOf(allWords)`.
- Give `buildUseCase` a `focus: LearningFocus? = null` param that appends
  `ObserveLearningFocusUseCase(repo, FakeLearningFocusRepository(focus))` as the 5th constructor argument.

Add:

```kotlin
    @Test
    fun `DueCards with Single focus returns only focused language before applying daily goal`() = runTest {
        val german = (1..3).map { testWord(it, language = Language.GERMAN) }
        val spanish = (10..12).map { testWord(it, language = Language.SPANISH) }
        val mixedDue = listOf(spanish[0], german[0], spanish[1], german[1], german[2], spanish[2])
        val repo = ConfigurableWordRepository(dueCards = mixedDue, allWords = german + spanish)

        val result = buildUseCase(repo, dailyGoal = 2, focus = LearningFocus.Single(Language.GERMAN))(ReviewSource.DueCards)

        assertEquals(listOf(1, 2), result.getOrNull()?.map { it.id })
    }

    @Test
    fun `DueCards with All focus keeps every language`() = runTest {
        val words = listOf(testWord(1, language = Language.GERMAN), testWord(2, language = Language.SPANISH))
        val repo = ConfigurableWordRepository(dueCards = words, allWords = words)

        val result = buildUseCase(repo, focus = LearningFocus.All)(ReviewSource.DueCards)

        assertEquals(listOf(1, 2), result.getOrNull()?.map { it.id })
    }
```

In `ReviewViewModelTest.createViewModel()` add to the `LoadReviewQueueUseCase(...)` call:

```kotlin
                observeLearningFocus = ObserveLearningFocusUseCase(wordRepo, FakeLearningFocusRepository()),
```

- [x] **Step 3: Run to verify failure**

Run: `./gradlew composeApp:testDebugUnitTest --tests "domain.word.usecase.LoadReviewQueueUseCaseTest"`
Expected: FAIL: compile error, too many arguments for `LoadReviewQueueUseCase`

- [x] **Step 4: Implement**

```kotlin
class LoadReviewQueueUseCase(
    private val getDueWords: GetDueWordsUseCase,
    private val getWordsByStage: GetWordsByStageUseCase,
    private val getDueWordsByTag: GetDueWordsByTagUseCase,
    private val getDailyGoalWords: GetDailyGoalWordsUseCase,
    private val observeLearningFocus: ObserveLearningFocusUseCase,
) : UseCase<ReviewSource, List<Word>> {

    override suspend fun invoke(params: ReviewSource): Try<List<Word>> = Try<List<Word>> {
        val limit = getDailyGoalWords().getOrDefault(Int.MAX_VALUE)
        val focus = observeLearningFocus().first()
        when (params) {
            is ReviewSource.DueCards ->
                getDueWords().first().filterBy(focus).take(limit)

            is ReviewSource.ByStage ->
                getWordsByStage(params.stage).first().filterBy(focus)

            is ReviewSource.ByTag ->
                getDueWordsByTag(params.tagId).first().filterBy(focus).take(limit)

            is ReviewSource.ByStageAndTag ->
                getWordsByStage(params.stage).first()
                    .filter { params.tagId in it.tagIds }
                    .filterBy(focus)
        }
    }
}
```

Add imports `domain.focus.filterBy` and `domain.focus.usecase.ObserveLearningFocusUseCase`, and add one line to the KDoc: "Every source is scoped to the user's learning focus; the daily-goal cap applies after scoping."

- [x] **Step 5: Run to verify pass**

Run: `./gradlew composeApp:testDebugUnitTest --tests "domain.word.usecase.LoadReviewQueueUseCaseTest" --tests "presentation.feature.study.ReviewViewModelTest"`
Expected: PASS

- [x] **Step 6: Commit**

```bash
git add domain/src/commonMain/kotlin/domain/word/usecase/LoadReviewQueueUseCase.kt composeApp/src/commonTest
git commit -m "feat(focus): scope review queue to learning focus"
```

---

### Task 5: Scope Word Rush, widget, and the import default language

**Files:**
- Modify: `domain/src/commonMain/kotlin/domain/word/usecase/GetWordRushWordsUseCase.kt`
- Modify: `domain/src/commonMain/kotlin/domain/widget/usecase/GetDailyWidgetDataUseCase.kt`
- Modify: `domain/src/commonMain/kotlin/domain/word/usecase/GetSourceLanguageUseCase.kt`
- Modify: `test/src/commonMain/kotlin/fakes/FakeWidgetRefresher.kt` (`fakeGetDailyWidgetDataUseCase`)
- Modify tests: `composeApp/src/commonTest/kotlin/feature/study/wordrush/GetWordRushWordsUseCaseTest.kt`, `WordRushViewModelTest.kt`, `domain/widget/usecase/GetDailyWidgetDataUseCaseTest.kt`, `domain/word/usecase/GetSourceLanguageUseCaseTest.kt`

- [x] **Step 1: Write the failing tests**

`GetWordRushWordsUseCaseTest`: add a helper and switch every `GetWordRushWordsUseCase(x)` to `useCase(x)`:

```kotlin
    private fun useCase(repo: IWordRepository, focus: LearningFocus? = null) =
        GetWordRushWordsUseCase(repo, ObserveLearningFocusUseCase(repo, FakeLearningFocusRepository(focus)))

    @Test
    fun `invoke with Single focus only returns focused language`() = runTest {
        val words = (1..6).map { createWord(it).copy(sourceLanguage = Language.GERMAN) } +
            (7..12).map { createWord(it).copy(sourceLanguage = Language.SPANISH) }
        val repo = FakeWordRepository().apply { storedWords = words.toMutableList() }

        val result = useCase(repo, LearningFocus.Single(Language.SPANISH))(4).getOrThrow()

        assertTrue(result.all { it.sourceLanguage == Language.SPANISH })
    }

    @Test
    fun `invoke fails when focused language has fewer than minimum words`() = runTest {
        val words = (1..2).map { createWord(it).copy(sourceLanguage = Language.GERMAN) } +
            (3..12).map { createWord(it).copy(sourceLanguage = Language.SPANISH) }
        val repo = FakeWordRepository().apply { storedWords = words.toMutableList() }

        assertTrue(useCase(repo, LearningFocus.Single(Language.GERMAN))(4).isFailure)
    }
```

(Adapt `createWord(id)` to the test's existing word factory name and signature.)

`WordRushViewModelTest` line ~84: `GetWordRushWordsUseCase(repo, ObserveLearningFocusUseCase(repo, FakeLearningFocusRepository()))`.

`fakeGetDailyWidgetDataUseCase` (in `FakeWidgetRefresher.kt`): pass `ObserveLearningFocusUseCase(wordRepository, FakeLearningFocusRepository())` as the new 4th argument. The direct construction in `GetDailyWidgetDataUseCaseTest:69` gets the same 4th argument. Add:

```kotlin
    @Test
    fun `widget with Single focus picks focused word and counts focused due cards`() = runTest {
        val now = 1_000_000_000L
        val german = Word(id = 1, originalWord = "Hund", translation = "dog", description = "",
            sourceLanguage = Language.GERMAN, targetLanguage = Language.ENGLISH, nextReviewDate = now - 1)
        val spanish = (2..4).map { german.copy(id = it, originalWord = "perro$it", sourceLanguage = Language.SPANISH) }
        val repo = FakeWordRepository().apply { storedWords = (listOf(german) + spanish).toMutableList() }
        val useCase = GetDailyWidgetDataUseCase(
            repo, noOpStreakRepo(), FakeWidgetRefresher(),
            ObserveLearningFocusUseCase(repo, FakeLearningFocusRepository(LearningFocus.Single(Language.GERMAN))),
        ) { now }

        val data = useCase(Unit).getOrThrow()

        assertEquals("Hund", data.word)
        assertEquals(1, data.dueCardCount)
    }
```

`GetSourceLanguageUseCaseTest`: `createUseCase()` passes `ObserveLearningFocusUseCase(<its fake repo>, FakeLearningFocusRepository())`. Add:

```kotlin
    @Test
    fun `returns focused language when focus is Single`() = runTest {
        val word = Word(id = 1, originalWord = "a", translation = "b", description = "",
            sourceLanguage = Language.GERMAN, targetLanguage = Language.ENGLISH, nextReviewDate = 0L)
        val repo = FakeWordRepository().apply {
            storedWords = mutableListOf(word, word.copy(id = 2, sourceLanguage = Language.SPANISH))
        }
        val useCase = GetSourceLanguageUseCase(
            repo, ObserveLearningFocusUseCase(repo, FakeLearningFocusRepository(LearningFocus.Single(Language.SPANISH))),
        )
        assertEquals(Language.SPANISH, useCase().getOrThrow())
    }
```

- [x] **Step 2: Run to verify failure**

Run: `./gradlew composeApp:testDebugUnitTest --tests "*GetWordRushWordsUseCaseTest" --tests "*GetDailyWidgetDataUseCaseTest" --tests "*GetSourceLanguageUseCaseTest"`
Expected: FAIL: compile errors on the new constructor arguments

- [x] **Step 3: Implement**

`GetWordRushWordsUseCase`:

```kotlin
class GetWordRushWordsUseCase(
    private val wordRepository: IWordRepository,
    private val observeLearningFocus: ObserveLearningFocusUseCase,
) {

    suspend operator fun invoke(count: Int): Try<List<Word>> = Try {
        val focus = observeLearningFocus().first()
        val allWords = wordRepository.getAllWordsAsync().getOrThrow().filterBy(focus)
        require(allWords.size >= MINIMUM_WORDS) {
            "Need at least $MINIMUM_WORDS words to play Word Rush"
        }
        selectMixedWords(allWords, count)
    }
    // selectMixedWords + companion unchanged
```

`GetDailyWidgetDataUseCase`: add the constructor params and replace the body of `invoke`:

```kotlin
class GetDailyWidgetDataUseCase(
    private val wordRepository: IWordRepository,
    private val streakRepository: IStreakRepository,
    private val widgetRefresher: IWidgetRefresher,
    private val observeLearningFocus: ObserveLearningFocusUseCase,
    private val nowMillis: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) : NoParamUseCase<DailyWidgetData> {

    override suspend operator fun invoke(params: Unit): Try<DailyWidgetData> {
        return wordRepository.getAllWordsAsync().flatMap { allWords ->
            val focus = observeLearningFocus().first()
            val words = allWords.filterBy(focus)
            if (words.isEmpty()) {
                return@flatMap Try.failure(NoWordsAvailableException())
            }

            // Pick a word deterministically based on the day so it stays consistent
            val daysSinceEpoch = currentDaysSinceEpoch()
            val random = Random(daysSinceEpoch.toLong())
            val selectedWord = words[random.nextInt(words.size)]

            val streakCount = streakRepository.getStreak()
                .map { it.currentStreak }
                .getOrDefault(0)

            val dueCount = when (focus) {
                LearningFocus.All -> wordRepository.getDueCount().getOrDefault(0)
                is LearningFocus.Single -> words.count { it.isDue(nowMillis()) }
            }

            val data = DailyWidgetData(
                wordId = selectedWord.id,
                word = selectedWord.originalWord,
                translation = selectedWord.translation,
                streakCount = streakCount,
                dueCardCount = dueCount
            )

            widgetRefresher.push(data)

            Try.success(data)
        }
    }
```

`currentDaysSinceEpoch()` should use `nowMillis()` instead of calling `Clock` directly. If `flatMap` turns out not to be inline (a suspend call inside fails to compile), resolve `focus` before `getAllWordsAsync()`.

`GetSourceLanguageUseCase`:

```kotlin
class GetSourceLanguageUseCase(
    private val wordRepository: IWordRepository,
    private val observeLearningFocus: ObserveLearningFocusUseCase,
) : NoParamUseCase<Language> {

    override suspend operator fun invoke(params: Unit) = invoke()

    suspend operator fun invoke(): Try<Language> = Try {
        when (val focus = observeLearningFocus().first()) {
            is LearningFocus.Single -> focus.language
            LearningFocus.All -> wordRepository.getMostCommonSourceLanguage().getOrNull()
                ?.let(Language::fromCode)
                ?: Language.ENGLISH
        }
    }
}
```

Update its KDoc to: "Default learning language for new words: the focused language, else the most common one, else ENGLISH."

- [x] **Step 4: Run to verify pass**

Run: `./gradlew composeApp:testDebugUnitTest --tests "*WordRush*" --tests "*GetDailyWidgetDataUseCaseTest" --tests "*GetSourceLanguageUseCaseTest" --tests "*DeleteWord*" --tests "*VocabularyViewModelTest" --tests "*WordManagerViewModelTest"`
Expected: PASS

- [x] **Step 5: Commit**

```bash
git add domain/src/commonMain test/src/commonMain composeApp/src/commonTest
git commit -m "feat(focus): scope word rush, widget and import default to focus"
```

---

### Task 6: Data layer — table, migration, data source, repository

**Files:**
- Modify: `data/src/commonMain/sqldelight/data/core/database/Lexicon.sq`
- Create: `data/src/commonMain/sqldelight/data/core/database/13.sqm`
- Create: `data/src/commonMain/kotlin/data/focus/local/LearningFocusLocalDataSource.kt`
- Create: `data/src/commonMain/kotlin/data/focus/repository/LearningFocusRepositoryImpl.kt`
- Modify: `data/src/commonMain/kotlin/data/settings/local/SettingsLocalDataSourceImpl.kt`
- Test: `composeApp/src/commonTest/kotlin/data/focus/LearningFocusRepositoryImplTest.kt`

- [x] **Step 1: Write the failing test**

```kotlin
package data.focus

import core.common.getOrThrow
import data.focus.local.ILearningFocusLocalDataSource
import data.focus.local.LearningFocusRecord
import data.focus.repository.LearningFocusRepositoryImpl
import domain.focus.model.LearningFocus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LearningFocusRepositoryImplTest {

    private class FakeLocal : ILearningFocusLocalDataSource {
        val record = MutableStateFlow<LearningFocusRecord?>(null)
        private fun current() = record.value ?: LearningFocusRecord(null, null, false)
        override fun observe(): Flow<LearningFocusRecord?> = record
        override suspend fun setFocusCode(code: String) { record.value = current().copy(focusCode = code) }
        override suspend fun setNudgeDismissedDay(day: Long) { record.value = current().copy(nudgeDismissedDay = day) }
        override suspend fun setIntroAcknowledged() { record.value = current().copy(introAcknowledged = true) }
    }

    private val local = FakeLocal()
    private val repo = LearningFocusRepositoryImpl(local)

    @Test
    fun `preference is null when nothing stored`() = runTest {
        assertNull(repo.observePreference().first())
    }

    @Test
    fun `Single preference round trips through language code`() = runTest {
        repo.setPreference(LearningFocus.Single(Language.SPANISH)).getOrThrow()
        assertEquals("es", local.record.value?.focusCode)
        assertEquals(LearningFocus.Single(Language.SPANISH), repo.observePreference().first())
    }

    @Test
    fun `All preference round trips`() = runTest {
        repo.setPreference(LearningFocus.All).getOrThrow()
        assertEquals(LearningFocus.All, repo.observePreference().first())
    }

    @Test
    fun `nudge day and intro flag persist`() = runTest {
        repo.dismissNudge(20_000L).getOrThrow()
        repo.acknowledgeIntro().getOrThrow()
        assertEquals(20_000L, repo.observeNudgeDismissedDay().first())
        assertTrue(repo.observeIntroAcknowledged().first())
    }
}
```

- [x] **Step 2: Run to verify failure**

Run: `./gradlew composeApp:testDebugUnitTest --tests "data.focus.LearningFocusRepositoryImplTest"`
Expected: FAIL: unresolved `data.focus`

- [x] **Step 3: Schema + migration**

Append to `Lexicon.sq`:

```sql
-- LearningFocusEntity: single-row, client-only learning focus preference.
-- Separate table on purpose: insertSettings is INSERT OR REPLACE with an explicit
-- column list, so extra SettingsEntity columns would be reset on every settings save.
CREATE TABLE LearningFocusEntity (
    id INTEGER NOT NULL PRIMARY KEY DEFAULT 1,
    focusCode TEXT,
    nudgeDismissedDay INTEGER,
    introAcknowledged INTEGER NOT NULL DEFAULT 0
);

getLearningFocus:
SELECT * FROM LearningFocusEntity WHERE id = 1;

ensureLearningFocusRow:
INSERT OR IGNORE INTO LearningFocusEntity(id) VALUES (1);

setLearningFocusCode:
UPDATE LearningFocusEntity SET focusCode = ? WHERE id = 1;

setLearningFocusNudgeDismissedDay:
UPDATE LearningFocusEntity SET nudgeDismissedDay = ? WHERE id = 1;

setLearningFocusIntroAcknowledged:
UPDATE LearningFocusEntity SET introAcknowledged = 1 WHERE id = 1;

clearLearningFocus:
DELETE FROM LearningFocusEntity;
```

Create `13.sqm`:

```sql
-- Migration from version 13 to version 14
-- Learning focus: one active learning language at a time (client-only preference).
CREATE TABLE IF NOT EXISTS LearningFocusEntity (
    id INTEGER NOT NULL PRIMARY KEY DEFAULT 1,
    focusCode TEXT,
    nudgeDismissedDay INTEGER,
    introAcknowledged INTEGER NOT NULL DEFAULT 0
);
```

- [x] **Step 4: Local data source**

```kotlin
package data.focus.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import data.core.database.LexiconQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class LearningFocusRecord(
    val focusCode: String?,
    val nudgeDismissedDay: Long?,
    val introAcknowledged: Boolean,
)

interface ILearningFocusLocalDataSource {
    fun observe(): Flow<LearningFocusRecord?>
    suspend fun setFocusCode(code: String)
    suspend fun setNudgeDismissedDay(day: Long)
    suspend fun setIntroAcknowledged()
}

class LearningFocusLocalDataSourceImpl(
    private val queries: LexiconQueries,
) : ILearningFocusLocalDataSource {

    override fun observe(): Flow<LearningFocusRecord?> =
        queries.getLearningFocus().asFlow().mapToOneOrNull(Dispatchers.Default)
            .map { entity ->
                entity?.let {
                    LearningFocusRecord(
                        focusCode = it.focusCode,
                        nudgeDismissedDay = it.nudgeDismissedDay,
                        introAcknowledged = it.introAcknowledged != 0L,
                    )
                }
            }

    override suspend fun setFocusCode(code: String) {
        queries.ensureLearningFocusRow()
        queries.setLearningFocusCode(code)
    }

    override suspend fun setNudgeDismissedDay(day: Long) {
        queries.ensureLearningFocusRow()
        queries.setLearningFocusNudgeDismissedDay(day)
    }

    override suspend fun setIntroAcknowledged() {
        queries.ensureLearningFocusRow()
        queries.setLearningFocusIntroAcknowledged()
    }
}
```

- [x] **Step 5: Repository**

```kotlin
package data.focus.repository

import core.common.Try
import data.focus.local.ILearningFocusLocalDataSource
import domain.focus.model.LearningFocus
import domain.focus.repository.ILearningFocusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import utils.Language

class LearningFocusRepositoryImpl(
    private val localDataSource: ILearningFocusLocalDataSource,
) : ILearningFocusRepository {

    override fun observePreference(): Flow<LearningFocus?> =
        localDataSource.observe().map { it?.focusCode?.toLearningFocus() }.distinctUntilChanged()

    override suspend fun setPreference(focus: LearningFocus): Try<Unit> = Try {
        localDataSource.setFocusCode(focus.toCode())
    }

    override fun observeNudgeDismissedDay(): Flow<Long?> =
        localDataSource.observe().map { it?.nudgeDismissedDay }.distinctUntilChanged()

    override suspend fun dismissNudge(epochDay: Long): Try<Unit> = Try {
        localDataSource.setNudgeDismissedDay(epochDay)
    }

    override fun observeIntroAcknowledged(): Flow<Boolean> =
        localDataSource.observe().map { it?.introAcknowledged ?: false }.distinctUntilChanged()

    override suspend fun acknowledgeIntro(): Try<Unit> = Try {
        localDataSource.setIntroAcknowledged()
    }

    private companion object {
        const val ALL_CODE = "all"
    }

    private fun LearningFocus.toCode(): String = when (this) {
        LearningFocus.All -> ALL_CODE
        is LearningFocus.Single -> language.code
    }

    private fun String.toLearningFocus(): LearningFocus =
        if (this == ALL_CODE) LearningFocus.All else LearningFocus.Single(Language.fromCode(this))
}
```

- [x] **Step 6: Wipe with settings**

In `SettingsLocalDataSourceImpl.clearSettings()`:

```kotlin
    override suspend fun clearSettings() {
        queries.clearSettings()
        queries.clearLearningFocus()
    }
```

This covers logout, delete account and clear data, which all go through `ISettingsRepository.clearSettings()`.

- [x] **Step 7: Run to verify pass + schema generation**

Run: `./gradlew data:generateCommonMainLexiconDatabaseInterface composeApp:testDebugUnitTest --tests "data.focus.*" --tests "*SettingsRepositoryImplTest"`
Expected: BUILD SUCCESSFUL, tests PASS

- [x] **Step 8: Commit**

```bash
git add data/src/commonMain composeApp/src/commonTest/kotlin/data/focus
git commit -m "feat(focus): persist learning focus in local table, clear with settings"
```

---

### Task 7: DI wiring

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/di/SettingsModule.kt`
- Modify: `composeApp/src/commonMain/kotlin/di/WordModule.kt`
- Modify: `feature/words/src/commonMain/kotlin/feature/words/di/WordsModule.kt` (Task 9 adds the arg)

- [x] **Step 1: Register data**

In `SettingsModule.kt`, next to the settings data sources:

```kotlin
    single<ILearningFocusLocalDataSource> { LearningFocusLocalDataSourceImpl(queries = get()) }
    single<ILearningFocusRepository> { LearningFocusRepositoryImpl(localDataSource = get()) }
```

- [x] **Step 2: Register use cases**

In `WordModule.kt`:

```kotlin
    // Use Cases - Learning Focus
    // Explicit lambdas: these take a `nowMillis` default param that Koin's constructor DSL can't resolve.
    factory { ObserveLearningFocusUseCase(wordRepository = get(), focusRepository = get()) }
    factory { ObserveStudyFocusUseCase(wordRepository = get(), tagRepository = get(), focusRepository = get()) }
    factoryOf(::SetLearningFocusUseCase)
    factory { DismissFocusNudgeUseCase(focusRepository = get()) }
    factoryOf(::AcknowledgeFocusIntroUseCase)
```

Replace `singleOf(::GetDailyWidgetDataUseCase)` with:

```kotlin
    single {
        GetDailyWidgetDataUseCase(
            wordRepository = get(),
            streakRepository = get(),
            widgetRefresher = get(),
            observeLearningFocus = get(),
        )
    }
```

`LoadReviewQueueUseCase`, `GetSourceLanguageUseCase` (`singleOf`) and `GetWordRushWordsUseCase` (`factoryOf` in StudyModule) resolve the new parameter automatically.

- [x] **Step 3: Verify graph compiles and app assembles**

Run: `./gradlew composeApp:assembleDebug`
Expected: BUILD SUCCESSFUL. If a Koin `verify()`/`checkModules` test exists, run `./gradlew composeApp:testDebugUnitTest` and expect PASS.

- [x] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/di
git commit -m "feat(focus): register learning focus in DI"
```

---

### Task 8: StudyProgressViewModel

**Files:**
- Modify: `feature/study/src/commonMain/kotlin/feature/study/StudyProgressViewModel.kt`
- Modify: `feature/study/src/commonMain/kotlin/feature/study/di/StudyModule.kt`
- Modify: `composeApp/src/commonTest/kotlin/presentation/feature/study/StudyProgressViewModelTest.kt`

- [x] **Step 1: Write the failing tests**

In `StudyProgressViewModelTest`:
- Change `fakeWordRepo()` to `fakeWordRepo(allWords: Flow<List<Word>> = emptyFlow())` with `override fun getAllWords() = allWords`. The default `emptyFlow()` keeps `initial progress state is Loading` valid.
- Replace `createViewModel` with:

```kotlin
    private val now = 1_000_000_000L
    private lateinit var focusRepo: FakeLearningFocusRepository

    private fun word(id: Int, language: Language, tags: List<Long> = emptyList()) = Word(
        id = id, originalWord = "w$id", translation = "t$id", description = "",
        sourceLanguage = language, targetLanguage = Language.ENGLISH,
        nextReviewDate = now - 1, tagIds = tags,
    )

    private fun createViewModel(
        hasPremiumAccess: Boolean = false,
        tags: List<Tag> = emptyList(),
        words: Flow<List<Word>> = emptyFlow(),
        preference: LearningFocus? = null,
    ): StudyProgressViewModel {
        val wordRepo = fakeWordRepo(words)
        val settingsRepo = fakeSettingsRepo()
        val notifRepo = fakeNotifRepo()
        focusRepo = FakeLearningFocusRepository(preference)
        return StudyProgressViewModel(
            evaluateProgressUseCase = EvaluateProgressUseCase(),
            scheduleNotificationsUseCase = ScheduleNotificationsUseCase(notifRepo, settingsRepo),
            analyticsTracker = fakeAnalytics(),
            performanceTracer = FakePerformanceTracer(),
            getFeatureAccessUseCase = GetFeatureAccessUseCase(fakeAuthRepo(hasPremiumAccess), FakeSubscriptionManager()),
            tagUseCases = StudyTagUseCases(
                getSkipTagSelector = GetSkipTagSelectorUseCase(settingsRepo),
                setSkipTagSelector = SetSkipTagSelectorUseCase(settingsRepo),
            ),
            focusUseCases = StudyFocusUseCases(
                observeStudyFocus = ObserveStudyFocusUseCase(wordRepo, fakeTagRepo(tags), focusRepo) { now },
                setLearningFocus = SetLearningFocusUseCase(focusRepo),
                dismissFocusNudge = DismissFocusNudgeUseCase(focusRepo) { now },
                acknowledgeFocusIntro = AcknowledgeFocusIntroUseCase(focusRepo),
            ),
        )
    }
```

- Replace the `tags when repository emits tags exposes them in state` test with:

```kotlin
    @Test
    fun `tags are counted from focused words`() = runTest {
        val tags = listOf(
            Tag(id = 1L, name = "Travel", wordCount = 99L, createdAt = 0L, updatedAt = 0L),
            Tag(id = 2L, name = "Work", wordCount = 99L, createdAt = 0L, updatedAt = 0L),
        )
        val words = flowOf(listOf(word(1, Language.GERMAN, tags = listOf(1L)), word(2, Language.GERMAN, tags = listOf(1L))))
        val vm = createViewModel(tags = tags, words = words)
        // single language → All focus → empty tags kept, counts recomputed from words
        assertEquals(listOf(1L to 2L, 2L to 0L), vm.currentState.tags.map { it.id to it.wordCount })
    }
```

- Add:

```kotlin
    private val mixedWords = buildList {
        add(word(1, Language.GERMAN))
        repeat(10) { add(word(100 + it, Language.SPANISH)) }
    }

    @Test
    fun `two languages expose switcher, focus, intro and nudge`() = runTest {
        val vm = createViewModel(words = flowOf(mixedWords), preference = LearningFocus.Single(Language.GERMAN))
        val state = vm.currentState
        assertEquals(LearningFocus.Single(Language.GERMAN), state.focus)
        assertTrue(state.showFocusSwitcher)
        assertTrue(state.showIntro)
        assertEquals(Language.SPANISH, state.nudge?.language)
        assertEquals(1, (state.progress as UiState.Loaded).value.progressStats.totalWords)
    }

    @Test
    fun `selectFocus rescopes stats and acknowledges intro`() = runTest {
        val vm = createViewModel(words = flowOf(mixedWords), preference = LearningFocus.Single(Language.GERMAN))
        vm.selectFocus(LearningFocus.Single(Language.SPANISH))
        assertEquals(10, (vm.currentState.progress as UiState.Loaded).value.progressStats.totalWords)
        assertFalse(vm.currentState.showIntro)
    }

    @Test
    fun `dismissNudge hides nudge`() = runTest {
        val vm = createViewModel(words = flowOf(mixedWords), preference = LearningFocus.Single(Language.GERMAN))
        vm.dismissNudge()
        assertNull(vm.currentState.nudge)
    }

    @Test
    fun `acknowledgeIntro hides intro`() = runTest {
        val vm = createViewModel(words = flowOf(mixedWords))
        vm.acknowledgeIntro()
        assertFalse(vm.currentState.showIntro)
    }

    @Test
    fun `single language hides switcher`() = runTest {
        val vm = createViewModel(words = flowOf(listOf(word(1, Language.GERMAN))))
        assertFalse(vm.currentState.showFocusSwitcher)
        assertEquals(LearningFocus.All, vm.currentState.focus)
    }
```

(Add imports for the focus use cases, `LearningFocus`, `FakeLearningFocusRepository`, `StudyFocusUseCases`, `assertTrue`, `assertFalse`, `assertNull`. Remove the now-unused `GetDueTagsUseCase`, `GetTagsByLevelUseCase`, `GetTagsUseCase` and `GetProgressStatsUseCase` imports.)

- [x] **Step 2: Run to verify failure**

Run: `./gradlew composeApp:testDebugUnitTest --tests "presentation.feature.study.StudyProgressViewModelTest"`
Expected: FAIL: compile errors (`StudyFocusUseCases`, `focus`, `selectFocus` unresolved)

- [x] **Step 3: Implement**

Replace the top of `StudyProgressViewModel.kt` (bundles + state) and the observation code:

```kotlin
data class StudyTagUseCases(
    val getSkipTagSelector: GetSkipTagSelectorUseCase,
    val setSkipTagSelector: SetSkipTagSelectorUseCase,
)

data class StudyFocusUseCases(
    val observeStudyFocus: ObserveStudyFocusUseCase,
    val setLearningFocus: SetLearningFocusUseCase,
    val dismissFocusNudge: DismissFocusNudgeUseCase,
    val acknowledgeFocusIntro: AcknowledgeFocusIntroUseCase,
)

data class StudyProgressState(
    val progress: UiState<ProgressScreenState> = UiState.Loading,
    val hasPremiumAccess: Boolean = false,
    val dueTags: List<Tag> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val skipTagSelector: Boolean = false,
    val stageTagsMap: Map<Int, List<Tag>> = emptyMap(),
    val focus: LearningFocus = LearningFocus.All,
    val languages: List<LanguageSummary> = emptyList(),
    val nudge: LanguageSummary? = null,
    val showIntro: Boolean = false,
) {
    val showFocusSwitcher: Boolean get() = languages.size >= 2
}

class StudyProgressViewModel(
    private val evaluateProgressUseCase: EvaluateProgressUseCase,
    private val scheduleNotificationsUseCase: ScheduleNotificationsUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val performanceTracer: IPerformanceTracer,
    getFeatureAccessUseCase: GetFeatureAccessUseCase,
    private val tagUseCases: StudyTagUseCases,
    private val focusUseCases: StudyFocusUseCases,
) : BaseViewModel<StudyProgressState, Nothing>() {

    override fun initialState() = StudyProgressState()

    private var progressObservationJob: Job? = null

    init {
        observeFeatureAccess(getFeatureAccessUseCase)
        startObservingProgress()
        observeSkipTagSelector()
    }

    fun selectFocus(focus: LearningFocus) {
        viewModelScope.launch {
            focusUseCases.setLearningFocus(focus)
            focusUseCases.acknowledgeFocusIntro()
            analyticsTracker.logEvent(
                "learning_focus_changed",
                mapOf("focus" to ((focus as? LearningFocus.Single)?.language?.code ?: "all")),
            )
        }
    }

    fun dismissNudge() {
        viewModelScope.launch { focusUseCases.dismissFocusNudge() }
    }

    fun acknowledgeIntro() {
        viewModelScope.launch { focusUseCases.acknowledgeFocusIntro() }
    }
```

Delete `startObservingDueTags`, `startObservingTags` and `startObservingTagsByLevel`. Replace `startObservingProgress` with:

```kotlin
    private fun startObservingProgress() {
        progressObservationJob = viewModelScope.launch {
            val trace = performanceTracer.startTrace("study_session_load")
            focusUseCases.observeStudyFocus()
                .collect { overview ->
                    val stats = overview.progressStats
                    val screenState = ProgressScreenState(
                        progressStats = stats,
                        progressEvaluation = evaluateProgressUseCase(stats).getOrThrow(),
                    )
                    updateState {
                        copy(
                            progress = UiState.Loaded(screenState),
                            focus = overview.focus,
                            languages = overview.languages,
                            nudge = overview.nudge,
                            showIntro = overview.showIntro,
                            tags = overview.tagStats.tags,
                            dueTags = overview.tagStats.dueTags,
                            stageTagsMap = overview.tagStats.tagsByLevel,
                        )
                    }
                    performanceTracer.putMetric(trace, "total_words", stats.totalWords.toLong())
                    performanceTracer.putMetric(trace, "due_cards", stats.dueCards.toLong())
                    performanceTracer.stopTrace(trace)

                    analyticsTracker.updateUserProgress(
                        totalWords = stats.totalWords,
                        matureWords = stats.matureWords,
                        currentStreak = 0
                    )

                    val notifStrings =
                        NotificationStringHelper.getNotificationResources(stats.dueCards)
                    val title = getString(
                        notifStrings.titleRes,
                        *notifStrings.titleParams.toTypedArray()
                    )
                    val message = getString(
                        notifStrings.messageRes,
                        *notifStrings.messageParams.toTypedArray()
                    )
                    scheduleNotificationsUseCase(
                        stats = stats,
                        titleProvider = { title },
                        messageProvider = { message }
                    )
                }
        }
    }
```

Local reminders now use the focused due count, and they reschedule when focus changes because the stream re-emits. Fix imports: add `domain.focus.model.*` and `domain.focus.usecase.*`; remove `GetProgressStatsUseCase`, `GetDueTagsUseCase`, `GetTagsByLevelUseCase`, `GetTagsUseCase` and the unused `catch` if any.

`StudyModule.kt`:

```kotlin
    viewModel {
        StudyProgressViewModel(
            evaluateProgressUseCase = get(),
            scheduleNotificationsUseCase = get(),
            getFeatureAccessUseCase = get(),
            analyticsTracker = get(),
            performanceTracer = get(),
            tagUseCases = StudyTagUseCases(
                getSkipTagSelector = get(),
                setSkipTagSelector = get(),
            ),
            focusUseCases = StudyFocusUseCases(
                observeStudyFocus = get(),
                setLearningFocus = get(),
                dismissFocusNudge = get(),
                acknowledgeFocusIntro = get(),
            ),
        )
    }
```

- [x] **Step 4: Run to verify pass**

Run: `./gradlew composeApp:testDebugUnitTest --tests "presentation.feature.study.*"`
Expected: PASS

- [x] **Step 5: Commit**

```bash
git add feature/study/src/commonMain composeApp/src/commonTest
git commit -m "feat(focus): drive study progress from focused overview"
```

---

### Task 9: Word Manager starts filtered to focus

**Files:**
- Modify: `feature/words/src/commonMain/kotlin/feature/words/WordManagerViewModel.kt`
- Modify: `feature/words/src/commonMain/kotlin/feature/words/di/WordsModule.kt`
- Modify: `composeApp/src/commonTest/kotlin/presentation/viewmodel/WordManagerViewModelTest.kt`

- [x] **Step 1: Write the failing test**

In `WordManagerViewModelTest`, add `observeLearningFocus = ObserveLearningFocusUseCase(wordRepo, focusRepo)` to the VM construction, where `focusRepo` is a `FakeLearningFocusRepository` field (default `null` preference). Add:

```kotlin
    @Test
    fun `filter starts at focused language when user has several languages`() = runTest {
        // seed wordRepo with one GERMAN and one SPANISH word, focusRepo.preference = Single(SPANISH), then build VM
        val vm = createViewModel()
        assertEquals(Language.SPANISH, vm.currentState.filterLanguage)
    }

    @Test
    fun `changing filter does not change study focus`() = runTest {
        val vm = createViewModel()
        vm.setFilterLanguage(null)
        assertEquals(LearningFocus.Single(Language.SPANISH), focusRepo.preference.value)
    }
```

Seed through whatever the test's existing word-repo setup is. `ObserveLearningFocusUseCase` reads `getAllWords()`, so that repo must return both words from it.

- [x] **Step 2: Run to verify failure**

Run: `./gradlew composeApp:testDebugUnitTest --tests "presentation.viewmodel.WordManagerViewModelTest"`
Expected: FAIL: unknown parameter `observeLearningFocus`

- [x] **Step 3: Implement**

Add the constructor param `private val observeLearningFocus: ObserveLearningFocusUseCase,` (last position) and, in `init`:

```kotlin
        viewModelScope.launch {
            // Default only: Single implies 2+ languages; user changes here never touch study focus.
            val focus = observeLearningFocus().first()
            if (focus is LearningFocus.Single && currentState.filterLanguage == null) {
                updateState { copy(filterLanguage = focus.language) }
            }
        }
```

In `WordsModule.kt` add `observeLearningFocus = get(),` to the `WordManagerViewModel(...)` block.

- [x] **Step 4: Run to verify pass**

Run: `./gradlew composeApp:testDebugUnitTest --tests "presentation.viewmodel.WordManagerViewModelTest"`
Expected: PASS

- [x] **Step 5: Commit**

```bash
git add feature/words/src/commonMain composeApp/src/commonTest
git commit -m "feat(focus): default word manager filter to focus language"
```

---

### Task 10: UI components

**Files:**
- Create: `design-system/src/commonMain/kotlin/components/LanguageBadge.kt`
- Create: `feature/study/src/commonMain/kotlin/feature/study/ui/focus/LearningFocusComponents.kt`
- Modify: `resources/src/commonMain/composeResources/values/strings.xml`

UI-only. It's covered by the ViewModel tests plus browser/device verification in Task 12.

- [x] **Step 1: Strings** (append inside `<resources>`)

```xml
    <string name="focus_on">Focus on</string>
    <string name="focus_all_languages">All languages</string>
    <string name="focus_all_languages_subtitle">Mixed review</string>
    <string name="focus_language_counts">%1$d words · %2$d due</string>
    <string name="focus_nudge_message">%1$d %2$s words are waiting</string>
    <string name="focus_nudge_switch">Switch</string>
    <string name="focus_intro_message">You're learning %1$d languages. Focus on one at a time — switch anytime here.</string>
    <string name="focus_intro_got_it">Got it</string>
    <string name="focus_switcher_open">Change focus language</string>
    <string name="focus_dismiss">Dismiss</string>
```

- [x] **Step 2: `LanguageBadge`** (generic: label + tint passed in)

```kotlin
package components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import theme.Theme

/** Round badge with a short label (e.g. a language code), tinted by the caller. */
@Composable
fun LanguageBadge(
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = Theme.dimensions.iconSizeLarge,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = Theme.opacity.dragged)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = tint,
        )
    }
}
```

- [x] **Step 3: Focus components**

```kotlin
package feature.study.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import components.LanguageBadge
import domain.focus.model.LanguageSummary
import domain.focus.model.LearningFocus
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.focus_all_languages
import lexicon.resources.generated.resources.focus_all_languages_subtitle
import lexicon.resources.generated.resources.focus_dismiss
import lexicon.resources.generated.resources.focus_intro_got_it
import lexicon.resources.generated.resources.focus_intro_message
import lexicon.resources.generated.resources.focus_language_counts
import lexicon.resources.generated.resources.focus_nudge_message
import lexicon.resources.generated.resources.focus_nudge_switch
import lexicon.resources.generated.resources.focus_on
import lexicon.resources.generated.resources.focus_switcher_open
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.Language

/** Stable per-language tint from theme roles (no hardcoded colors). */
@Composable
private fun Language.badgeTint(): Color {
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
    )
    return palette[ordinal % palette.size]
}

@Composable
private fun Language.Badge(size: androidx.compose.ui.unit.Dp = Theme.dimensions.iconSizeLarge) =
    LanguageBadge(label = code.uppercase(), tint = badgeTint(), size = size)

/** Chip + optional one-time intro. Renders nothing for single-language users. */
@Composable
fun LearningFocusHeader(
    focus: LearningFocus,
    showSwitcher: Boolean,
    showIntro: Boolean,
    languageCount: Int,
    onOpenSwitcher: () -> Unit,
    onAcknowledgeIntro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!showSwitcher) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
        LearningFocusChip(focus = focus, onClick = onOpenSwitcher)
        if (showIntro) {
            FocusIntroCard(languageCount = languageCount, onGotIt = onAcknowledgeIntro)
        }
    }
}

@Composable
fun LearningFocusChip(
    focus: LearningFocus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Theme.shapes.pill))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClickLabel = stringResource(Res.string.focus_switcher_open), onClick = onClick)
            .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        when (focus) {
            is LearningFocus.Single -> {
                focus.language.Badge(size = Theme.dimensions.iconSize)
                Text(focus.language.nativeName, style = MaterialTheme.typography.labelLarge)
            }
            LearningFocus.All -> {
                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(Theme.dimensions.iconSize))
                Text(stringResource(Res.string.focus_all_languages), style = MaterialTheme.typography.labelLarge)
            }
        }
        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(Theme.dimensions.iconSizeMedium))
    }
}

@Composable
fun LanguageSwitcherSheetContent(
    focus: LearningFocus,
    languages: List<LanguageSummary>,
    onSelect: (LearningFocus) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.screenGutter, vertical = Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.listGap),
    ) {
        Text(stringResource(Res.string.focus_on), style = MaterialTheme.typography.titleMedium)
        languages.forEach { summary ->
            val option = LearningFocus.Single(summary.language)
            FocusOptionRow(
                leading = { summary.language.Badge() },
                title = summary.language.nativeName,
                subtitle = stringResource(Res.string.focus_language_counts, summary.wordCount, summary.dueCount),
                selected = focus == option,
                hasDue = summary.dueCount > 0 && focus != option,
                onClick = { onSelect(option) },
            )
        }
        HorizontalDivider()
        FocusOptionRow(
            leading = { Icon(Icons.Default.Language, contentDescription = null) },
            title = stringResource(Res.string.focus_all_languages),
            subtitle = stringResource(Res.string.focus_all_languages_subtitle),
            selected = focus == LearningFocus.All,
            hasDue = false,
            onClick = { onSelect(LearningFocus.All) },
        )
    }
}

@Composable
private fun FocusOptionRow(
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String,
    selected: Boolean,
    hasDue: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.medium))
            .clickable(onClick = onClick)
            .padding(Theme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        leading()
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (hasDue) {
            Box(Modifier.size(Theme.spacing.xs).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun FocusNudgeCard(
    summary: LanguageSummary,
    onSwitch: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.dimensions.cardCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = Theme.spacing.cardPadding, top = Theme.spacing.xxs, bottom = Theme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        summary.language.Badge(size = Theme.dimensions.iconSize)
        Text(
            text = stringResource(Res.string.focus_nudge_message, summary.dueCount, summary.language.displayName),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onSwitch) { Text(stringResource(Res.string.focus_nudge_switch)) }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.focus_dismiss))
        }
    }
}

@Composable
fun FocusIntroCard(
    languageCount: Int,
    onGotIt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.dimensions.cardCornerRadius))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(start = Theme.spacing.cardPadding, top = Theme.spacing.xs, bottom = Theme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.focus_intro_message, languageCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onGotIt) { Text(stringResource(Res.string.focus_intro_got_it)) }
    }
}
```

If `feature/study` does not depend on `:domain` directly, check its `build.gradle.kts`. ViewModels there already import `domain.*`, so it should.

- [x] **Step 4: Compile**

Run: `./gradlew composeApp:compileKotlinMetadata composeApp:assembleDebug`
Expected: BUILD SUCCESSFUL

- [x] **Step 5: Commit**

```bash
git add design-system/src/commonMain/kotlin/components/LanguageBadge.kt feature/study/src/commonMain/kotlin/feature/study/ui/focus resources/src/commonMain/composeResources/values/strings.xml
git commit -m "feat(focus): add focus chip, switcher sheet, nudge and intro components"
```

---

### Task 11: Wire into StudyScreen

**Files:**
- Modify: `presentation/src/commonMain/kotlin/presentation/ui/screens/StudyScreen.kt`

- [x] **Step 1: Add the switcher opener** (after `openImportSheet`)

```kotlin
    val openFocusSwitcher: () -> Unit = {
        overlayHost.showSizeToFitBottomSheet(tag = "focus-switcher") { sheetNav ->
            val sheetState by progressViewModel.state()
            LanguageSwitcherSheetContent(
                focus = sheetState.focus,
                languages = sheetState.languages,
                onSelect = { focus ->
                    progressViewModel.selectFocus(focus)
                    sheetNav.dismiss()
                },
            )
        }
    }
```

If `showSizeToFitBottomSheet` requires `properties`, pass the same default the other non-import call sites in this file use.

- [x] **Step 2: Header + nudge in the `UiState.Loaded` branch**

Directly before `StatsSection(`:

```kotlin
                    LearningFocusHeader(
                        focus = progressState.focus,
                        showSwitcher = progressState.showFocusSwitcher,
                        showIntro = progressState.showIntro,
                        languageCount = progressState.languages.size,
                        onOpenSwitcher = openFocusSwitcher,
                        onAcknowledgeIntro = progressViewModel::acknowledgeIntro,
                        modifier = Modifier.padding(bottom = Theme.spacing.sm),
                    )
```

Directly after the `StatsSection(...)` call:

```kotlin
                    progressState.nudge?.let { nudge ->
                        FocusNudgeCard(
                            summary = nudge,
                            onSwitch = { progressViewModel.selectFocus(LearningFocus.Single(nudge.language)) },
                            onDismiss = progressViewModel::dismissNudge,
                            modifier = Modifier.padding(top = Theme.spacing.sm),
                        )
                    }
```

Imports: `feature.study.ui.focus.LearningFocusHeader`, `LanguageSwitcherSheetContent`, `FocusNudgeCard`, and `domain.focus.model.LearningFocus`.

- [x] **Step 3: Compile + run all tests**

Run: `./gradlew composeApp:assembleDebug composeApp:cleanAllTests composeApp:allTests`
Expected: BUILD SUCCESSFUL, 0 failures

- [x] **Step 4: Commit**

```bash
git add presentation/src/commonMain/kotlin/presentation/ui/screens/StudyScreen.kt
git commit -m "feat(focus): show focus chip, switcher and nudge on study screen"
```

---

### Task 12: Verification

- [x] **Step 1: Full suite + lint**

Run: `./gradlew composeApp:cleanAllTests composeApp:allTests detekt`
Expected: 0 test failures, no new detekt issues in the touched files

- [x] **Step 2: iOS framework compiles**

Run: `./gradlew composeApp:linkDebugFrameworkIosSimulatorArm64`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Device check (Android emulator)**

Install the debug build on an existing install, so migration `13.sqm` runs. Check each of these:
1. With only one language, there's no chip and the Study screen looks as it does today.
2. Add 1 German and 12 Spanish words. The chip appears, the intro card shows, and focus = the most recently reviewed language (or Spanish, which has the most due).
3. Open the switcher. Language rows show counts. Pick German: the stats, due count and Review queue show only German, and the intro disappears.
4. The nudge "12 Spanish words are waiting" appears. Tap Switch: focus changes to Spanish. Switch back and dismiss: the nudge stays hidden after an app restart (same day).
5. Choose "All languages": the queue is mixed and there's no nudge.
6. Word Manager opens filtered to the focus language. Clearing the filter doesn't change the study chip.
7. Word Rush only uses focused-language words.
8. Delete every word of the focused language: focus falls back automatically, with no crash or empty-queue dead end.
9. Log out and back in: the focus preference is reset (smart default applies).

- [ ] **Step 4: UX review**

Run the `ux-reviewer` agent on `StudyScreen.kt` and `LearningFocusComponents.kt`. Fix must-fix items.

- [ ] **Step 5: Update plan status and final commit**

```bash
git add docs/plans/2026-10-02-learning-focus.md
git commit -m "docs(plan): mark learning focus plan complete"
```

---

## Spec Coverage

| Spec requirement | Task |
|---|---|
| Focus model, smart default, stale-pref fallback | 1, 3 |
| Local storage, wiped by clearSettings | 6 |
| Queue scoped, daily-goal cap after filter | 4 |
| Progress / level buckets / tags scoped | 1, 3, 8 |
| Word Rush, widget, local reminders scoped | 5, 8 |
| Add/import default language | 5 |
| Word Manager default filter, independent afterwards | 9 |
| Chip, switcher sheet, All option | 10, 11 |
| Nudge ≥10, once/day, never in All | 1, 10, 11 |
| One-time intro | 3, 8, 10, 11 |
| Streak, backend push, analytics stay global | untouched by design |
| Deferred: import snackbar, session-header badge, string translations | listed under Decisions |
