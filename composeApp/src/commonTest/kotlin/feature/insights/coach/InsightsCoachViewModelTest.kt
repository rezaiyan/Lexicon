package feature.insights.coach

import app.cash.turbine.test
import domain.insights.InsightsFixtures
import domain.insights.model.CoachAction
import domain.insights.usecase.DismissCoachCardUseCase
import domain.insights.usecase.ObserveDismissedCoachCardsUseCase
import domain.insights.usecase.ObserveInsightsScreenUseCase
import domain.insights.usecase.RefreshInsightsScreenUseCase
import domain.settings.usecase.SetReviewRemindersEnabledUseCase
import domain.word.model.ProgressStats
import domain.word.model.ReviewSource
import domain.word.model.Word
import domain.word.usecase.GetProgressStatsUseCase
import domain.word.usecase.ObserveReviewForecastUseCase
import fakes.FakeAnalyticsTracker
import fakes.FakeDismissedCoachCardsRepository
import fakes.FakeInsightsScreenRepository
import fakes.FakeSettingsRepository
import fakes.FakeWordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import utils.Language
import kotlin.time.Instant
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
    private val words = FakeWordRepository()

    private fun viewModel() = InsightsCoachViewModel(
        useCases = InsightsCoachUseCases(
            observeScreen = ObserveInsightsScreenUseCase(screens),
            refreshScreen = RefreshInsightsScreenUseCase(screens),
            observeDismissed = ObserveDismissedCoachCardsUseCase(dismissed),
            dismissCard = DismissCoachCardUseCase(dismissed),
            setReviewReminders = SetReviewRemindersEnabledUseCase(settings),
            observeForecast = ObserveReviewForecastUseCase(words, now = { Instant.fromEpochMilliseconds(NOW) }, timeZone = { TimeZone.UTC }),
            observeProgress = GetProgressStatsUseCase(words),
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
        assertTrue(tracker.events.any { it.first == "coach_card_action" && it.second?.get("type") == "SLIPPING_WORDS" })
    }

    @Test
    fun `enable reminder action turns reminders on`() = runTest {
        val reminderCard = InsightsFixtures.card(type = "BEST_TIME", action = CoachAction.EnableReminder("Remind me", 20))
        screens.cache.value = InsightsFixtures.cached(InsightsFixtures.screen(coach = listOf(reminderCard)))
        settings.reviewRemindersEnabled = false
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
    fun `enable reminder success hides the reminder card`() = runTest {
        val reminderCard = InsightsFixtures.card(type = "BEST_TIME", action = CoachAction.EnableReminder("Remind me", 20))
        screens.cache.value = InsightsFixtures.cached(InsightsFixtures.screen(coach = listOf(reminderCard)))
        val vm = viewModel()

        vm.onCoachAction(vm.currentState.content!!.coach.single())

        assertTrue(vm.currentState.content!!.coach.isEmpty())
    }

    @Test
    fun `forecast and stages come from local words without the server`() = runTest {
        words.storedWords = mutableListOf(word(1, due = NOW - 1), word(2, due = NOW + DAY_MS))
        words.progressStats = ProgressStats(level0Count = 1, level6Count = 1, totalWords = 2)

        val state = viewModel().currentState

        assertNull(state.content)
        assertEquals(1, state.forecast?.dueNow)
        assertEquals(1, state.forecast?.tomorrow)
        assertEquals(2, state.stages?.total)
    }

    @Test
    fun `review hardest starts a review of the listed words`() = runTest {
        screens.cache.value = InsightsFixtures.cached()
        val vm = viewModel()

        vm.effects.test {
            vm.reviewHardest()
            assertEquals(InsightsCoachEffect.StartReview(ReviewSource.ByWords(listOf(1L))), awaitItem())
        }
    }

    @Test
    fun `review due now starts a due-cards review`() = runTest {
        val vm = viewModel()

        vm.effects.test {
            vm.reviewDueNow()
            assertEquals(InsightsCoachEffect.StartReview(ReviewSource.DueCards), awaitItem())
        }
    }

    private fun word(id: Int, due: Long) = Word(
        id = id,
        originalWord = "w$id",
        translation = "t$id",
        description = "",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.GERMAN,
        nextReviewDate = due,
    )

    private companion object {
        /** 2026-10-23T12:00Z */
        const val NOW = 1_792_756_800_000L
        const val DAY_MS = 86_400_000L
    }
}
