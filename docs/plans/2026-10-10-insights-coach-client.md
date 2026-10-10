# Insights Coach — Client Implementation Plan

> **For agentic workers:** Use claude-kit-v2:implement to execute this plan task-by-task.

**Goal:** Replace the Insights tab with a coach-first screen (weekly hero, up to 3 actionable coach cards, collapsible deep-dive sections) fed by one cached call to `GET /analytics/insights-screen`, behind the `insights_coach` feature flag.

**Architecture:** `data` fetches the screen DTO, stores the raw JSON in SQLDelight, and exposes a `Flow` of the cached screen (stale-while-revalidate). `domain` owns pure models and four tiny use cases. `feature/insights/coach` has a new `InsightsCoachViewModel`, a pure `InsightsUiMapper` that applies every display rule from the spec (§6), and small single-purpose composables. Coach actions reach the Study tab through the existing `NotificationNavigator`, which gains "open review with source" and "open Word Rush" requests.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Koin, Ktor, SQLDelight (async driver), kotlinx.serialization, kotlinx-datetime 0.7, kotlin-test + Turbine + coroutines-test, Maestro.

**Spec:** `docs/specs/2026-10-10-insights-coach-design.md`
**Depends on:** `docs/plans/2026-10-10-insights-coach-backend.md` deployed (endpoint live).

**Fixes an existing bug on the way:** today the Insights "Study difficult words" button emits `NavigateToReviewWithWords`, but `insightsGraph` never wires `onNavigateToReview`, so the tap does nothing. Task 1–2 add the missing review path; the new screen uses it.

**Deviation from spec:** dismissed card IDs live in a SQLDelight table (not a key-value store) so they share the cache migration and are cleared with it.

Shorthand:
- `TEST = composeApp/src/commonTest/kotlin`
- Common test command: `./gradlew composeApp:testDebugUnitTest --tests "<fqcn>"`
- Full suite: `./gradlew composeApp:cleanAllTests composeApp:allTests`

---

## File map

| File | Responsibility |
|---|---|
| `domain/src/commonMain/kotlin/domain/word/model/ReviewSource.kt` (modify) | add `ByWords` |
| `domain/src/commonMain/kotlin/domain/word/usecase/LoadReviewQueueUseCase.kt` (modify) | resolve `ByWords` |
| `feature/study/src/commonMain/kotlin/feature/study/ReviewViewModel.kt` (modify) | `ByWords` → `ReviewType.REVIEW` |
| `presentation/src/commonMain/kotlin/presentation/navigation/NotificationNavigator.kt` (modify) | review-with-source + Word Rush requests |
| `presentation/src/commonMain/kotlin/presentation/ui/screens/StudyScreen.kt` (modify) | collect both requests |
| `domain/src/commonMain/kotlin/domain/insights/model/InsightsScreen.kt` | domain models |
| `domain/src/commonMain/kotlin/domain/insights/repository/*.kt` | repository interfaces |
| `domain/src/commonMain/kotlin/domain/insights/usecase/*.kt` | 4 use cases |
| `data/src/commonMain/kotlin/data/insights/remote/*.kt` | DTOs, remote data source |
| `data/src/commonMain/kotlin/data/insights/InsightsScreenMappers.kt` | DTO → domain |
| `data/src/commonMain/sqldelight/data/core/database/20.sqm` + `Lexicon.sq` (modify) | cache + dismissed tables |
| `data/src/commonMain/kotlin/data/insights/local/InsightsScreenLocalDataSource.kt` | cache I/O |
| `data/src/commonMain/kotlin/data/insights/repository/*.kt` | repository impls |
| `data/src/commonMain/kotlin/data/auth/repository/AuthRepositoryImpl.kt` (modify) | clear cache on logout/delete |
| `platforms/src/*/kotlin/time/HourFormat*.kt` | `is24HourClock()` expect/actual |
| `feature/insights/src/commonMain/kotlin/feature/insights/coach/InsightsFormatter.kt` | numbers/hours/relative time |
| `feature/insights/src/commonMain/kotlin/feature/insights/coach/model/InsightsUiModel.kt` | UI models |
| `feature/insights/src/commonMain/kotlin/feature/insights/coach/InsightsUiMapper.kt` | domain → UI |
| `feature/insights/src/commonMain/kotlin/feature/insights/coach/InsightsCoachViewModel.kt` | state + event sink |
| `feature/insights/src/commonMain/kotlin/feature/insights/coach/ui/*.kt` | composables (one concern per file) |
| `feature/insights/src/commonMain/kotlin/feature/insights/navigation/InsightsRoute.kt` (modify) | flag switch + new callbacks |
| `feature/insights/src/commonMain/kotlin/feature/insights/di/InsightsModule.kt` (modify) | register VM |
| `composeApp/src/commonMain/kotlin/di/AnalyticsModule.kt` (modify) | register data + use cases |
| `presentation/src/commonMain/kotlin/presentation/ui/NavigationGraph.kt` (modify) | wire callbacks |
| `resources/src/commonMain/composeResources/values/strings.xml` (modify) | new strings |
| `maestro/flows/insights/01_insights_coach.yaml` | E2E flow |

---

### Task 1: `ReviewSource.ByWords`

**Files:**
- Modify: `domain/src/commonMain/kotlin/domain/word/model/ReviewSource.kt`
- Modify: `domain/src/commonMain/kotlin/domain/word/usecase/LoadReviewQueueUseCase.kt`
- Modify: `feature/study/src/commonMain/kotlin/feature/study/ReviewViewModel.kt`
- Test: `TEST/domain/word/usecase/LoadReviewQueueUseCaseTest.kt`

- [ ] **Step 1: Write the failing test** — add to `LoadReviewQueueUseCaseTest`:

```kotlin
    @Test
    fun `ByWords returns only the requested words in focus, ignoring daily goal`() = runTest {
        val words = (1..5).map { testWord(it) }
        val repo = ConfigurableWordRepository(allWords = words)
        val useCase = buildUseCase(repo, dailyGoal = 1)

        val result = useCase(ReviewSource.ByWords(listOf(2L, 4L, 99L)))

        assertEquals(listOf(words[1], words[3]), result.getOrNull())
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew composeApp:testDebugUnitTest --tests "domain.word.usecase.LoadReviewQueueUseCaseTest"`
Expected: FAIL — `Unresolved reference: ByWords`.

- [ ] **Step 3: Implement**

`ReviewSource.kt` — add inside the sealed class:

```kotlin
    /** A hand-picked set of words, e.g. from an Insights coach card. Not capped by the daily goal. */
    data class ByWords(val wordIds: List<Long>) : ReviewSource() {
        override val sessionTypeLabel: String = "REVIEW"
    }
```

`LoadReviewQueueUseCase.kt` — add constructor param and branch:

```kotlin
class LoadReviewQueueUseCase(
    private val getDueWords: GetDueWordsUseCase,
    private val getWordsByStage: GetWordsByStageUseCase,
    private val getDueWordsByTag: GetDueWordsByTagUseCase,
    private val getDailyGoalWords: GetDailyGoalWordsUseCase,
    private val observeLearningFocus: ObserveLearningFocusUseCase,
    private val wordRepository: IWordRepository,
) : UseCase<ReviewSource, List<Word>> {
```

```kotlin
            is ReviewSource.ByWords -> {
                val ids = params.wordIds.toSet()
                wordRepository.getAllWords().first().filter { it.id.toLong() in ids }.filterBy(focus)
            }
```

(import `domain.word.repository.IWordRepository`.) Koin `singleOf(::LoadReviewQueueUseCase)` in `composeApp/src/commonMain/kotlin/di/WordModule.kt` resolves the new param automatically.

Update the test builder `buildUseCase(...)` to pass `repo` as the last argument:

```kotlin
        return LoadReviewQueueUseCase(
            getDueWords,
            getWordsByStage,
            getDueWordsByTag,
            getDailyGoalWords,
            observeLearningFocus,
            repo,
        )
```

`ReviewViewModel.kt` — make the `when` exhaustive:

```kotlin
    private fun ReviewSource.toReviewType() = when (this) {
        is ReviewSource.DueCards, is ReviewSource.ByTag, is ReviewSource.ByWords -> ReviewType.REVIEW
        is ReviewSource.ByStage, is ReviewSource.ByStageAndTag -> ReviewType.BROWSE
    }
```

- [ ] **Step 4: Find every other exhaustive `when` on `ReviewSource`**

Run: `grep -rn "is ReviewSource\." --include='*.kt' . | grep -v /build/`
Expected: only the two files above. If others appear, add `ByWords` to them with the `REVIEW` semantics.

- [ ] **Step 5: Run tests**

Run: `./gradlew composeApp:testDebugUnitTest --tests "domain.word.usecase.LoadReviewQueueUseCaseTest" --tests "*ReviewViewModelTest*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add domain/src/commonMain/kotlin/domain/word feature/study/src/commonMain/kotlin/feature/study/ReviewViewModel.kt composeApp/src/commonTest/kotlin/domain/word/usecase/LoadReviewQueueUseCaseTest.kt
git commit -m "feat(review): support reviewing a hand-picked set of words"
```

---

### Task 2: Study launch requests from other tabs

**Files:**
- Modify: `presentation/src/commonMain/kotlin/presentation/navigation/NotificationNavigator.kt`
- Modify: `presentation/src/commonMain/kotlin/presentation/ui/screens/StudyScreen.kt`
- Test: `TEST/presentation/navigation/NotificationNavigatorTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package presentation.navigation

import app.cash.turbine.test
import domain.word.model.ReviewSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationNavigatorTest {

    @Test
    fun `openReview switches to Study and requests that source`() = runTest {
        val navigator = NotificationNavigator()
        navigator.openReview(ReviewSource.ByWords(listOf(1L, 2L)))

        navigator.destinations.test { assertEquals(NotificationDestination.Study, awaitItem()) }
        navigator.reviewRequests.test { assertEquals(ReviewSource.ByWords(listOf(1L, 2L)), awaitItem()) }
    }

    @Test
    fun `openDueReview requests due cards`() = runTest {
        val navigator = NotificationNavigator()
        navigator.openDueReview()
        navigator.reviewRequests.test { assertEquals(ReviewSource.DueCards, awaitItem()) }
    }

    @Test
    fun `openWordRush switches to Study and requests a game`() = runTest {
        val navigator = NotificationNavigator()
        navigator.openWordRush()
        navigator.destinations.test { assertEquals(NotificationDestination.Study, awaitItem()) }
        navigator.wordRushRequests.test { assertEquals(Unit, awaitItem()) }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew composeApp:testDebugUnitTest --tests "presentation.navigation.NotificationNavigatorTest"`
Expected: FAIL — `Unresolved reference: openReview`.

- [ ] **Step 3: Implement** — replace the body of `NotificationNavigator`:

```kotlin
class NotificationNavigator {

    private val requests = Channel<NotificationDestination>(Channel.CONFLATED)
    private val reviews = Channel<ReviewSource>(Channel.CONFLATED)
    private val wordRush = Channel<Unit>(Channel.CONFLATED)

    /** One-shot destinations; collect with OnEvents. */
    val destinations: Flow<NotificationDestination> = requests.receiveAsFlow()

    /** One-shot requests to start a review; collected by the Study screen once it's shown. */
    val reviewRequests: Flow<ReviewSource> = reviews.receiveAsFlow()

    /** One-shot requests to start a Word Rush game; collected by the Study screen. */
    val wordRushRequests: Flow<Unit> = wordRush.receiveAsFlow()

    fun open(destination: NotificationDestination) {
        requests.trySend(destination)
    }

    /** Switches to the Study tab and starts a review of the due cards there. */
    fun openDueReview() = openReview(ReviewSource.DueCards)

    /** Switches to the Study tab and starts a review of [source] there. */
    fun openReview(source: ReviewSource) {
        open(NotificationDestination.Study)
        reviews.trySend(source)
    }

    /** Switches to the Study tab and starts a Word Rush game there. */
    fun openWordRush() {
        open(NotificationDestination.Study)
        wordRush.trySend(Unit)
    }
}
```

(add `import domain.word.model.ReviewSource`.)

`StudyScreen.kt` — replace the existing review-reminder collector with:

```kotlin
    val studyLauncher = koinInject<NotificationNavigator>()
    // Review requests from reminders and other tabs: start the review, unless one is already running
    // (restarting it would drop the user's place and orphan the session's analytics).
    OnEvents(studyLauncher.reviewRequests) { source ->
        if (reviewViewModel.currentState.review !is ReviewState.Active) {
            openReviewScreen(source)
        }
    }
```

and after `val openWordRush: () -> Unit = { ... }` is declared, add:

```kotlin
    OnEvents(studyLauncher.wordRushRequests) { openWordRush() }
```

- [ ] **Step 4: Run tests and compile**

Run: `./gradlew composeApp:testDebugUnitTest --tests "presentation.navigation.NotificationNavigatorTest" && ./gradlew composeApp:compileKotlinMetadata`
Expected: PASS, BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add presentation/src/commonMain/kotlin/presentation/navigation/NotificationNavigator.kt presentation/src/commonMain/kotlin/presentation/ui/screens/StudyScreen.kt composeApp/src/commonTest/kotlin/presentation/navigation/NotificationNavigatorTest.kt
git commit -m "feat(study): accept review and word rush requests from other tabs"
```

---

### Task 3: Domain models, repository interfaces, use cases

**Files:**
- Create: `domain/src/commonMain/kotlin/domain/insights/model/InsightsScreen.kt`
- Create: `domain/src/commonMain/kotlin/domain/insights/repository/IInsightsScreenRepository.kt`
- Create: `domain/src/commonMain/kotlin/domain/insights/repository/IDismissedCoachCardsRepository.kt`
- Create: `domain/src/commonMain/kotlin/domain/insights/usecase/InsightsScreenUseCases.kt`
- Create: `test/src/commonMain/kotlin/fakes/FakeInsightsScreenRepository.kt`
- Create: `test/src/commonMain/kotlin/fakes/FakeDismissedCoachCardsRepository.kt`
- Test: `TEST/domain/insights/InsightsScreenUseCasesTest.kt`

- [ ] **Step 1: Write models** — `InsightsScreen.kt`:

```kotlin
package domain.insights.model

import kotlinx.datetime.LocalDate

data class InsightsScreen(
    val totalReviews: Long,
    val hero: WeeklyHero,
    val coach: List<CoachCard>,
    val mastery: MasterySection?,
    val habits: HabitsSection?,
    val words: WordsSection?,
    val wordRush: WordRushSection?,
    val locked: List<LockedSection>,
)

data class Metric(val value: Int, val previous: Int) {
    val change: Int get() = value - previous
}

data class DayCount(val date: LocalDate, val reviews: Int)

data class WeeklyHero(
    val headline: String,
    val reviews: Metric,
    val accuracyPct: Metric?,
    val leveledUp: Metric,
    val currentStreak: Int,
    /** Monday … Sunday of the current week. */
    val week: List<DayCount>,
)

data class WordChip(val id: Long, val text: String)

sealed interface CoachAction {
    data class ReviewWords(val label: String, val wordIds: List<Long>) : CoachAction
    data class StartReview(val label: String) : CoachAction
    data class EnableReminder(val label: String, val hour: Int) : CoachAction
    data class StartWordRush(val label: String) : CoachAction
    data object None : CoachAction
}

data class CoachCard(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val words: List<WordChip>,
    val moreWordsCount: Int,
    val action: CoachAction,
)

data class LevelCount(val level: Int, val words: Long)

data class MasterySection(
    val caption: String,
    val levels: List<LevelCount>,
    val promotedThisWeek: Int,
    val demotedThisWeek: Int,
)

data class BestHour(val hour: Int, val accuracyPct: Int)

data class WeekdayAccuracy(val isoDay: Int, val accuracyPct: Int, val reviews: Int)

data class HabitsSection(
    val caption: String,
    val heatmap: List<DayCount>,
    val bestHour: BestHour?,
    val weekdays: List<WeekdayAccuracy>,
)

data class InsightWord(val id: Long, val text: String, val accuracyPct: Int)

data class WordsSection(val caption: String, val hardest: List<InsightWord>, val comebacks: List<WordChip>)

data class WordRushSection(val caption: String, val gamesPlayed: Long, val bestScore: Int, val recentScores: List<Int>)

enum class InsightSectionKey { MASTERY, HABITS, WORDS, WORD_RUSH }

data class LockedSection(val section: InsightSectionKey, val reviewsNeeded: Int)

data class CachedInsights(val screen: InsightsScreen, val fetchedAtMs: Long)
```

- [ ] **Step 2: Write interfaces**

```kotlin
package domain.insights.repository

import core.common.Try
import domain.insights.model.CachedInsights
import kotlinx.coroutines.flow.Flow

interface IInsightsScreenRepository {
    /** Last cached screen (null before the first successful fetch); re-emits after each refresh. */
    fun observe(): Flow<CachedInsights?>

    /** Fetches a fresh screen computed in [timeZoneId] and replaces the cache. Failure keeps the cache. */
    suspend fun refresh(timeZoneId: String): Try<Unit>
}
```

```kotlin
package domain.insights.repository

import core.common.Try
import kotlinx.coroutines.flow.Flow

interface IDismissedCoachCardsRepository {
    fun observe(): Flow<Set<String>>
    suspend fun dismiss(cardId: String): Try<Unit>
}
```

- [ ] **Step 3: Write fakes** — `FakeInsightsScreenRepository.kt`:

```kotlin
package fakes

import core.common.Try
import domain.insights.model.CachedInsights
import domain.insights.repository.IInsightsScreenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeInsightsScreenRepository(initial: CachedInsights? = null) : IInsightsScreenRepository {
    val cache = MutableStateFlow(initial)
    var nextRefresh: CachedInsights? = null
    var refreshError: Throwable? = null
    val refreshedZones = mutableListOf<String>()

    override fun observe(): Flow<CachedInsights?> = cache

    override suspend fun refresh(timeZoneId: String): Try<Unit> {
        refreshedZones += timeZoneId
        refreshError?.let { return Try.failure(it) }
        nextRefresh?.let { cache.value = it }
        return Try.success(Unit)
    }
}
```

`FakeDismissedCoachCardsRepository.kt`:

```kotlin
package fakes

import core.common.Try
import domain.insights.repository.IDismissedCoachCardsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeDismissedCoachCardsRepository : IDismissedCoachCardsRepository {
    val dismissed = MutableStateFlow<Set<String>>(emptySet())
    override fun observe(): Flow<Set<String>> = dismissed
    override suspend fun dismiss(cardId: String): Try<Unit> {
        dismissed.update { it + cardId }
        return Try.success(Unit)
    }
}
```

- [ ] **Step 4: Write the failing test**

```kotlin
package domain.insights

import app.cash.turbine.test
import core.common.Try
import domain.insights.usecase.DismissCoachCardUseCase
import domain.insights.usecase.ObserveDismissedCoachCardsUseCase
import domain.insights.usecase.ObserveInsightsScreenUseCase
import domain.insights.usecase.RefreshInsightsScreenUseCase
import fakes.FakeDismissedCoachCardsRepository
import fakes.FakeInsightsScreenRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InsightsScreenUseCasesTest {

    @Test
    fun `refresh forwards the time zone and observe emits the cache`() = runTest {
        val repo = FakeInsightsScreenRepository()
        ObserveInsightsScreenUseCase(repo)(Unit).test {
            assertNull(awaitItem())
            repo.nextRefresh = InsightsFixtures.cached()
            assertTrue(RefreshInsightsScreenUseCase(repo)("Europe/Berlin").isSuccess)
            assertEquals(InsightsFixtures.cached(), awaitItem())
        }
        assertEquals(listOf("Europe/Berlin"), repo.refreshedZones)
    }

    @Test
    fun `dismissed ids flow through`() = runTest {
        val repo = FakeDismissedCoachCardsRepository()
        DismissCoachCardUseCase(repo)("WEEK_TREND:2026-10-10")
        ObserveDismissedCoachCardsUseCase(repo)(Unit).test {
            assertEquals(setOf("WEEK_TREND:2026-10-10"), awaitItem())
        }
    }
}
```

And a shared fixture file `TEST/domain/insights/InsightsFixtures.kt` used by later tasks:

```kotlin
package domain.insights

import domain.insights.model.*
import kotlinx.datetime.LocalDate

object InsightsFixtures {
    val MONDAY: LocalDate = LocalDate(2026, 10, 5)

    fun hero(
        reviews: Metric = Metric(212, 194),
        accuracy: Metric? = Metric(84, 79),
        leveledUp: Metric = Metric(31, 22),
        streak: Int = 6,
    ) = WeeklyHero(
        headline = "Your best week in 12 weeks",
        reviews = reviews,
        accuracyPct = accuracy,
        leveledUp = leveledUp,
        currentStreak = streak,
        week = (0..6).map { DayCount(LocalDate.fromEpochDays(MONDAY.toEpochDays() + it), if (it < 5) 30 else 0) },
    )

    fun card(
        type: String = "SLIPPING_WORDS",
        action: CoachAction = CoachAction.ReviewWords("Review 3 words", listOf(1, 2, 3)),
    ) = CoachCard(
        id = "$type:2026-10-10",
        type = type,
        title = "3 words need a refresh",
        body = "They dropped a stage this week.",
        words = listOf(WordChip(1, "ubiquitous"), WordChip(2, "ephemeral"), WordChip(3, "laconic")),
        moreWordsCount = 0,
        action = action,
    )

    fun screen(
        coach: List<CoachCard> = listOf(card()),
        hero: WeeklyHero = hero(),
        habits: HabitsSection? = null,
        locked: List<LockedSection> = listOf(LockedSection(InsightSectionKey.HABITS, 18)),
    ) = InsightsScreen(
        totalReviews = 1420,
        hero = hero,
        coach = coach,
        mastery = MasterySection("31 words moved up a stage this week", (0..6).map { LevelCount(it, 10L * it) }, 31, 12),
        habits = habits,
        words = WordsSection("Focus here for quick wins", listOf(InsightWord(1, "ubiquitous", 33)), emptyList()),
        wordRush = null,
        locked = locked,
    )

    fun cached(screen: InsightsScreen = screen(), fetchedAtMs: Long = 1_000L) = CachedInsights(screen, fetchedAtMs)
}
```

- [ ] **Step 5: Run test to verify it fails**

Run: `./gradlew composeApp:testDebugUnitTest --tests "domain.insights.InsightsScreenUseCasesTest"`
Expected: FAIL — unresolved use case classes.

- [ ] **Step 6: Implement** — `InsightsScreenUseCases.kt`:

```kotlin
package domain.insights.usecase

import core.common.FlowUseCase
import core.common.Try
import core.common.UseCase
import domain.insights.model.CachedInsights
import domain.insights.repository.IDismissedCoachCardsRepository
import domain.insights.repository.IInsightsScreenRepository
import kotlinx.coroutines.flow.Flow

class ObserveInsightsScreenUseCase(
    private val repository: IInsightsScreenRepository,
) : FlowUseCase<Unit, CachedInsights?> {
    override fun invoke(params: Unit): Flow<CachedInsights?> = repository.observe()
}

/** [params] is the IANA time zone id the server should compute days and hours in. */
class RefreshInsightsScreenUseCase(
    private val repository: IInsightsScreenRepository,
) : UseCase<String, Unit> {
    override suspend fun invoke(params: String): Try<Unit> = repository.refresh(params)
}

class ObserveDismissedCoachCardsUseCase(
    private val repository: IDismissedCoachCardsRepository,
) : FlowUseCase<Unit, Set<String>> {
    override fun invoke(params: Unit): Flow<Set<String>> = repository.observe()
}

class DismissCoachCardUseCase(
    private val repository: IDismissedCoachCardsRepository,
) : UseCase<String, Unit> {
    override suspend fun invoke(params: String): Try<Unit> = repository.dismiss(params)
}
```

- [ ] **Step 7: Run test to verify it passes**

Run: `./gradlew composeApp:testDebugUnitTest --tests "domain.insights.InsightsScreenUseCasesTest"`
Expected: PASS (2 tests).

- [ ] **Step 8: Commit**

```bash
git add domain/src/commonMain/kotlin/domain/insights test/src/commonMain/kotlin/fakes/FakeInsightsScreenRepository.kt test/src/commonMain/kotlin/fakes/FakeDismissedCoachCardsRepository.kt composeApp/src/commonTest/kotlin/domain/insights
git commit -m "feat(insights): add coach screen domain models and use cases"
```

---

### Task 4: DTOs, mapper, remote data source

**Files:**
- Create: `data/src/commonMain/kotlin/data/insights/remote/InsightsScreenDto.kt`
- Create: `data/src/commonMain/kotlin/data/insights/remote/InsightsScreenRemoteDataSource.kt`
- Create: `data/src/commonMain/kotlin/data/insights/InsightsScreenMappers.kt`
- Test: `TEST/data/insights/InsightsScreenRemoteDataSourceTest.kt`
- Test: `TEST/data/insights/InsightsScreenMappersTest.kt`

- [ ] **Step 1: Write DTOs** — every optional field has a default so older/newer payloads parse:

```kotlin
package data.insights.remote

import kotlinx.serialization.Serializable

@Serializable
data class InsightsScreenDto(
    val generatedAt: String = "",
    val totalReviews: Long = 0,
    val hero: HeroDto,
    val coach: List<CoachCardDto> = emptyList(),
    val sections: SectionsDto = SectionsDto(),
    val locked: List<LockedSectionDto> = emptyList(),
)

@Serializable data class MetricDto(val value: Int = 0, val previous: Int = 0)
@Serializable data class DayCountDto(val date: String, val reviews: Int = 0)

@Serializable
data class HeroDto(
    val headline: String = "",
    val reviews: MetricDto = MetricDto(),
    val accuracyPct: MetricDto? = null,
    val leveledUp: MetricDto = MetricDto(),
    val currentStreak: Int = 0,
    val week: List<DayCountDto> = emptyList(),
)

@Serializable data class WordChipDto(val id: Long, val text: String)

@Serializable
data class CoachActionDto(
    val kind: String = "NONE",
    val label: String? = null,
    val wordIds: List<Long> = emptyList(),
    val hour: Int? = null,
)

@Serializable
data class CoachCardDto(
    val id: String,
    val type: String = "",
    val priority: Int = 0,
    val title: String,
    val body: String = "",
    val words: List<WordChipDto> = emptyList(),
    val moreWordsCount: Int = 0,
    val action: CoachActionDto = CoachActionDto(),
)

@Serializable
data class SectionsDto(
    val mastery: MasterySectionDto? = null,
    val habits: HabitsSectionDto? = null,
    val words: WordsSectionDto? = null,
    val wordRush: WordRushSectionDto? = null,
)

@Serializable data class LevelCountDto(val level: Int, val words: Long = 0)
@Serializable data class MasterySectionDto(
    val caption: String = "",
    val levels: List<LevelCountDto> = emptyList(),
    val promotedThisWeek: Int = 0,
    val demotedThisWeek: Int = 0,
)
@Serializable data class BestHourDto(val hour: Int, val accuracyPct: Int)
@Serializable data class WeekdayAccuracyDto(val isoDay: Int, val accuracyPct: Int, val reviews: Int = 0)
@Serializable data class HabitsSectionDto(
    val caption: String = "",
    val heatmap: List<DayCountDto> = emptyList(),
    val bestHour: BestHourDto? = null,
    val weekdays: List<WeekdayAccuracyDto> = emptyList(),
)
@Serializable data class WordAccuracyDto(val id: Long, val text: String, val accuracyPct: Int)
@Serializable data class WordsSectionDto(
    val caption: String = "",
    val hardest: List<WordAccuracyDto> = emptyList(),
    val comebacks: List<WordChipDto> = emptyList(),
)
@Serializable data class WordRushSectionDto(
    val caption: String = "",
    val gamesPlayed: Long = 0,
    val bestScore: Int = 0,
    val recentScores: List<Int> = emptyList(),
)
@Serializable data class LockedSectionDto(val section: String, val reviewsNeeded: Int = 0)
```

- [ ] **Step 2: Write the failing mapper test**

```kotlin
package data.insights

import data.insights.remote.*
import domain.insights.model.CoachAction
import domain.insights.model.InsightSectionKey
import domain.insights.model.LockedSection
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InsightsScreenMappersTest {

    private fun card(kind: String, hour: Int? = null, type: String = "X") = CoachCardDto(
        id = "id", type = type, title = "t",
        action = CoachActionDto(kind = kind, label = "Go", wordIds = listOf(1, 2), hour = hour),
    )

    @Test
    fun `maps known action kinds`() {
        assertEquals(CoachAction.ReviewWords("Go", listOf(1, 2)), card("REVIEW_WORDS").toDomain().action)
        assertEquals(CoachAction.StartReview("Go"), card("START_REVIEW").toDomain().action)
        assertEquals(CoachAction.EnableReminder("Go", 20), card("ENABLE_REMINDER", hour = 20).toDomain().action)
        assertEquals(CoachAction.StartWordRush("Go"), card("START_WORD_RUSH").toDomain().action)
    }

    @Test
    fun `unknown kind or reminder without hour becomes None`() {
        assertEquals(CoachAction.None, card("TELEPORT").toDomain().action)
        assertEquals(CoachAction.None, card("ENABLE_REMINDER", hour = null).toDomain().action)
    }

    @Test
    fun `unknown card type is kept for generic rendering`() {
        assertEquals("BRAND_NEW_TYPE", card("NONE", type = "BRAND_NEW_TYPE").toDomain().type)
    }

    @Test
    fun `malformed dates are dropped and unknown locked sections ignored`() {
        val dto = InsightsScreenDto(
            hero = HeroDto(week = listOf(DayCountDto("2026-10-05", 3), DayCountDto("garbage", 9))),
            locked = listOf(LockedSectionDto("habits", 18), LockedSectionDto("future_section", 5)),
        )
        val screen = dto.toDomain()
        assertEquals(listOf(LocalDate(2026, 10, 5)), screen.hero.week.map { it.date })
        assertEquals(listOf(LockedSection(InsightSectionKey.HABITS, 18)), screen.locked)
    }

    @Test
    fun `null sections stay null`() {
        val screen = InsightsScreenDto(hero = HeroDto()).toDomain()
        assertTrue(screen.mastery == null && screen.habits == null && screen.words == null && screen.wordRush == null)
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew composeApp:testDebugUnitTest --tests "data.insights.InsightsScreenMappersTest"`
Expected: FAIL — unresolved `toDomain`.

- [ ] **Step 4: Implement mappers** — `InsightsScreenMappers.kt`:

```kotlin
package data.insights

import data.insights.remote.*
import domain.insights.model.*
import kotlinx.datetime.LocalDate

fun InsightsScreenDto.toDomain() = InsightsScreen(
    totalReviews = totalReviews,
    hero = hero.toDomain(),
    coach = coach.map { it.toDomain() },
    mastery = sections.mastery?.toDomain(),
    habits = sections.habits?.toDomain(),
    words = sections.words?.toDomain(),
    wordRush = sections.wordRush?.toDomain(),
    locked = locked.mapNotNull { it.toDomainOrNull() },
)

private fun HeroDto.toDomain() = WeeklyHero(
    headline = headline,
    reviews = reviews.toDomain(),
    accuracyPct = accuracyPct?.toDomain(),
    leveledUp = leveledUp.toDomain(),
    currentStreak = currentStreak,
    week = week.mapNotNull { it.toDomainOrNull() },
)

private fun MetricDto.toDomain() = Metric(value, previous)

private fun DayCountDto.toDomainOrNull(): DayCount? =
    LocalDate.Formats.ISO.parseOrNull(date)?.let { DayCount(it, reviews) }

fun CoachCardDto.toDomain() = CoachCard(
    id = id,
    type = type,
    title = title,
    body = body,
    words = words.map { WordChip(it.id, it.text) },
    moreWordsCount = moreWordsCount,
    action = action.toDomain(),
)

private fun CoachActionDto.toDomain(): CoachAction {
    val text = label.orEmpty()
    return when (kind) {
        "REVIEW_WORDS" -> if (wordIds.isEmpty()) CoachAction.None else CoachAction.ReviewWords(text, wordIds)
        "START_REVIEW" -> CoachAction.StartReview(text)
        "ENABLE_REMINDER" -> hour?.let { CoachAction.EnableReminder(text, it) } ?: CoachAction.None
        "START_WORD_RUSH" -> CoachAction.StartWordRush(text)
        else -> CoachAction.None
    }
}

private fun MasterySectionDto.toDomain() =
    MasterySection(caption, levels.map { LevelCount(it.level, it.words) }, promotedThisWeek, demotedThisWeek)

private fun HabitsSectionDto.toDomain() = HabitsSection(
    caption = caption,
    heatmap = heatmap.mapNotNull { it.toDomainOrNull() },
    bestHour = bestHour?.let { BestHour(it.hour, it.accuracyPct) },
    weekdays = weekdays.map { WeekdayAccuracy(it.isoDay, it.accuracyPct, it.reviews) },
)

private fun WordsSectionDto.toDomain() = WordsSection(
    caption = caption,
    hardest = hardest.map { InsightWord(it.id, it.text, it.accuracyPct) },
    comebacks = comebacks.map { WordChip(it.id, it.text) },
)

private fun WordRushSectionDto.toDomain() = WordRushSection(caption, gamesPlayed, bestScore, recentScores)

private fun LockedSectionDto.toDomainOrNull(): LockedSection? {
    val key = when (section) {
        "mastery" -> InsightSectionKey.MASTERY
        "habits" -> InsightSectionKey.HABITS
        "words" -> InsightSectionKey.WORDS
        "wordRush" -> InsightSectionKey.WORD_RUSH
        else -> return null
    }
    return LockedSection(key, reviewsNeeded)
}
```

- [ ] **Step 5: Run mapper test**

Run: `./gradlew composeApp:testDebugUnitTest --tests "data.insights.InsightsScreenMappersTest"`
Expected: PASS (5 tests).

- [ ] **Step 6: Write the failing data source test**

```kotlin
package data.insights

import core.common.Try
import data.core.network.client.ApiClient
import data.core.network.mapper.ApiResponseMapper
import data.insights.remote.InsightsScreenRemoteDataSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InsightsScreenRemoteDataSourceTest {

    private val json = Json { ignoreUnknownKeys = true }
    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun dataSource(engine: MockEngine) = InsightsScreenRemoteDataSource(
        ApiClient("https://api.test", HttpClient(engine) { install(ContentNegotiation) { json(json) } }, ApiResponseMapper())
    )

    private val payload = """{"success":true,"data":{
        "generatedAt":"2026-10-10T18:04:00Z","totalReviews":12,"futureField":true,
        "hero":{"headline":"Ahead of last week","reviews":{"value":12,"previous":5},"accuracyPct":null,
                "leveledUp":{"value":3,"previous":1},"currentStreak":2,"week":[{"date":"2026-10-05","reviews":12}]},
        "coach":[{"id":"NEW:2026-10-10","type":"NEW","priority":5,"title":"Hi","body":"b",
                  "action":{"kind":"SOMETHING_NEW"}}],
        "sections":{"mastery":null,"habits":null,"words":null,"wordRush":null},
        "locked":[{"section":"habits","reviewsNeeded":18}]}}"""

    @Test
    fun `sends tz and parses payload with unknown fields`() = runTest {
        var url = ""
        val engine = MockEngine { request ->
            url = request.url.toString()
            respond(payload, HttpStatusCode.OK, jsonHeaders())
        }

        val result = dataSource(engine).fetch("Europe/Berlin")

        assertTrue(url.endsWith("/analytics/insights-screen?tz=Europe%2FBerlin"), url)
        val dto = (result as Try.Success).value
        assertEquals(12, dto.totalReviews)
        assertEquals("SOMETHING_NEW", dto.coach.single().action.kind)
    }

    @Test
    fun `server error is a failure`() = runTest {
        val engine = MockEngine { respond("""{"success":false,"message":"boom"}""", HttpStatusCode.InternalServerError, jsonHeaders()) }
        assertTrue(dataSource(engine).fetch("UTC").isFailure)
    }
}
```

- [ ] **Step 7: Run test to verify it fails**

Run: `./gradlew composeApp:testDebugUnitTest --tests "data.insights.InsightsScreenRemoteDataSourceTest"`
Expected: FAIL — unresolved `InsightsScreenRemoteDataSource`.

- [ ] **Step 8: Implement** — `InsightsScreenRemoteDataSource.kt`:

```kotlin
package data.insights.remote

import core.common.Try
import data.core.network.client.ApiClient
import io.ktor.client.request.parameter

interface IInsightsScreenRemoteDataSource {
    suspend fun fetch(timeZoneId: String): Try<InsightsScreenDto>
}

class InsightsScreenRemoteDataSource(
    private val apiClient: ApiClient,
) : IInsightsScreenRemoteDataSource {
    override suspend fun fetch(timeZoneId: String): Try<InsightsScreenDto> =
        apiClient.getNotNull<InsightsScreenDto>("/analytics/insights-screen") { parameter("tz", timeZoneId) }
}
```

- [ ] **Step 9: Run test to verify it passes**

Run: `./gradlew composeApp:testDebugUnitTest --tests "data.insights.InsightsScreenRemoteDataSourceTest"`
Expected: PASS (2 tests).

- [ ] **Step 10: Commit**

```bash
git add data/src/commonMain/kotlin/data/insights composeApp/src/commonTest/kotlin/data/insights
git commit -m "feat(insights): add insights screen DTOs, mapper and remote data source"
```

---

### Task 5: SQLDelight cache + local data source

**Files:**
- Create: `data/src/commonMain/sqldelight/data/core/database/20.sqm`
- Modify: `data/src/commonMain/sqldelight/data/core/database/Lexicon.sq`
- Create: `data/src/commonMain/kotlin/data/insights/local/InsightsScreenLocalDataSource.kt`

- [ ] **Step 1: Confirm migration number**

Run: `ls data/src/commonMain/sqldelight/data/core/database/*.sqm | sort -V | tail -1`
Expected: `.../19.sqm` → new file is `20.sqm`. If a higher number exists, use the next one and keep the same contents.

- [ ] **Step 2: Write migration** — `20.sqm`:

```sql
-- Migration from version 20 to version 21
-- Insights coach: last server screen (raw JSON) and coach cards the user dismissed today.
CREATE TABLE IF NOT EXISTS InsightsScreenCache (
    id            INTEGER NOT NULL PRIMARY KEY DEFAULT 1,
    json          TEXT    NOT NULL,
    fetched_at_ms INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS DismissedCoachCard (
    card_id TEXT NOT NULL PRIMARY KEY
);
```

- [ ] **Step 3: Add tables + queries** — append to `Lexicon.sq`:

```sql
CREATE TABLE InsightsScreenCache (
    id            INTEGER NOT NULL PRIMARY KEY DEFAULT 1,
    json          TEXT    NOT NULL,
    fetched_at_ms INTEGER NOT NULL
);

getInsightsScreenCache:
SELECT json, fetched_at_ms FROM InsightsScreenCache WHERE id = 1;

saveInsightsScreenCache:
INSERT OR REPLACE INTO InsightsScreenCache(id, json, fetched_at_ms) VALUES (1, ?, ?);

clearInsightsScreenCache:
DELETE FROM InsightsScreenCache;

CREATE TABLE DismissedCoachCard (
    card_id TEXT NOT NULL PRIMARY KEY
);

selectDismissedCoachCards:
SELECT card_id FROM DismissedCoachCard;

insertDismissedCoachCard:
INSERT OR IGNORE INTO DismissedCoachCard(card_id) VALUES (?);

-- Card ids end with ":<local date>"; keep only today's.
pruneDismissedCoachCards:
DELETE FROM DismissedCoachCard WHERE card_id NOT LIKE '%:' || ?;

clearDismissedCoachCards:
DELETE FROM DismissedCoachCard;
```

- [ ] **Step 4: Write local data source**

```kotlin
package data.insights.local

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import data.core.database.LexiconQueries
import data.insights.remote.InsightsScreenDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

data class StoredInsightsScreen(val dto: InsightsScreenDto, val fetchedAtMs: Long)

interface IInsightsScreenLocalDataSource {
    fun observe(): Flow<StoredInsightsScreen?>
    suspend fun write(dto: InsightsScreenDto, fetchedAtMs: Long)
    fun observeDismissed(): Flow<Set<String>>
    /** Records [cardId] and drops ids from other days ([today] is ISO yyyy-MM-dd). */
    suspend fun dismiss(cardId: String, today: String)
    suspend fun clear()
}

class InsightsScreenLocalDataSource(
    private val queries: LexiconQueries,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : IInsightsScreenLocalDataSource {

    /** A row written by an older app version that no longer parses is treated as absent. */
    override fun observe(): Flow<StoredInsightsScreen?> =
        queries.getInsightsScreenCache().asFlow().mapToOneOrNull(dispatcher).map { row ->
            row?.let { decode(it.json)?.let { dto -> StoredInsightsScreen(dto, it.fetched_at_ms) } }
        }

    override suspend fun write(dto: InsightsScreenDto, fetchedAtMs: Long) {
        queries.saveInsightsScreenCache(json.encodeToString(InsightsScreenDto.serializer(), dto), fetchedAtMs)
    }

    override fun observeDismissed(): Flow<Set<String>> =
        queries.selectDismissedCoachCards().asFlow().mapToList(dispatcher).map { it.toSet() }

    override suspend fun dismiss(cardId: String, today: String) {
        queries.transaction {
            queries.pruneDismissedCoachCards(today)
            queries.insertDismissedCoachCard(cardId)
        }
    }

    override suspend fun clear() {
        queries.clearInsightsScreenCache()
        queries.clearDismissedCoachCards()
    }

    private fun decode(raw: String): InsightsScreenDto? =
        // SerializationException extends IllegalArgumentException.
        try {
            json.decodeFromString(InsightsScreenDto.serializer(), raw)
        } catch (_: IllegalArgumentException) {
            null
        }
}
```

Note: the `try/catch` mirrors `FeatureAccessLocalDataSourceImpl` (decode boundary, not control flow). If `queries.transaction { }` is `suspend` in the async driver setup, call it as `queries.transaction { ... }` inside the suspend function as written; check `grep -rn "queries.transaction" data/src` for the local idiom and match it.

- [ ] **Step 5: Compile (generates queries)**

Run: `./gradlew data:generateCommonMainLexiconDatabaseInterface composeApp:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL. (If the generate task name differs, `./gradlew data:tasks | grep -i generate` and use the listed one.)

- [ ] **Step 6: Verify migration**

Run: `./gradlew data:verifySqlDelightMigration`
Expected: BUILD SUCCESSFUL (or "no task" — then skip; the Android install in Task 13 exercises the migration).

- [ ] **Step 7: Commit**

```bash
git add data/src/commonMain/sqldelight data/src/commonMain/kotlin/data/insights/local
git commit -m "feat(insights): cache insights screen and dismissed coach cards locally"
```

---

### Task 6: Repositories + logout cleanup + DI

**Files:**
- Create: `data/src/commonMain/kotlin/data/insights/repository/InsightsScreenRepositoryImpl.kt`
- Create: `data/src/commonMain/kotlin/data/insights/repository/DismissedCoachCardsRepositoryImpl.kt`
- Modify: `data/src/commonMain/kotlin/data/auth/repository/AuthRepositoryImpl.kt`
- Modify: `composeApp/src/commonMain/kotlin/di/AnalyticsModule.kt`
- Test: `TEST/data/insights/InsightsScreenRepositoryImplTest.kt`

- [ ] **Step 1: Write the failing test** (fake data sources, no DB):

```kotlin
package data.insights

import app.cash.turbine.test
import core.common.Try
import data.insights.local.IInsightsScreenLocalDataSource
import data.insights.local.StoredInsightsScreen
import data.insights.remote.HeroDto
import data.insights.remote.IInsightsScreenRemoteDataSource
import data.insights.remote.InsightsScreenDto
import data.insights.repository.InsightsScreenRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InsightsScreenRepositoryImplTest {

    private class FakeLocal : IInsightsScreenLocalDataSource {
        val stored = MutableStateFlow<StoredInsightsScreen?>(null)
        override fun observe(): Flow<StoredInsightsScreen?> = stored
        override suspend fun write(dto: InsightsScreenDto, fetchedAtMs: Long) { stored.value = StoredInsightsScreen(dto, fetchedAtMs) }
        override fun observeDismissed(): Flow<Set<String>> = MutableStateFlow(emptySet())
        override suspend fun dismiss(cardId: String, today: String) = Unit
        override suspend fun clear() { stored.value = null }
    }

    private class FakeRemote(var result: Try<InsightsScreenDto>) : IInsightsScreenRemoteDataSource {
        override suspend fun fetch(timeZoneId: String) = result
    }

    private val dto = InsightsScreenDto(totalReviews = 7, hero = HeroDto(headline = "Hi"))

    @Test
    fun `successful refresh writes cache with fetch time`() = runTest {
        val local = FakeLocal()
        val repo = InsightsScreenRepositoryImpl(FakeRemote(Try.success(dto)), local, nowMs = { 42L })

        repo.observe().test {
            assertNull(awaitItem())
            assertTrue(repo.refresh("UTC").isSuccess)
            val cached = awaitItem()
            assertEquals(7, cached?.screen?.totalReviews)
            assertEquals(42L, cached?.fetchedAtMs)
        }
    }

    @Test
    fun `failed refresh keeps the cache`() = runTest {
        val local = FakeLocal().apply { stored.value = StoredInsightsScreen(dto, 1L) }
        val repo = InsightsScreenRepositoryImpl(FakeRemote(Try.failure(Exception("offline"))), local, nowMs = { 99L })

        assertTrue(repo.refresh("UTC").isFailure)
        repo.observe().test { assertEquals(1L, awaitItem()?.fetchedAtMs) }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew composeApp:testDebugUnitTest --tests "data.insights.InsightsScreenRepositoryImplTest"`
Expected: FAIL — unresolved `InsightsScreenRepositoryImpl`.

- [ ] **Step 3: Implement**

```kotlin
package data.insights.repository

import core.common.Try
import core.common.map
import data.insights.local.IInsightsScreenLocalDataSource
import data.insights.remote.IInsightsScreenRemoteDataSource
import data.insights.toDomain
import domain.insights.model.CachedInsights
import domain.insights.repository.IInsightsScreenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class InsightsScreenRepositoryImpl(
    private val remote: IInsightsScreenRemoteDataSource,
    private val local: IInsightsScreenLocalDataSource,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : IInsightsScreenRepository {

    override fun observe(): Flow<CachedInsights?> =
        local.observe().map { stored -> stored?.let { CachedInsights(it.dto.toDomain(), it.fetchedAtMs) } }

    override suspend fun refresh(timeZoneId: String): Try<Unit> =
        remote.fetch(timeZoneId).map { dto -> local.write(dto, nowMs()) }
}
```

```kotlin
package data.insights.repository

import core.common.Try
import data.insights.local.IInsightsScreenLocalDataSource
import domain.insights.repository.IDismissedCoachCardsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class DismissedCoachCardsRepositoryImpl(
    private val local: IInsightsScreenLocalDataSource,
    private val today: () -> String = { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString() },
) : IDismissedCoachCardsRepository {

    override fun observe(): Flow<Set<String>> = local.observeDismissed()

    override suspend fun dismiss(cardId: String): Try<Unit> = Try { local.dismiss(cardId, today()) }
}
```

(If `core.common.map` on `Try` takes a non-suspend lambda, use `flatMap { dto -> Try { local.write(dto, nowMs()) } }` — check the signature in `core/src/commonMain/kotlin/core/common/Try.kt`. `Try { }` is the existing builder used by `LoadReviewQueueUseCase`.)

- [ ] **Step 4: Clear on logout/delete** — in `AuthRepositoryImpl`:

Run first: `grep -rn "AuthRepositoryImpl(" --include='*.kt' . | grep -v /build/`
Add constructor param `private val insightsScreenLocal: IInsightsScreenLocalDataSource`, call `insightsScreenLocal.clear()` right after `featureAccessRemoteDataSource.clearCache()` in `logout()` and at the equivalent point in `deleteAccount()`. Update every construction site found by the grep (DI + tests; tests pass a no-op fake implementing `IInsightsScreenLocalDataSource`, e.g. the `FakeLocal` above moved to `test/src/commonMain/kotlin/fakes/FakeInsightsScreenLocalDataSource.kt`).

- [ ] **Step 5: DI** — in `composeApp/src/commonMain/kotlin/di/AnalyticsModule.kt`:

```kotlin
    single<IInsightsScreenRemoteDataSource> { InsightsScreenRemoteDataSource(apiClient = get()) }
    single<IInsightsScreenLocalDataSource> { InsightsScreenLocalDataSource(queries = get()) }
    single<IInsightsScreenRepository> { InsightsScreenRepositoryImpl(remote = get(), local = get()) }
    single<IDismissedCoachCardsRepository> { DismissedCoachCardsRepositoryImpl(local = get()) }
    factoryOf(::ObserveInsightsScreenUseCase)
    factoryOf(::RefreshInsightsScreenUseCase)
    factoryOf(::ObserveDismissedCoachCardsUseCase)
    factoryOf(::DismissCoachCardUseCase)
```

(Match how `LexiconQueries` is provided: `grep -n "LexiconQueries" composeApp/src/commonMain/kotlin/di/*.kt` and use the same `get()` form.)

- [ ] **Step 6: Run tests + compile + Koin check**

Run: `./gradlew composeApp:testDebugUnitTest --tests "data.insights.*" --tests "*AuthRepository*" && ./gradlew composeApp:compileKotlinMetadata`
Expected: PASS, BUILD SUCCESSFUL. If a Koin module verification test exists (`grep -rln "verify()" composeApp/src/*Test`), run it too.

- [ ] **Step 7: Commit**

```bash
git add data/src/commonMain/kotlin/data/insights data/src/commonMain/kotlin/data/auth/repository/AuthRepositoryImpl.kt composeApp/src/commonMain/kotlin/di/AnalyticsModule.kt composeApp/src/commonTest test/src/commonMain/kotlin/fakes
git commit -m "feat(insights): add cached insights screen repositories"
```

---

### Task 7: Platform hour format

**Files:**
- Create: `platforms/src/commonMain/kotlin/time/HourFormat.kt`
- Create: `platforms/src/androidMain/kotlin/time/HourFormat.android.kt`
- Create: `platforms/src/iosMain/kotlin/time/HourFormat.ios.kt`
- Create: `platforms/src/wasmJsMain/kotlin/time/HourFormat.wasmJs.kt`

No unit test (thin platform bridge); exercised on emulator in Task 13.

- [ ] **Step 1: Write expect + actuals**

```kotlin
// commonMain
package time

/** True when the device shows times as 24-hour ("20:00"), false for 12-hour ("8 PM"). */
expect fun is24HourClock(): Boolean
```

```kotlin
// androidMain
package time

import java.text.DateFormat
import java.text.SimpleDateFormat

actual fun is24HourClock(): Boolean =
    (DateFormat.getTimeInstance(DateFormat.SHORT) as? SimpleDateFormat)?.toPattern()?.contains('H') ?: true
```

```kotlin
// iosMain
package time

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

actual fun is24HourClock(): Boolean =
    NSDateFormatter.dateFormatFromTemplate("j", 0u, NSLocale.currentLocale)?.contains('a') != true
```

```kotlin
// wasmJsMain
package time

actual fun is24HourClock(): Boolean = true
```

- [ ] **Step 2: Compile all targets**

Run: `./gradlew platforms:compileKotlinMetadata platforms:compileDebugKotlinAndroid platforms:compileKotlinIosSimulatorArm64`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add platforms/src/*/kotlin/time
git commit -m "feat(platforms): expose device 12/24-hour clock preference"
```

---

### Task 8: Formatter (display rules)

**Files:**
- Create: `feature/insights/src/commonMain/kotlin/feature/insights/coach/InsightsFormatter.kt`
- Test: `TEST/feature/insights/coach/InsightsFormatterTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package feature.insights.coach

import kotlin.test.Test
import kotlin.test.assertEquals

class InsightsFormatterTest {

    @Test
    fun `compact counts`() {
        assertEquals("999", InsightsFormatter.compactCount(999))
        assertEquals("1k", InsightsFormatter.compactCount(1000))
        assertEquals("1.2k", InsightsFormatter.compactCount(1249))
        assertEquals("12.5k", InsightsFormatter.compactCount(12_500))
    }

    @Test
    fun `hours follow the clock preference`() {
        assertEquals("20:00", InsightsFormatter.hour(20, use24Hour = true))
        assertEquals("08:00", InsightsFormatter.hour(8, use24Hour = true))
        assertEquals("8 PM", InsightsFormatter.hour(20, use24Hour = false))
        assertEquals("12 AM", InsightsFormatter.hour(0, use24Hour = false))
        assertEquals("12 PM", InsightsFormatter.hour(12, use24Hour = false))
    }

    @Test
    fun `updated ago buckets`() {
        assertEquals(UpdatedAgo.JustNow, InsightsFormatter.updatedAgo(fetchedAtMs = 0, nowMs = 59_000))
        assertEquals(UpdatedAgo.Minutes(5), InsightsFormatter.updatedAgo(0, 5 * 60_000))
        assertEquals(UpdatedAgo.Hours(2), InsightsFormatter.updatedAgo(0, 2 * 3_600_000 + 10))
        assertEquals(UpdatedAgo.Days(3), InsightsFormatter.updatedAgo(0, 3 * 86_400_000L))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew composeApp:testDebugUnitTest --tests "feature.insights.coach.InsightsFormatterTest"`
Expected: FAIL — unresolved `InsightsFormatter`.

(If `composeApp` tests can't see `feature/insights` internals, make the formatter `public` — feature test sources live in composeApp per existing `WordRushViewModelTest` location.)

- [ ] **Step 3: Implement**

```kotlin
package feature.insights.coach

sealed interface UpdatedAgo {
    data object JustNow : UpdatedAgo
    data class Minutes(val value: Int) : UpdatedAgo
    data class Hours(val value: Int) : UpdatedAgo
    data class Days(val value: Int) : UpdatedAgo
}

/** Pure, locale-light formatting for insights numbers (spec §6). Words come from string resources. */
object InsightsFormatter {

    private const val THOUSAND = 1000
    private const val MINUTE_MS = 60_000L
    private const val HOUR_MS = 60 * MINUTE_MS
    private const val DAY_MS = 24 * HOUR_MS
    private const val HOURS_PER_HALF_DAY = 12

    fun compactCount(value: Long): String {
        if (value < THOUSAND) return value.toString()
        val tenths = value * 10 / THOUSAND
        val whole = tenths / 10
        val decimal = tenths % 10
        return if (decimal == 0L) "${whole}k" else "$whole.${decimal}k"
    }

    fun compactCount(value: Int): String = compactCount(value.toLong())

    fun hour(hour: Int, use24Hour: Boolean): String {
        if (use24Hour) return "${hour.toString().padStart(2, '0')}:00"
        val display = (hour % HOURS_PER_HALF_DAY).let { if (it == 0) HOURS_PER_HALF_DAY else it }
        return "$display ${if (hour < HOURS_PER_HALF_DAY) "AM" else "PM"}"
    }

    fun updatedAgo(fetchedAtMs: Long, nowMs: Long): UpdatedAgo {
        val elapsed = (nowMs - fetchedAtMs).coerceAtLeast(0)
        return when {
            elapsed < MINUTE_MS -> UpdatedAgo.JustNow
            elapsed < HOUR_MS -> UpdatedAgo.Minutes((elapsed / MINUTE_MS).toInt())
            elapsed < DAY_MS -> UpdatedAgo.Hours((elapsed / HOUR_MS).toInt())
            else -> UpdatedAgo.Days((elapsed / DAY_MS).toInt())
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew composeApp:testDebugUnitTest --tests "feature.insights.coach.InsightsFormatterTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add feature/insights/src/commonMain/kotlin/feature/insights/coach/InsightsFormatter.kt composeApp/src/commonTest/kotlin/feature/insights/coach
git commit -m "feat(insights): add friendly number, hour and freshness formatting"
```

---

### Task 9: UI models + mapper

**Files:**
- Create: `feature/insights/src/commonMain/kotlin/feature/insights/coach/model/InsightsUiModel.kt`
- Create: `feature/insights/src/commonMain/kotlin/feature/insights/coach/InsightsUiMapper.kt`
- Test: `TEST/feature/insights/coach/InsightsUiMapperTest.kt`

- [ ] **Step 1: Write UI models**

```kotlin
package feature.insights.coach.model

import androidx.compose.runtime.Immutable
import domain.insights.model.CoachAction
import domain.insights.model.InsightWord
import domain.insights.model.WordChip

enum class Trend { UP, DOWN, FLAT }
enum class ChangeUnit { COUNT, POINTS }

/** A big number with its change vs last week; the UI renders arrow + sign + words (never color alone). */
@Immutable
data class StatUi(val value: String, val trend: Trend, val changeAmount: Int, val unit: ChangeUnit)

@Immutable
data class DayDotUi(val isoDay: Int, val reviews: Int, val isToday: Boolean, val isFuture: Boolean)

@Immutable
data class HeroUi(
    val headline: String,
    val reviews: StatUi,
    /** Null below the sample gate: the tile shows "—" with a hint instead of a misleading %. */
    val accuracy: StatUi?,
    val leveledUp: StatUi,
    val streak: Int,
    val week: List<DayDotUi>,
)

@Immutable
data class CoachCardUi(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val words: List<WordChip>,
    val moreWordsCount: Int,
    val action: CoachAction,
    /** Pre-formatted hour for ENABLE_REMINDER cards ("8 PM" / "20:00"). */
    val hourLabel: String?,
)

enum class SectionKind { MASTERY, HABITS, WORDS, WORD_RUSH }

@Immutable
data class LevelBarUi(val level: Int, val words: Long, val fraction: Float)

@Immutable
data class HeatCellUi(val epochDay: Long, val reviews: Int, val intensity: Int)

@Immutable
data class WeekdayBarUi(val isoDay: Int, val accuracyPct: Int, val isBest: Boolean)

@Immutable
sealed interface DeepDiveUi {
    val kind: SectionKind
    val caption: String

    data class Mastery(override val caption: String, val levels: List<LevelBarUi>, val promoted: Int, val demoted: Int) : DeepDiveUi {
        override val kind = SectionKind.MASTERY
    }

    data class Habits(
        override val caption: String,
        val heatmap: List<HeatCellUi>,
        val bestHourLabel: String?,
        val bestHourAccuracy: Int?,
        val weekdays: List<WeekdayBarUi>,
    ) : DeepDiveUi {
        override val kind = SectionKind.HABITS
    }

    data class Words(override val caption: String, val hardest: List<InsightWord>, val comebacks: List<WordChip>) : DeepDiveUi {
        override val kind = SectionKind.WORDS
    }

    data class WordRush(override val caption: String, val gamesPlayed: String, val bestScore: String, val recentScores: List<Int>) : DeepDiveUi {
        override val kind = SectionKind.WORD_RUSH
    }

    /** Not enough data yet: teaser with progress instead of an empty chart. */
    data class Locked(override val kind: SectionKind, val reviewsNeeded: Int, val progress: Float) : DeepDiveUi {
        override val caption: String = ""
    }
}

@Immutable
data class InsightsUiModel(
    val hero: HeroUi,
    val coach: List<CoachCardUi>,
    val sections: List<DeepDiveUi>,
    val isNewUser: Boolean,
)
```

- [ ] **Step 2: Write the failing test**

```kotlin
package feature.insights.coach

import domain.insights.InsightsFixtures
import domain.insights.model.*
import feature.insights.coach.model.*
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InsightsUiMapperTest {

    private val wednesday = LocalDate(2026, 10, 7)
    private fun map(screen: InsightsScreen = InsightsFixtures.screen(), dismissed: Set<String> = emptySet(), use24 : Boolean = false) =
        InsightsUiMapper.map(screen, dismissed, today = wednesday, use24Hour = use24)

    @Test
    fun `hero stats carry trend and change`() {
        val hero = map().hero
        assertEquals(StatUi("212", Trend.UP, 18, ChangeUnit.COUNT), hero.reviews)
        assertEquals(StatUi("84%", Trend.UP, 5, ChangeUnit.POINTS), hero.accuracy)
        assertEquals(StatUi("31", Trend.UP, 9, ChangeUnit.COUNT), hero.leveledUp)
    }

    @Test
    fun `hero accuracy null below sample gate and flat when unchanged`() {
        val hero = map(InsightsFixtures.screen(hero = InsightsFixtures.hero(accuracy = null, reviews = Metric(5, 5)))).hero
        assertNull(hero.accuracy)
        assertEquals(Trend.FLAT, hero.reviews.trend)
    }

    @Test
    fun `large counts are compact`() {
        val hero = map(InsightsFixtures.screen(hero = InsightsFixtures.hero(reviews = Metric(1249, 2000)))).hero
        assertEquals(StatUi("1.2k", Trend.DOWN, 751, ChangeUnit.COUNT), hero.reviews)
    }

    @Test
    fun `week dots mark today and future`() {
        val week = map().hero.week
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), week.map { it.isoDay })
        assertTrue(week[2].isToday)
        assertTrue(week[3].isFuture && !week[1].isFuture)
    }

    @Test
    fun `dismissed coach cards are filtered`() {
        val card = InsightsFixtures.card()
        assertTrue(map(dismissed = setOf(card.id)).coach.isEmpty())
    }

    @Test
    fun `reminder card gets a localized hour label`() {
        val card = InsightsFixtures.card(type = "BEST_TIME", action = CoachAction.EnableReminder("Remind me", 20))
        assertEquals("8 PM", map(InsightsFixtures.screen(coach = listOf(card))).coach.single().hourLabel)
        assertEquals("20:00", map(InsightsFixtures.screen(coach = listOf(card)), use24 = true).coach.single().hourLabel)
    }

    @Test
    fun `sections keep fixed order and locked ones become teasers`() {
        val sections = map().sections
        assertEquals(listOf(SectionKind.MASTERY, SectionKind.HABITS, SectionKind.WORDS), sections.map { it.kind })
        val locked = sections[1] as DeepDiveUi.Locked
        assertEquals(18, locked.reviewsNeeded)
        assertEquals(0.4f, locked.progress, 0.01f) // (30 - 18) / 30
    }

    @Test
    fun `mastery bars are scaled to the largest stage`() {
        val mastery = map().sections.first() as DeepDiveUi.Mastery
        assertEquals(1f, mastery.levels.last().fraction)
        assertEquals(0f, mastery.levels.first().fraction)
    }

    @Test
    fun `habits mark the best weekday and bucket heat`() {
        val habits = HabitsSection(
            caption = "You're sharpest on Tuesdays",
            heatmap = listOf(DayCount(LocalDate(2026, 10, 1), 2), DayCount(LocalDate(2026, 10, 2), 40)),
            bestHour = BestHour(20, 91),
            weekdays = listOf(WeekdayAccuracy(1, 70, 40), WeekdayAccuracy(2, 88, 40)),
        )
        val ui = map(InsightsFixtures.screen(habits = habits, locked = emptyList())).sections
            .filterIsInstance<DeepDiveUi.Habits>().single()
        assertEquals("8 PM", ui.bestHourLabel)
        assertEquals(listOf(false, true), ui.weekdays.map { it.isBest })
        assertEquals(listOf(1, 4), ui.heatmap.map { it.intensity })
    }

    @Test
    fun `new user flag when no reviews at all`() {
        assertTrue(map(InsightsFixtures.screen().copy(totalReviews = 0)).isNewUser)
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew composeApp:testDebugUnitTest --tests "feature.insights.coach.InsightsUiMapperTest"`
Expected: FAIL — unresolved `InsightsUiMapper`.

- [ ] **Step 4: Implement**

```kotlin
package feature.insights.coach

import domain.insights.model.*
import feature.insights.coach.model.*
import kotlinx.datetime.LocalDate

/** Domain → UI. Every spec §6 display rule that is not wording lives here. */
object InsightsUiMapper {

    /** Must match the server's MIN_REVIEWS_PER_BUCKET (habits unlock threshold). */
    private const val HABITS_UNLOCK_REVIEWS = 30
    private const val HEAT_LEVELS = 4

    fun map(screen: InsightsScreen, dismissed: Set<String>, today: LocalDate, use24Hour: Boolean) = InsightsUiModel(
        hero = screen.hero.toUi(today),
        coach = screen.coach.filterNot { it.id in dismissed }.map { it.toUi(use24Hour) },
        sections = sections(screen, use24Hour),
        isNewUser = screen.totalReviews == 0L,
    )

    private fun WeeklyHero.toUi(today: LocalDate) = HeroUi(
        headline = headline,
        reviews = reviews.toStat(InsightsFormatter.compactCount(reviews.value), ChangeUnit.COUNT),
        accuracy = accuracyPct?.let { it.toStat("${it.value}%", ChangeUnit.POINTS) },
        leveledUp = leveledUp.toStat(InsightsFormatter.compactCount(leveledUp.value), ChangeUnit.COUNT),
        streak = currentStreak,
        week = week.map { day ->
            DayDotUi(
                isoDay = day.date.dayOfWeek.ordinal + 1,
                reviews = day.reviews,
                isToday = day.date == today,
                isFuture = day.date > today,
            )
        },
    )

    private fun Metric.toStat(display: String, unit: ChangeUnit) = StatUi(
        value = display,
        trend = when {
            change > 0 -> Trend.UP
            change < 0 -> Trend.DOWN
            else -> Trend.FLAT
        },
        changeAmount = kotlin.math.abs(change),
        unit = unit,
    )

    private fun CoachCard.toUi(use24Hour: Boolean) = CoachCardUi(
        id = id,
        type = type,
        title = title,
        body = body,
        words = words,
        moreWordsCount = moreWordsCount,
        action = action,
        hourLabel = (action as? CoachAction.EnableReminder)?.let { InsightsFormatter.hour(it.hour, use24Hour) },
    )

    private fun sections(screen: InsightsScreen, use24Hour: Boolean): List<DeepDiveUi> {
        val locked = screen.locked.associateBy { it.section }
        return listOfNotNull(
            screen.mastery?.toUi() ?: locked[InsightSectionKey.MASTERY]?.toUi(SectionKind.MASTERY),
            screen.habits?.toUi(use24Hour) ?: locked[InsightSectionKey.HABITS]?.toUi(SectionKind.HABITS),
            screen.words?.toUi() ?: locked[InsightSectionKey.WORDS]?.toUi(SectionKind.WORDS),
            screen.wordRush?.toUi() ?: locked[InsightSectionKey.WORD_RUSH]?.toUi(SectionKind.WORD_RUSH),
        )
    }

    private fun LockedSection.toUi(kind: SectionKind) = DeepDiveUi.Locked(
        kind = kind,
        reviewsNeeded = reviewsNeeded,
        progress = ((HABITS_UNLOCK_REVIEWS - reviewsNeeded).toFloat() / HABITS_UNLOCK_REVIEWS).coerceIn(0f, 1f),
    )

    private fun MasterySection.toUi(): DeepDiveUi.Mastery {
        val max = levels.maxOfOrNull { it.words }?.takeIf { it > 0 } ?: 1L
        return DeepDiveUi.Mastery(
            caption = caption,
            levels = levels.map { LevelBarUi(it.level, it.words, it.words.toFloat() / max) },
            promoted = promotedThisWeek,
            demoted = demotedThisWeek,
        )
    }

    private fun HabitsSection.toUi(use24Hour: Boolean): DeepDiveUi.Habits {
        val maxReviews = heatmap.maxOfOrNull { it.reviews }?.takeIf { it > 0 } ?: 1
        val bestDay = weekdays.maxByOrNull { it.accuracyPct }?.isoDay
        return DeepDiveUi.Habits(
            caption = caption,
            heatmap = heatmap.map { day ->
                val intensity = ((day.reviews.toFloat() / maxReviews) * HEAT_LEVELS).toInt().coerceIn(1, HEAT_LEVELS)
                HeatCellUi(day.date.toEpochDays().toLong(), day.reviews, intensity)
            },
            bestHourLabel = bestHour?.let { InsightsFormatter.hour(it.hour, use24Hour) },
            bestHourAccuracy = bestHour?.accuracyPct,
            weekdays = weekdays.map { WeekdayBarUi(it.isoDay, it.accuracyPct, it.isoDay == bestDay) },
        )
    }

    private fun WordsSection.toUi() = DeepDiveUi.Words(caption, hardest, comebacks)

    private fun WordRushSection.toUi() = DeepDiveUi.WordRush(
        caption = caption,
        gamesPlayed = InsightsFormatter.compactCount(gamesPlayed),
        bestScore = InsightsFormatter.compactCount(bestScore),
        recentScores = recentScores,
    )
}
```

Note: `LocalDate.toEpochDays()` returns `Long` in kotlinx-datetime 0.7 (Int in older); keep `.toLong()` — harmless either way.

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew composeApp:testDebugUnitTest --tests "feature.insights.coach.InsightsUiMapperTest"`
Expected: PASS (10 tests).

- [ ] **Step 6: Commit**

```bash
git add feature/insights/src/commonMain/kotlin/feature/insights/coach composeApp/src/commonTest/kotlin/feature/insights/coach
git commit -m "feat(insights): map coach screen to user-friendly UI models"
```

---

### Task 10: ViewModel

**Files:**
- Create: `feature/insights/src/commonMain/kotlin/feature/insights/coach/InsightsCoachViewModel.kt`
- Test: `TEST/feature/insights/coach/InsightsCoachViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package feature.insights.coach

import app.cash.turbine.test
import domain.insights.InsightsFixtures
import domain.insights.model.CoachAction
import domain.insights.usecase.*
import domain.settings.usecase.SetReviewRemindersEnabledUseCase
import domain.word.model.ReviewSource
import fakes.FakeAnalyticsTracker
import fakes.FakeDismissedCoachCardsRepository
import fakes.FakeInsightsScreenRepository
import fakes.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsCoachViewModelTest {

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private val screens = FakeInsightsScreenRepository()
    private val dismissed = FakeDismissedCoachCardsRepository()
    private val settings = FakeSettingsRepository()
    private val tracker = FakeAnalyticsTracker()

    private fun viewModel() = InsightsCoachViewModel(
        useCases = InsightsCoachUseCases(
            observeScreen = ObserveInsightsScreenUseCase(screens),
            refreshScreen = RefreshInsightsScreenUseCase(screens),
            observeDismissed = ObserveDismissedCoachCardsUseCase(dismissed),
            dismissCard = DismissCoachCardUseCase(dismissed),
            setReviewReminders = SetReviewRemindersEnabledUseCase(settings),
        ),
        analyticsTracker = tracker,
        clock = InsightsClock(timeZoneId = { "Europe/Berlin" }, nowMs = { 61_000L }, today = { LocalDate(2026, 10, 7) }),
        use24Hour = { false },
    )

    @Test
    fun `no cache then refresh success shows content`() = runTest {
        screens.nextRefresh = InsightsFixtures.cached(fetchedAtMs = 1_000L)
        val vm = viewModel()
        vm.refresh()

        val state = vm.currentState
        assertNotNull(state.content)
        assertFalse(state.isRefreshing)
        assertNull(state.error)
        assertEquals(listOf("Europe/Berlin"), screens.refreshedZones)
        assertEquals(UpdatedAgo.Minutes(1), state.updatedAgo)
    }

    @Test
    fun `refresh failure with cache keeps content and no error`() = runTest {
        screens.cache.value = InsightsFixtures.cached()
        screens.refreshError = Exception("offline")
        val vm = viewModel()
        vm.refresh()

        assertNotNull(vm.currentState.content)
        assertNull(vm.currentState.error)
        assertTrue(vm.currentState.isStale)
    }

    @Test
    fun `refresh failure without cache shows error`() = runTest {
        screens.refreshError = Exception("offline")
        val vm = viewModel()
        vm.refresh()

        assertNull(vm.currentState.content)
        assertNotNull(vm.currentState.error)
    }

    @Test
    fun `review words action emits review effect and tracks it`() = runTest {
        screens.cache.value = InsightsFixtures.cached()
        val vm = viewModel()
        val card = vm.currentState.content!!.coach.single()

        vm.effects.test {
            vm.onCoachAction(card)
            assertEquals(InsightsCoachEffect.StartReview(ReviewSource.ByWords(listOf(1, 2, 3))), awaitItem())
        }
        assertTrue(tracker.events.any { it.first == "coach_card_action" && it.second["type"] == "SLIPPING_WORDS" })
    }

    @Test
    fun `enable reminder action turns reminders on`() = runTest {
        val reminderCard = InsightsFixtures.card(type = "BEST_TIME", action = CoachAction.EnableReminder("Remind me", 20))
        screens.cache.value = InsightsFixtures.cached(InsightsFixtures.screen(coach = listOf(reminderCard)))
        val vm = viewModel()

        vm.effects.test {
            vm.onCoachAction(vm.currentState.content!!.coach.single())
            assertEquals(InsightsCoachEffect.ReminderEnabled, awaitItem())
        }
        assertTrue(settings.reviewRemindersEnabled)
    }

    @Test
    fun `dismiss hides the card`() = runTest {
        screens.cache.value = InsightsFixtures.cached()
        val vm = viewModel()
        vm.dismissCard(vm.currentState.content!!.coach.single())
        assertTrue(vm.currentState.content!!.coach.isEmpty())
    }

    @Test
    fun `toggle section expands and collapses`() = runTest {
        val vm = viewModel()
        vm.toggleSection(feature.insights.coach.model.SectionKind.HABITS)
        assertEquals(setOf(feature.insights.coach.model.SectionKind.HABITS), vm.currentState.expanded)
        vm.toggleSection(feature.insights.coach.model.SectionKind.HABITS)
        assertTrue(vm.currentState.expanded.isEmpty())
    }
}
```

Before running, check the shared fakes' real APIs and adapt names in this test only (not production):
`grep -n "class FakeAnalyticsTracker" -A15 test/src/commonMain/kotlin/fakes/FakeAnalyticsTracker.kt` (recorded events property),
`grep -n "reviewReminders" test/src/commonMain/kotlin/fakes/FakeSettingsRepository.kt`,
`grep -n "class SetReviewRemindersEnabledUseCase" -A8 -r domain/src` (constructor deps).

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew composeApp:testDebugUnitTest --tests "feature.insights.coach.InsightsCoachViewModelTest"`
Expected: FAIL — unresolved `InsightsCoachViewModel`.

- [ ] **Step 3: Implement**

```kotlin
package feature.insights.coach

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.error.toUserMessage
import domain.insights.model.CachedInsights
import domain.insights.model.CoachAction
import domain.insights.usecase.DismissCoachCardUseCase
import domain.insights.usecase.ObserveDismissedCoachCardsUseCase
import domain.insights.usecase.ObserveInsightsScreenUseCase
import domain.insights.usecase.RefreshInsightsScreenUseCase
import domain.settings.usecase.SetReviewRemindersEnabledUseCase
import domain.word.model.ReviewSource
import feature.insights.coach.model.CoachCardUi
import feature.insights.coach.model.InsightsUiModel
import feature.insights.coach.model.SectionKind
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class InsightsCoachState(
    val content: InsightsUiModel? = null,
    val isRefreshing: Boolean = false,
    /** Set only when there is nothing cached to show. */
    val error: String? = null,
    /** Cached content shown after a failed refresh. */
    val isStale: Boolean = false,
    val updatedAgo: UpdatedAgo? = null,
    val expanded: Set<SectionKind> = emptySet(),
)

sealed interface InsightsCoachEffect {
    data class StartReview(val source: ReviewSource) : InsightsCoachEffect
    data object StartWordRush : InsightsCoachEffect
    data object ReminderEnabled : InsightsCoachEffect
    data object OpenNotificationSettings : InsightsCoachEffect
}

class InsightsCoachUseCases(
    val observeScreen: ObserveInsightsScreenUseCase,
    val refreshScreen: RefreshInsightsScreenUseCase,
    val observeDismissed: ObserveDismissedCoachCardsUseCase,
    val dismissCard: DismissCoachCardUseCase,
    val setReviewReminders: SetReviewRemindersEnabledUseCase,
)

/** Time sources, injectable for tests. */
class InsightsClock(
    val timeZoneId: () -> String = { TimeZone.currentSystemDefault().id },
    val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    val today: () -> LocalDate = { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date },
)

class InsightsCoachViewModel(
    private val useCases: InsightsCoachUseCases,
    private val analyticsTracker: IAnalyticsTracker,
    private val clock: InsightsClock = InsightsClock(),
    private val use24Hour: () -> Boolean,
) : BaseViewModel<InsightsCoachState, InsightsCoachEffect>() {

    private val trackedShown = mutableSetOf<String>()

    override fun initialState() = InsightsCoachState()

    init {
        combine(useCases.observeScreen(Unit), useCases.observeDismissed(Unit)) { cached, dismissed -> cached to dismissed }
            .onEach { (cached, dismissed) -> render(cached, dismissed) }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        if (currentState.isRefreshing) return
        viewModelScope.launch {
            updateState { copy(isRefreshing = true) }
            useCases.refreshScreen(clock.timeZoneId()).reduce(
                onSuccess = { copy(isRefreshing = false, error = null, isStale = false) },
                onFailure = { error ->
                    if (content == null) copy(isRefreshing = false, error = error.toUserMessage())
                    else copy(isRefreshing = false, isStale = true)
                },
            )
        }
        analyticsTracker.logEvent("insights_viewed", emptyMap())
    }

    fun onCoachAction(card: CoachCardUi) {
        analyticsTracker.logEvent("coach_card_action", mapOf("type" to card.type))
        when (val action = card.action) {
            is CoachAction.ReviewWords -> emitEffect(InsightsCoachEffect.StartReview(ReviewSource.ByWords(action.wordIds)))
            is CoachAction.StartReview -> emitEffect(InsightsCoachEffect.StartReview(ReviewSource.DueCards))
            is CoachAction.StartWordRush -> emitEffect(InsightsCoachEffect.StartWordRush)
            is CoachAction.EnableReminder -> enableReminders()
            CoachAction.None -> Unit
        }
    }

    fun dismissCard(card: CoachCardUi) {
        analyticsTracker.logEvent("coach_card_dismissed", mapOf("type" to card.type))
        viewModelScope.launch { useCases.dismissCard(card.id) }
    }

    fun toggleSection(kind: SectionKind) {
        updateState { copy(expanded = if (kind in expanded) expanded - kind else expanded + kind) }
    }

    private fun enableReminders() {
        viewModelScope.launch {
            useCases.setReviewReminders(true).reduce(
                onSuccess = { also { emitEffect(InsightsCoachEffect.ReminderEnabled) } },
                onFailure = { also { emitEffect(InsightsCoachEffect.OpenNotificationSettings) } },
            )
        }
    }

    private fun render(cached: CachedInsights?, dismissed: Set<String>) {
        if (cached == null) return
        val content = InsightsUiMapper.map(cached.screen, dismissed, clock.today(), use24Hour())
        content.coach.filter { trackedShown.add(it.id) }
            .forEach { analyticsTracker.logEvent("coach_card_shown", mapOf("type" to it.type)) }
        updateState {
            copy(
                content = content,
                error = null,
                updatedAgo = InsightsFormatter.updatedAgo(cached.fetchedAtMs, clock.nowMs()),
            )
        }
    }
}
```

Check before compiling: `IAnalyticsTracker.logEvent` signature (`grep -n "fun logEvent" -r platforms/src/commonMain domain/src/commonMain`) and the `Try.reduce` signature in `BaseViewModel` (lines 56–70). If `SetReviewRemindersEnabledUseCase` is invoked differently (e.g. `invoke(enabled: Boolean)` vs `invoke(params)`), match the call in the existing `InsightsViewModel.setReminder`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew composeApp:testDebugUnitTest --tests "feature.insights.coach.InsightsCoachViewModelTest"`
Expected: PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add feature/insights/src/commonMain/kotlin/feature/insights/coach/InsightsCoachViewModel.kt composeApp/src/commonTest/kotlin/feature/insights/coach/InsightsCoachViewModelTest.kt
git commit -m "feat(insights): add coach view model with cached refresh and actions"
```

---

### Task 11: Strings

**Files:**
- Modify: `resources/src/commonMain/composeResources/values/strings.xml`

- [ ] **Step 1: Add strings** (inside `<resources>`, near other `insights_` keys):

```xml
    <!-- Insights coach -->
    <string name="insights_coach_this_week">This week</string>
    <string name="insights_coach_reviews">Reviews</string>
    <string name="insights_coach_accuracy">Accuracy</string>
    <string name="insights_coach_leveled_up">Words leveled up</string>
    <string name="insights_coach_accuracy_locked">After 20 reviews</string>
    <string name="insights_coach_change_more">▲ %1$d more than last week</string>
    <string name="insights_coach_change_fewer">▼ %1$d fewer than last week</string>
    <string name="insights_coach_change_points_up">▲ %1$d points vs last week</string>
    <string name="insights_coach_change_points_down">▼ %1$d points vs last week</string>
    <string name="insights_coach_change_same">Same as last week</string>
    <string name="insights_coach_streak">%1$d-day streak</string>
    <string name="insights_coach_next_steps">Your next steps</string>
    <string name="insights_coach_dismiss">Hide for today</string>
    <string name="insights_coach_more_words">+%1$d more</string>
    <string name="insights_coach_reminder_at">Daily at %1$s</string>
    <string name="insights_coach_deep_dive">Dig deeper</string>
    <string name="insights_coach_section_mastery">Mastery journey</string>
    <string name="insights_coach_section_habits">Study habits</string>
    <string name="insights_coach_section_words">Your words</string>
    <string name="insights_coach_section_word_rush">Word Rush</string>
    <string name="insights_coach_locked">Unlocks after %1$d more reviews</string>
    <string name="insights_coach_stage">Stage %1$d</string>
    <string name="insights_coach_moved_up">%1$d moved up</string>
    <string name="insights_coach_slipped">%1$d slipped back</string>
    <string name="insights_coach_best_hour">Sharpest hour: %1$s (%2$d%% right)</string>
    <string name="insights_coach_hardest">Hardest words</string>
    <string name="insights_coach_comebacks">Comeback wins</string>
    <string name="insights_coach_games_played">Games played</string>
    <string name="insights_coach_best_score">Best score</string>
    <string name="insights_coach_updated_just_now">Updated just now</string>
    <string name="insights_coach_updated_minutes">Updated %1$dm ago</string>
    <string name="insights_coach_updated_hours">Updated %1$dh ago</string>
    <string name="insights_coach_updated_days">Updated %1$dd ago</string>
    <string name="insights_coach_welcome_title">Your insights start here</string>
    <string name="insights_coach_welcome_body">Finish your first review and we'll show how you're doing and what to study next.</string>
    <string name="insights_coach_welcome_action">Start your first review</string>
    <string name="insights_coach_reminder_on">Daily reminder is on</string>
    <string name="weekday_initial_1">M</string>
    <string name="weekday_initial_2">T</string>
    <string name="weekday_initial_3">W</string>
    <string name="weekday_initial_4">T</string>
    <string name="weekday_initial_5">F</string>
    <string name="weekday_initial_6">S</string>
    <string name="weekday_initial_7">S</string>
```

- [ ] **Step 2: Check for key collisions**

Run: `grep -c 'name="weekday_initial_1"' resources/src/commonMain/composeResources/values/strings.xml`
Expected: `1`.

- [ ] **Step 3: Generate resources**

Run: `./gradlew resources:generateComposeResClass`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add resources/src/commonMain/composeResources/values/strings.xml
git commit -m "feat(insights): add coach screen strings"
```

---

### Task 12: Composables

**Files (one concern each, all under `feature/insights/src/commonMain/kotlin/feature/insights/coach/ui/`):**
- Create: `InsightsCoachScreen.kt` — VM wiring, states, layout
- Create: `HeroSection.kt` — this-week hero
- Create: `ChangeLabel.kt` — arrow + words delta text
- Create: `CoachCardItem.kt` — generic card + word chips + action
- Create: `DeepDiveSection.kt` — collapsible shell + locked teaser + section bodies
- Create: `WeekdayLabels.kt` — weekday initial resource lookup

Before writing: load the `screen-patterns` and `design-system` skills and check `Theme.*` token names (`grep -n "val " design-system/src/commonMain/kotlin/theme/Theme*.kt | head -40`). Replace any token below that does not exist with its nearest existing equivalent; never hardcode colors.

- [ ] **Step 1: `WeekdayLabels.kt`**

```kotlin
package feature.insights.coach.ui

import androidx.compose.runtime.Composable
import lexicon.resources.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun weekdayInitial(isoDay: Int): String = stringResource(
    when (isoDay) {
        1 -> Res.string.weekday_initial_1
        2 -> Res.string.weekday_initial_2
        3 -> Res.string.weekday_initial_3
        4 -> Res.string.weekday_initial_4
        5 -> Res.string.weekday_initial_5
        6 -> Res.string.weekday_initial_6
        else -> Res.string.weekday_initial_7
    }
)
```

- [ ] **Step 2: `ChangeLabel.kt`**

```kotlin
package feature.insights.coach.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import feature.insights.coach.model.ChangeUnit
import feature.insights.coach.model.StatUi
import feature.insights.coach.model.Trend
import lexicon.resources.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import theme.AppColors

/** Change vs last week: arrow + amount + words, so meaning never depends on color alone. */
@Composable
internal fun ChangeLabel(stat: StatUi, modifier: Modifier = Modifier) {
    val text = when (stat.trend) {
        Trend.FLAT -> stringResource(Res.string.insights_coach_change_same)
        Trend.UP -> if (stat.unit == ChangeUnit.POINTS) stringResource(Res.string.insights_coach_change_points_up, stat.changeAmount)
            else stringResource(Res.string.insights_coach_change_more, stat.changeAmount)
        Trend.DOWN -> if (stat.unit == ChangeUnit.POINTS) stringResource(Res.string.insights_coach_change_points_down, stat.changeAmount)
            else stringResource(Res.string.insights_coach_change_fewer, stat.changeAmount)
    }
    val color = when (stat.trend) {
        Trend.UP -> AppColors.success
        Trend.DOWN -> MaterialTheme.colorScheme.onSurfaceVariant
        Trend.FLAT -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text = text, style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}
```

(Down is neutral grey, not red — "encourage, don't scold". If `AppColors.success` doesn't exist, use the existing positive token found in Step 0.)

- [ ] **Step 3: `HeroSection.kt`**

```kotlin
package feature.insights.coach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import components.Pill
import feature.insights.coach.model.DayDotUi
import feature.insights.coach.model.HeroUi
import feature.insights.coach.model.StatUi
import lexicon.resources.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import theme.Theme

@Composable
internal fun HeroSection(hero: HeroUi, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.large))
            .background(Theme.gradients.primaryWash)
            .padding(Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.insights_coach_this_week).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (hero.streak > 0) {
                Pill(text = stringResource(Res.string.insights_coach_streak, hero.streak), color = MaterialTheme.colorScheme.primary)
            }
        }
        Text(text = hero.headline, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
            HeroStat(stringResource(Res.string.insights_coach_reviews), hero.reviews, Modifier.weight(1f))
            val accuracy = hero.accuracy
            if (accuracy != null) {
                HeroStat(stringResource(Res.string.insights_coach_accuracy), accuracy, Modifier.weight(1f))
            } else {
                LockedStat(stringResource(Res.string.insights_coach_accuracy), Modifier.weight(1f))
            }
            HeroStat(stringResource(Res.string.insights_coach_leveled_up), hero.leveledUp, Modifier.weight(1f))
        }
        WeekDots(hero.week)
    }
}

@Composable
private fun HeroStat(label: String, stat: StatUi, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
        Text(stat.value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChangeLabel(stat)
    }
}

@Composable
private fun LockedStat(label: String, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
        Text("—", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(Res.string.insights_coach_accuracy_locked), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WeekDots(week: List<DayDotUi>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        week.forEach { day ->
            val label = weekdayInitial(day.isoDay)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs),
                modifier = Modifier.semantics { contentDescription = "$label ${day.reviews}" },
            ) {
                val fill = when {
                    day.reviews > 0 -> MaterialTheme.colorScheme.primary
                    day.isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                Box(Modifier.size(Theme.dimensions.iconSize).clip(CircleShape).background(fill))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
```

- [ ] **Step 4: `CoachCardItem.kt`**

```kotlin
package feature.insights.coach.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import components.AccentCard
import components.AccentIconBadge
import components.Pill
import domain.insights.model.CoachAction
import feature.insights.coach.model.CoachCardUi
import lexicon.resources.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/** Generic coach card: renders any server type from title/body/words/action. */
@Composable
internal fun CoachCardItem(
    card: CoachCardUi,
    onAction: (CoachCardUi) -> Unit,
    onDismiss: (CoachCardUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    AccentCard(accent = accent, enabled = true, onClick = { onAction(card) }, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(Theme.spacing.md), verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
                AccentIconBadge(icon = iconFor(card.type), accent = accent)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
                    Text(card.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(card.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onDismiss(card) }) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(Res.string.insights_coach_dismiss))
                }
            }
            if (card.words.isNotEmpty()) WordChips(card)
            ActionButton(card, onAction)
        }
    }
}

@Composable
private fun WordChips(card: CoachCardUi) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs), verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
        card.words.forEach { Pill(text = it.text, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, height = Theme.dimensions.chipHeight) }
        if (card.moreWordsCount > 0) {
            Pill(text = stringResource(Res.string.insights_coach_more_words, card.moreWordsCount), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActionButton(card: CoachCardUi, onAction: (CoachCardUi) -> Unit) {
    val label = when (val action = card.action) {
        is CoachAction.ReviewWords -> action.label
        is CoachAction.StartReview -> action.label
        is CoachAction.StartWordRush -> action.label
        is CoachAction.EnableReminder -> card.hourLabel?.let { "${action.label} · ${stringResource(Res.string.insights_coach_reminder_at, it)}" } ?: action.label
        CoachAction.None -> return
    }
    FilledTonalButton(onClick = { onAction(card) }) { Text(label) }
}

private fun iconFor(type: String): ImageVector = when (type) {
    "STREAK_AT_RISK" -> Icons.Rounded.LocalFireDepartment
    "SLIPPING_WORDS", "DIFFICULT_WORDS", "LEVEL_BOTTLENECK" -> Icons.Rounded.Refresh
    "BEST_TIME" -> Icons.Rounded.AlarmOn
    "MILESTONE_NEAR", "COMEBACK_WIN" -> Icons.Rounded.EmojiEvents
    "WEEK_TREND" -> Icons.Rounded.Bolt
    else -> Icons.Rounded.AutoAwesome
}
```

(`FlowRow` needs `@OptIn(ExperimentalLayoutApi::class)` on older Compose; add it if the compiler asks. If `Theme.dimensions.chipHeight` doesn't exist, omit the `height` argument.)

- [ ] **Step 5: `DeepDiveSection.kt`**

```kotlin
package feature.insights.coach.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import components.Pill
import feature.insights.coach.model.DeepDiveUi
import feature.insights.coach.model.SectionKind
import lexicon.resources.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import theme.Theme

@Composable
internal fun DeepDiveSection(section: DeepDiveUi, expanded: Boolean, onToggle: (SectionKind) -> Unit) {
    val chevron by animateFloatAsState(if (expanded) 180f else 0f)
    val locked = section is DeepDiveUi.Locked
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.large))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(enabled = !locked) { onToggle(section.kind) }
            .padding(Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(titleFor(section.kind), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (section.caption.isNotEmpty()) {
                    Text(section.caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                imageVector = if (locked) Icons.Rounded.Lock else Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = if (locked) Modifier else Modifier.rotate(chevron),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (section is DeepDiveUi.Locked) {
            LinearProgressIndicator(progress = { section.progress }, modifier = Modifier.fillMaxWidth())
            Text(stringResource(Res.string.insights_coach_locked, section.reviewsNeeded), style = MaterialTheme.typography.labelMedium)
        }
        AnimatedVisibility(visible = expanded && !locked) {
            // Chart reads the caption aloud as its takeaway.
            Box(Modifier.semantics { contentDescription = section.caption }) {
                when (section) {
                    is DeepDiveUi.Mastery -> MasteryBody(section)
                    is DeepDiveUi.Habits -> HabitsBody(section)
                    is DeepDiveUi.Words -> WordsBody(section)
                    is DeepDiveUi.WordRush -> WordRushBody(section)
                    is DeepDiveUi.Locked -> Unit
                }
            }
        }
    }
}

@Composable
private fun titleFor(kind: SectionKind) = stringResource(
    when (kind) {
        SectionKind.MASTERY -> Res.string.insights_coach_section_mastery
        SectionKind.HABITS -> Res.string.insights_coach_section_habits
        SectionKind.WORDS -> Res.string.insights_coach_section_words
        SectionKind.WORD_RUSH -> Res.string.insights_coach_section_word_rush
    }
)

@Composable
private fun MasteryBody(section: DeepDiveUi.Mastery) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
        section.levels.forEach { bar ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
                Text(stringResource(Res.string.insights_coach_stage, bar.level), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(64.dp))
                LinearProgressIndicator(progress = { bar.fraction }, modifier = Modifier.weight(1f))
                Text(bar.words.toString(), style = MaterialTheme.typography.labelMedium)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
            Pill(stringResource(Res.string.insights_coach_moved_up, section.promoted), color = MaterialTheme.colorScheme.primary)
            if (section.demoted > 0) Pill(stringResource(Res.string.insights_coach_slipped, section.demoted), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HabitsBody(section: DeepDiveUi.Habits) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
        val hour = section.bestHourLabel
        val accuracy = section.bestHourAccuracy
        if (hour != null && accuracy != null) {
            Text(stringResource(Res.string.insights_coach_best_hour, hour, accuracy), style = MaterialTheme.typography.bodyMedium)
        }
        Row(Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            section.weekdays.forEach { day ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${day.accuracyPct}%", style = MaterialTheme.typography.labelSmall, fontWeight = if (day.isBest) FontWeight.Bold else FontWeight.Normal)
                    Box(
                        Modifier.width(20.dp).height((day.accuracyPct * 0.6f).dp)
                            .clip(RoundedCornerShape(Theme.shapes.small))
                            .background(if (day.isBest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    )
                    Text(weekdayInitial(day.isoDay), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun WordsBody(section: DeepDiveUi.Words) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
        if (section.hardest.isNotEmpty()) {
            Text(stringResource(Res.string.insights_coach_hardest), style = MaterialTheme.typography.labelLarge)
            section.hardest.forEach { word ->
                Row {
                    Text(word.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("${word.accuracyPct}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (section.comebacks.isNotEmpty()) {
            Text(stringResource(Res.string.insights_coach_comebacks), style = MaterialTheme.typography.labelLarge)
            section.comebacks.forEach { Text(it.text, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun WordRushBody(section: DeepDiveUi.WordRush) {
    Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.lg)) {
        Column {
            Text(section.gamesPlayed, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.insights_coach_games_played), style = MaterialTheme.typography.labelMedium)
        }
        Column {
            Text(section.bestScore, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.insights_coach_best_score), style = MaterialTheme.typography.labelMedium)
        }
    }
}
```

- [ ] **Step 6: `InsightsCoachScreen.kt`**

```kotlin
package feature.insights.coach.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import components.ErrorScreen
import components.LoadingScreen
import components.scaffold.ActionIconConfig
import components.scaffold.LexiconColumn
import components.scaffold.TopBarColor
import core.ui.OnEvents
import domain.word.model.ReviewSource
import feature.insights.coach.InsightsCoachEffect
import feature.insights.coach.InsightsCoachState
import feature.insights.coach.InsightsCoachViewModel
import feature.insights.coach.UpdatedAgo
import feature.insights.coach.model.CoachCardUi
import feature.insights.coach.model.InsightsUiModel
import feature.insights.coach.model.SectionKind
import lexicon.resources.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import theme.Theme

@Composable
fun InsightsCoachScreen(
    onShowLeaderboard: () -> Unit,
    onStartReview: (ReviewSource) -> Unit,
    onStartWordRush: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val viewModel = koinViewModel<InsightsCoachViewModel>()
    val state by viewModel.state()

    // Refresh every time the tab is shown; cached content renders immediately.
    LaunchedEffect(Unit) { viewModel.refresh() }

    OnEvents(viewModel.effects) { effect ->
        when (effect) {
            is InsightsCoachEffect.StartReview -> onStartReview(effect.source)
            InsightsCoachEffect.StartWordRush -> onStartWordRush()
            InsightsCoachEffect.OpenNotificationSettings -> onOpenNotificationSettings()
            InsightsCoachEffect.ReminderEnabled -> Unit
        }
    }

    InsightsCoachContent(
        state = state,
        onShowLeaderboard = onShowLeaderboard,
        onRetry = viewModel::refresh,
        onAction = viewModel::onCoachAction,
        onDismiss = viewModel::dismissCard,
        onToggleSection = viewModel::toggleSection,
        onStartFirstReview = { onStartReview(ReviewSource.DueCards) },
    )
}

@Composable
internal fun InsightsCoachContent(
    state: InsightsCoachState,
    onShowLeaderboard: () -> Unit,
    onRetry: () -> Unit,
    onAction: (CoachCardUi) -> Unit,
    onDismiss: (CoachCardUi) -> Unit,
    onToggleSection: (SectionKind) -> Unit,
    onStartFirstReview: () -> Unit,
) {
    LexiconColumn(
        title = stringResource(Res.string.insights_title),
        scrollable = false,
        topBarColor = TopBarColor.Background,
        actionIcon1 = ActionIconConfig(
            icon = Icons.Rounded.EmojiEvents,
            contentDescription = stringResource(Res.string.leaderboard),
            onClick = onShowLeaderboard,
            size = Theme.dimensions.iconSize,
        ),
    ) {
        val content = state.content
        when {
            content == null && state.error != null ->
                ErrorScreen(message = state.error, retryLabel = stringResource(Res.string.retry), onRetry = onRetry)
            content == null -> LoadingScreen(message = stringResource(Res.string.insights_loading))
            content.isNewUser -> WelcomeContent(onStartFirstReview)
            else -> LoadedContent(content, state, onAction, onDismiss, onToggleSection)
        }
    }
}

@Composable
private fun LoadedContent(
    content: InsightsUiModel,
    state: InsightsCoachState,
    onAction: (CoachCardUi) -> Unit,
    onDismiss: (CoachCardUi) -> Unit,
    onToggleSection: (SectionKind) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = Theme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
    ) {
        state.updatedAgo?.takeIf { state.isStale }?.let { UpdatedAgoLabel(it) }
        HeroSection(content.hero)
        if (content.coach.isNotEmpty()) {
            SectionHeader(stringResource(Res.string.insights_coach_next_steps))
            content.coach.forEach { card -> CoachCardItem(card, onAction, onDismiss) }
        }
        if (content.sections.isNotEmpty()) {
            SectionHeader(stringResource(Res.string.insights_coach_deep_dive))
            content.sections.forEach { section -> DeepDiveSection(section, section.kind in state.expanded, onToggleSection) }
        }
        Spacer(Modifier.height(Theme.spacing.xl))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun UpdatedAgoLabel(ago: UpdatedAgo) {
    val text = when (ago) {
        UpdatedAgo.JustNow -> stringResource(Res.string.insights_coach_updated_just_now)
        is UpdatedAgo.Minutes -> stringResource(Res.string.insights_coach_updated_minutes, ago.value)
        is UpdatedAgo.Hours -> stringResource(Res.string.insights_coach_updated_hours, ago.value)
        is UpdatedAgo.Days -> stringResource(Res.string.insights_coach_updated_days, ago.value)
    }
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun WelcomeContent(onStartFirstReview: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(horizontal = Theme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        ) {
            Text(stringResource(Res.string.insights_coach_welcome_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(stringResource(Res.string.insights_coach_welcome_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(onClick = onStartFirstReview) { Text(stringResource(Res.string.insights_coach_welcome_action)) }
        }
    }
}
```

Imports for `ErrorScreen`, `LoadingScreen`, `OnEvents`, `LexiconColumn`, `ActionIconConfig`, `TopBarColor`: copy the exact import lines from the top of `feature/insights/src/commonMain/kotlin/feature/insights/ui/InsightsScreen.kt` (lines 1–104); the paths above are best guesses.

- [ ] **Step 7: Compile**

Run: `./gradlew feature:insights:compileKotlinMetadata composeApp:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL. Fix token/import names against the design system as noted; no other changes.

- [ ] **Step 8: Detekt**

Run: `./gradlew detekt`
Expected: no new violations in `feature/insights/coach`.

- [ ] **Step 9: Commit**

```bash
git add feature/insights/src/commonMain/kotlin/feature/insights/coach/ui
git commit -m "feat(insights): add coach screen composables"
```

---

### Task 13: Navigation, flag, DI, E2E

**Files:**
- Modify: `feature/insights/src/commonMain/kotlin/feature/insights/navigation/InsightsRoute.kt`
- Modify: `feature/insights/src/commonMain/kotlin/feature/insights/di/InsightsModule.kt`
- Modify: `presentation/src/commonMain/kotlin/presentation/ui/NavigationGraph.kt`
- Modify: `platforms/src/androidMain/kotlin/featureflag/AndroidFeatureFlagProvider.kt` (defaults map), and iOS/wasm providers if they keep their own defaults
- Create: `maestro/flows/insights/01_insights_coach.yaml`

- [ ] **Step 1: Route switch** — `InsightsRoute.kt`:

```kotlin
const val INSIGHTS_COACH_FLAG = "insights_coach"

fun NavGraphBuilder.insightsGraph(
    isCoachEnabled: () -> Boolean,
    onShowLeaderboard: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
    onStartReview: (ReviewSource) -> Unit = {},
    onStartWordRush: () -> Unit = {},
) {
    composable<InsightsRoute> {
        if (isCoachEnabled()) {
            InsightsCoachScreen(
                onShowLeaderboard = onShowLeaderboard,
                onStartReview = onStartReview,
                onStartWordRush = onStartWordRush,
                onOpenNotificationSettings = onNavigateToNotificationSettings,
            )
        } else {
            InsightsScreen(
                onShowLeaderboard = onShowLeaderboard,
                onNavigateToReview = { ids -> onStartReview(ReviewSource.ByWords(ids)) },
                onNavigateToNotificationSettings = onNavigateToNotificationSettings,
            )
        }
    }
}
```

(The old screen now gets a working `onNavigateToReview` — fixes the dead "Study difficult words" button even with the flag off.)

- [ ] **Step 2: DI** — `InsightsModule.kt`, add:

```kotlin
    factoryOf(::InsightsCoachUseCases)
    viewModel {
        InsightsCoachViewModel(
            useCases = get(),
            analyticsTracker = get(),
            use24Hour = ::is24HourClock,
        )
    }
```

(imports: `feature.insights.coach.*`, `time.is24HourClock`.)

- [ ] **Step 3: Wire NavigationGraph** — replace the `insightsGraph(...)` call:

```kotlin
        val featureFlags = koinInject<IFeatureFlagProvider>()
        val studyLauncher = koinInject<NotificationNavigator>()
        insightsGraph(
            isCoachEnabled = { featureFlags.getBoolean(INSIGHTS_COACH_FLAG, default = false) },
            onShowLeaderboard = { overlayHost.showLeaderboard() },
            onNavigateToNotificationSettings = { overlayHost.showNotificationSettingsSheet() },
            onStartReview = studyLauncher::openReview,
            onStartWordRush = studyLauncher::openWordRush,
        )
```

If `koinInject` can't be called there (non-composable `NavHost` builder), hoist both `koinInject` calls to the top of the enclosing `@Composable` function and reference the vals.

- [ ] **Step 4: Flag default** — add `"insights_coach" to false` to the defaults map in each provider that has one (`grep -rn "defaults" platforms/src/*/kotlin/featureflag`). For local verification, temporarily flip debug default to `true` only on your machine or set the flag in Firebase Remote Config for your test account — do not commit `true`.

- [ ] **Step 5: Maestro flow** — `maestro/flows/insights/01_insights_coach.yaml` (match the header of an existing flow, e.g. `maestro/flows/study/08_word_rush_game_flow.yaml`):

```yaml
appId: com.alirezaiyan.vokab
---
- launchApp
- tapOn: "Insights"
- assertVisible: "THIS WEEK"
- assertVisible: "Your next steps"
- tapOn:
    text: "Review.*|Keep going|Review now"
    index: 0
- assertVisible:
    id: "review_screen"
    optional: true
- back
- tapOn: "Insights"
- tapOn: "Dig deeper"
  optional: true
- takeScreenshot: maestro/screenshots/insights_coach
```

(Use the real `appId` from existing flows and the review screen's real test tag; `grep -rn "appId" maestro/flows | head -1`, `grep -rn "testTag(" feature/study/src | grep -i review | head`.)

- [ ] **Step 6: Full verification**

Run, in order:
1. `./gradlew composeApp:cleanAllTests composeApp:allTests` — Expected: 0 failures (full suite, not just new tests).
2. `./gradlew detekt` — Expected: clean.
3. `./gradlew composeApp:assembleDebug` — Expected: BUILD SUCCESSFUL.
4. `./gradlew composeApp:linkDebugFrameworkIosSimulatorArm64` — Expected: BUILD SUCCESSFUL (verifies iOS `is24HourClock` actual).
5. Install on emulator with flag on, backend deployed. Verify by hand and with `maestro test maestro/flows/insights/01_insights_coach.yaml`:
   - hero shows this week's numbers with "▲/▼ … than last week" text
   - accuracy shows "—" + "After 20 reviews" on a low-activity account
   - coach card tap opens a review of exactly the listed words (Study tab)
   - "Hide for today" removes the card; it stays hidden after relaunch
   - airplane mode → reopen tab → cached content + "Updated Xm ago", no error screen
   - best-hour label matches device 12/24h setting
   - flag off → old screen; "Study difficult words" now opens a review
6. Upgrade install over the previous release build to confirm the SQLDelight migration runs (no crash on launch).

- [ ] **Step 7: Commit**

```bash
git add feature/insights/src/commonMain/kotlin/feature/insights/navigation feature/insights/src/commonMain/kotlin/feature/insights/di presentation/src/commonMain/kotlin/presentation/ui/NavigationGraph.kt platforms/src/*/kotlin/featureflag maestro/flows/insights
git commit -m "feat(insights): ship coach screen behind insights_coach flag"
```

---

### Follow-up (separate change, after flag is default-on in production)

Delete `feature/insights/ui/InsightsScreen.kt`, `InsightsTrendsTab.kt`, `InsightsHeader.kt`, `InsightsViewModel.kt`, `InsightsAvailability.kt`, `WeeklyReportUiModel.kt`, the flag branch in `insightsGraph`, and any analytics use cases no other screen references (`grep -rn "<UseCaseName>" --include='*.kt' . | grep -v /build/` per use case before deleting).
