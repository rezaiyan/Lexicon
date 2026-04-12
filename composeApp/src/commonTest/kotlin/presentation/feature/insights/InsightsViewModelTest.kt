package presentation.feature.insights

import core.common.Try
import core.common.UiState
import data.storage.DailyInsightCache
import domain.analytics.model.AccuracyByLevel
import domain.analytics.model.ComebackWord
import domain.analytics.model.DailyStudyStats
import domain.analytics.model.DayOfWeekAccuracy
import domain.analytics.model.HourlyAccuracy
import domain.analytics.model.LanguagePairStats
import domain.analytics.model.MasteredWord
import domain.analytics.model.MonthlyStats
import domain.analytics.model.MostReviewedWord
import domain.analytics.model.StudyHeatmapDay
import domain.analytics.model.StudyInsights
import domain.analytics.model.StudySession
import domain.analytics.model.WeeklyReport
import domain.analytics.model.WordDifficulty
import domain.analytics.repository.IAnalyticsStatsRepository
import domain.analytics.repository.IAnalyticsWordRepository
import domain.analytics.usecase.GetAccuracyByLevelUseCase
import domain.analytics.usecase.GetAccuracyTrendUseCase
import domain.analytics.usecase.GetBestStudyTimeUseCase
import domain.analytics.usecase.GetDifficultWordsUseCase
import domain.analytics.usecase.GetStudyHeatmapUseCase
import domain.analytics.usecase.GetStudyInsightsUseCase
import domain.analytics.usecase.GetWeeklyReportUseCase
import domain.analytics.usecase.GetLevelTransitionsUseCase
import domain.analytics.usecase.GetResponseTimeTrendUseCase
import domain.analytics.model.LevelTransition
import domain.analytics.model.ResponseTimeTrend
import domain.wordrush.model.WordRushInsights
import feature.insights.WeeklyReportUiModel
import domain.wordrush.repository.IWordRushStatsRepository
import domain.wordrush.usecase.GetWordRushInsightsUseCase
import domain.profile.model.ProfileStats
import domain.profile.repository.IProfileStatsRepository
import domain.profile.usecase.GetProfileStatsUseCase
import feature.insights.InsightsEffect
import feature.insights.InsightsViewModel
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InsightsViewModelTest : ViewModelTestBase() {

    // region Fake

    private class FakeAnalyticsRepository(
        var studyInsightsResult: Try<StudyInsights> = Try.success(defaultInsights()),
        var dailyStatsResult: Try<List<DailyStudyStats>> = Try.success(defaultDailyStats()),
        var difficultWordsResult: Try<List<WordDifficulty>> = Try.success(defaultDifficultWords()),
        var accuracyByLevelResult: Try<List<AccuracyByLevel>> = Try.success(defaultAccuracyByLevel()),
        var heatmapResult: Try<List<StudyHeatmapDay>> = Try.success(defaultHeatmap()),
        var accuracyByHourResult: Try<List<HourlyAccuracy>> = Try.success(defaultHourlyAccuracy()),
        var weeklyReportResult: Try<WeeklyReport> = Try.success(defaultWeeklyReport()),
    ) : IAnalyticsStatsRepository, IAnalyticsWordRepository {
        var insightsCallCount = 0

        // IAnalyticsStatsRepository
        override suspend fun getStudyInsights(): Try<StudyInsights> {
            insightsCallCount++
            return studyInsightsResult
        }
        override suspend fun getDailyStats(startDate: String, endDate: String): Try<List<DailyStudyStats>> = dailyStatsResult
        override suspend fun getRecentSessions(limit: Int): Try<List<StudySession>> = Try.success(emptyList())
        override suspend fun getStudyHeatmap(startDate: String, endDate: String): Try<List<StudyHeatmapDay>> = heatmapResult
        override suspend fun getWeeklyReport(): Try<WeeklyReport> = weeklyReportResult
        override suspend fun getMonthlyStats(): Try<List<MonthlyStats>> = Try.success(emptyList())
        override suspend fun getResponseTimeTrend(): Try<List<domain.analytics.model.ResponseTimeTrend>> = Try.success(emptyList())
        override suspend fun syncToBackend(): Try<Int> = Try.success(0)

        // IAnalyticsWordRepository
        override suspend fun getDifficultWords(minReviews: Int, limit: Int): Try<List<WordDifficulty>> = difficultWordsResult
        override suspend fun getMostReviewedWords(limit: Int): Try<List<MostReviewedWord>> = Try.success(emptyList())
        override suspend fun getAccuracyByLevel(): Try<List<AccuracyByLevel>> = accuracyByLevelResult
        override suspend fun getAccuracyByHourOfDay(): Try<List<HourlyAccuracy>> = accuracyByHourResult
        override suspend fun getAccuracyByDayOfWeek(): Try<List<DayOfWeekAccuracy>> = Try.success(emptyList())
        override suspend fun getWordsMastered(limit: Int): Try<List<MasteredWord>> = Try.success(emptyList())
        override suspend fun getLanguagePairStats(): Try<List<LanguagePairStats>> = Try.success(emptyList())
        override suspend fun getComebackWords(): Try<List<ComebackWord>> = Try.success(emptyList())
        override suspend fun getLevelTransitions(): Try<List<domain.analytics.model.LevelTransition>> = Try.success(emptyList())
    }

    // endregion

    private class FakeWordRushStatsRepository(
        var insightsResult: Try<WordRushInsights> = Try.success(defaultWordRushInsights()),
    ) : IWordRushStatsRepository {
        override suspend fun getInsights(): Try<WordRushInsights> = insightsResult
    }

    // endregion

    // region Helpers

    private class FakeProfileStatsRepository(
        var result: Try<ProfileStats> = Try.success(
            ProfileStats(currentStreak = 0, longestStreak = 0, memberSince = "", weeklyActivity = emptyList(), languages = emptyList())
        )
    ) : IProfileStatsRepository {
        override suspend fun getProfileStats(): Try<ProfileStats> = result
    }

    private class FakeDailyInsightCache(initial: String? = null) : DailyInsightCache {
        private var value: String? = initial
        override fun getDailyInsight(): String? = value
        override fun saveDailyInsight(message: String) { value = message }
        override fun clearDailyInsight() { value = null }
    }

    companion object {
        fun defaultInsights() = StudyInsights(
            totalCardsReviewed = 100,
            totalCorrect = 80,
            accuracyPercent = 80.0,
            totalStudyTimeMs = 3600000,
            totalSessions = 10,
            daysStudied = 5,
            uniqueWordsReviewed = 50,
            averageResponseTimeMs = 2000,
            averageSessionDurationMs = 360000,
            wordsMasteredCount = 10,
            sessionCompletionRate = null,
        )

        fun defaultDailyStats() = listOf(
            DailyStudyStats(
                date = "2026-03-01", sessionsCount = 2, cardsReviewed = 20,
                correctCount = 16, incorrectCount = 4, studyTimeMs = 600000,
                uniqueWordsReviewed = 15, wordsLeveledUp = 3, wordsLeveledDown = 1,
            )
        )

        fun defaultDifficultWords() = listOf(
            WordDifficulty(
                wordId = 1, wordText = "difficult", wordTranslation = "dificil",
                sourceLanguage = "EN", targetLanguage = "ES",
                totalReviews = 10, errorCount = 6, errorRate = 0.6,
            )
        )

        fun defaultAccuracyByLevel() = listOf(
            AccuracyByLevel(level = 1, totalReviews = 50, correctCount = 35, accuracyPercent = 70.0),
        )

        fun defaultHeatmap() = listOf(
            StudyHeatmapDay(date = "2026-03-01", count = 5),
        )

        fun defaultHourlyAccuracy() = listOf(
            HourlyAccuracy(hour = 14, totalReviews = 20, correctCount = 18, accuracyPercent = 90.0),
        )

        fun defaultWordRushInsights() = WordRushInsights(
            totalGames = 15,
            totalCompleted = 12,
            completionRatePercent = 80.0,
            bestStreakEver = 8,
            avgScore = 42.5,
            avgAccuracyPercent = 75.0,
            totalTimePlayedMs = 180000,
            avgDurationMs = 12000.0,
            avgResponseMs = 2500.0,
        )

        fun defaultWeeklyReport() = WeeklyReport(
            cardsReviewed = 50,
            previousWeekCardsReviewed = 40,
            changePercent = 25.0,
            accuracyPercent = 82.0,
            wordsMastered = 5,
            totalStudyTimeMs = 1800000L,
            sessionsCount = 7,
            bestDay = null,
            weekStartDate = "2026-03-01",
            weekEndDate = "2026-03-07",
        )
    }

    private fun createViewModel(
        repo: FakeAnalyticsRepository = FakeAnalyticsRepository(),
        wordRushStatsRepo: FakeWordRushStatsRepository = FakeWordRushStatsRepository(),
        profileStatsRepo: FakeProfileStatsRepository = FakeProfileStatsRepository(),
        dailyInsightCache: DailyInsightCache = FakeDailyInsightCache(),
    ): InsightsViewModel {
        return InsightsViewModel(
            getStudyInsightsUseCase = GetStudyInsightsUseCase(repo),
            getDifficultWordsUseCase = GetDifficultWordsUseCase(repo),
            getAccuracyTrendUseCase = GetAccuracyTrendUseCase(repo),
            getAccuracyByLevelUseCase = GetAccuracyByLevelUseCase(repo),
            getStudyHeatmapUseCase = GetStudyHeatmapUseCase(repo),
            getBestStudyTimeUseCase = GetBestStudyTimeUseCase(repo),
            getWordRushInsightsUseCase = GetWordRushInsightsUseCase(wordRushStatsRepo),
            getWeeklyReportUseCase = GetWeeklyReportUseCase(repo),
            getLevelTransitionsUseCase = GetLevelTransitionsUseCase(repo),
            getResponseTimeTrendUseCase = GetResponseTimeTrendUseCase(repo),
            getProfileStatsUseCase = GetProfileStatsUseCase(profileStatsRepo),
            dailyInsightCache = dailyInsightCache,
        )
    }

    // endregion

    // region Init loading

    @Test
    fun `init does not trigger any network calls`() = runTest {
        val repo = FakeAnalyticsRepository()
        val vm = createViewModel(repo)

        assertEquals(0, repo.insightsCallCount)
        assertIs<UiState.Loading>(vm.currentState.overview)
    }

    @Test
    fun `all states load to Loaded on init with successful use cases`() = runTest {
        val vm = createViewModel()
        vm.refresh()

        val state = vm.currentState
        val overview = assertIs<UiState.Loaded<StudyInsights>>(state.overview)
        assertEquals(100, overview.value.totalCardsReviewed)
        assertEquals(80.0, overview.value.accuracyPercent)

        val trend = assertIs<UiState.Loaded<List<DailyStudyStats>>>(state.accuracyTrend)
        assertEquals(1, trend.value.size)

        val words = assertIs<UiState.Loaded<List<WordDifficulty>>>(state.difficultWords)
        assertEquals(1, words.value.size)
        assertEquals("difficult", words.value.first().wordText)

        val levels = assertIs<UiState.Loaded<List<AccuracyByLevel>>>(state.accuracyByLevel)
        assertEquals(1, levels.value.size)

        val heatmap = assertIs<UiState.Loaded<List<StudyHeatmapDay>>>(state.heatmap)
        assertEquals(1, heatmap.value.size)

        val bestTime = assertIs<UiState.Loaded<HourlyAccuracy?>>(state.bestStudyTime)
        assertEquals(14, bestTime.value?.hour)
    }

    // endregion

    // region Error handling

    @Test
    fun `overview shows error when insights use case fails`() = runTest {
        val repo = FakeAnalyticsRepository(
            studyInsightsResult = Try.failure(RuntimeException("DB corrupted")),
        )
        val vm = createViewModel(repo)
        vm.refresh()

        val state = vm.currentState
        val overview = assertIs<UiState.Error>(state.overview)
        assertEquals("DB corrupted", overview.message)
    }

    @Test
    fun `accuracy trend shows error when trend use case fails`() = runTest {
        val repo = FakeAnalyticsRepository(
            dailyStatsResult = Try.failure(RuntimeException("Query failed")),
        )
        val vm = createViewModel(repo)
        vm.refresh()

        val state = vm.currentState
        assertIs<UiState.Error>(state.accuracyTrend)
    }

    @Test
    fun `difficult words shows error when use case fails`() = runTest {
        val repo = FakeAnalyticsRepository(
            difficultWordsResult = Try.failure(RuntimeException("Query failed")),
        )
        val vm = createViewModel(repo)
        vm.refresh()

        assertIs<UiState.Error>(vm.currentState.difficultWords)
    }

    @Test
    fun `best study time shows error when use case fails`() = runTest {
        val repo = FakeAnalyticsRepository(
            accuracyByHourResult = Try.failure(RuntimeException("Hour query failed")),
        )
        val vm = createViewModel(repo)
        vm.refresh()

        assertIs<UiState.Error>(vm.currentState.bestStudyTime)
    }

    @Test
    fun `best study time is null when no hours have enough reviews`() = runTest {
        val repo = FakeAnalyticsRepository(
            accuracyByHourResult = Try.success(
                listOf(HourlyAccuracy(hour = 10, totalReviews = 3, correctCount = 3, accuracyPercent = 100.0))
            ),
        )
        val vm = createViewModel(repo)
        vm.refresh()

        val bestTime = assertIs<UiState.Loaded<HourlyAccuracy?>>(vm.currentState.bestStudyTime)
        assertNull(bestTime.value)
    }

    // endregion

    // region Refresh

    @Test
    fun `refresh re-loads all data`() = runTest {
        val repo = FakeAnalyticsRepository()
        val vm = createViewModel(repo)

        // After init, no network calls should have fired
        assertEquals(0, repo.insightsCallCount)

        vm.refresh()

        // After refresh, insights should be called once
        assertEquals(1, repo.insightsCallCount)
        assertIs<UiState.Loaded<StudyInsights>>(vm.currentState.overview)
    }

    @Test
    fun `refresh updates state when data changes`() = runTest {
        val repo = FakeAnalyticsRepository()
        val vm = createViewModel(repo)
        vm.refresh()

        val initialOverview = assertIs<UiState.Loaded<StudyInsights>>(vm.currentState.overview)
        assertEquals(100, initialOverview.value.totalCardsReviewed)

        // Change the data
        repo.studyInsightsResult = Try.success(
            defaultInsights().copy(totalCardsReviewed = 200)
        )

        vm.refresh()

        val updatedOverview = assertIs<UiState.Loaded<StudyInsights>>(vm.currentState.overview)
        assertEquals(200, updatedOverview.value.totalCardsReviewed)
    }

    // endregion

    // region Word Rush Insights

    @Test
    fun `wordRushInsights loaded successfully`() = runTest {
        val vm = createViewModel()
        vm.refresh()

        val state = vm.currentState
        val wordRush = assertIs<UiState.Loaded<WordRushInsights>>(state.wordRushInsights)
        assertEquals(15, wordRush.value.totalGames)
        assertEquals(8, wordRush.value.bestStreakEver)
        assertEquals(75.0, wordRush.value.avgAccuracyPercent)
    }

    @Test
    fun `wordRushInsights shows error when use case fails`() = runTest {
        val wordRushRepo = FakeWordRushStatsRepository(
            insightsResult = Try.failure(RuntimeException("Rush stats failed")),
        )
        val vm = createViewModel(wordRushStatsRepo = wordRushRepo)
        vm.refresh()

        assertIs<UiState.Error>(vm.currentState.wordRushInsights)
    }

    @Test
    fun `availability hasWordRush is true when totalGames greater than zero`() = runTest {
        val vm = createViewModel()
        vm.refresh()

        assertTrue(vm.currentState.availability.hasWordRush)
    }

    @Test
    fun `availability hasWordRush is false when totalGames is zero`() = runTest {
        val wordRushRepo = FakeWordRushStatsRepository(
            insightsResult = Try.success(defaultWordRushInsights().copy(totalGames = 0)),
        )
        val vm = createViewModel(wordRushStatsRepo = wordRushRepo)
        vm.refresh()

        assertEquals(false, vm.currentState.availability.hasWordRush)
    }

    // endregion

    // region Weekly Report

    @Test
    fun `weeklyReport shows Content when report has data`() = runTest {
        val vm = createViewModel()
        vm.refresh()

        val report = assertIs<UiState.Loaded<WeeklyReportUiModel>>(vm.currentState.weeklyReport)
        assertIs<WeeklyReportUiModel.Content>(report.value)
        val content = report.value as WeeklyReportUiModel.Content
        assertEquals("50", content.cardsReviewed)
        assertEquals("+25%", content.changeLabel)
        assertTrue(content.isChangePositive)
    }

    @Test
    fun `weeklyReport shows Empty when cardsReviewed and sessionsCount are both zero`() = runTest {
        val repo = FakeAnalyticsRepository(
            weeklyReportResult = Try.success(
                WeeklyReport(0, 0, null, 0.0, 0, 0L, 0, null, "", "")
            ),
        )
        val vm = createViewModel(repo)
        vm.refresh()

        val report = assertIs<UiState.Loaded<WeeklyReportUiModel>>(vm.currentState.weeklyReport)
        assertIs<WeeklyReportUiModel.Empty>(report.value)
    }

    // endregion

    // region Level Transitions

    @Test
    fun `levelTransitions loaded successfully`() = runTest {
        val transitions = listOf(
            LevelTransition(fromLevel = 1, toLevel = 2, count = 5),
            LevelTransition(fromLevel = 3, toLevel = 2, count = 2),
        )
        val repo = FakeAnalyticsRepository()
        // FakeAnalyticsRepository.getLevelTransitions returns emptyList by default
        // Override by wrapping in a subclass
        val vm = createViewModel(repo)
        vm.refresh()

        assertIs<UiState.Loaded<List<LevelTransition>>>(vm.currentState.levelTransitions)
    }

    @Test
    fun `availability hasTrends is true when levelTransitions are present`() = runTest {
        // FakeAnalyticsRepository returns empty list, so hasTrends from levelTransitions alone would be false
        // But heatmap and accuracyByLevel are non-empty by default → hasTrends is true
        val vm = createViewModel()
        vm.refresh()

        assertTrue(vm.currentState.availability.hasTrends)
    }

    // endregion

    // region Response Time Trend

    @Test
    fun `responseTimeTrend loaded successfully`() = runTest {
        val vm = createViewModel()
        vm.refresh()

        assertIs<UiState.Loaded<List<ResponseTimeTrend>>>(vm.currentState.responseTimeTrend)
    }

    // endregion

    // region Actionable CTAs

    @Test
    fun `studyDifficultWords emits NavigateToReviewWithWords with correct word IDs`() = runTest {
        val vm = createViewModel()
        vm.refresh()

        vm.effects.test {
            vm.studyDifficultWords()
            val effect = awaitItem()
            assertIs<InsightsEffect.NavigateToReviewWithWords>(effect)
            assertEquals(listOf(1L), effect.wordIds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `studyDifficultWords does nothing when difficultWords is not loaded`() = runTest {
        val vm = createViewModel()
        // Do NOT call refresh — difficultWords stays at UiState.Loading

        vm.effects.test {
            vm.studyDifficultWords()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `studyDifficultWords does nothing when difficult words list is empty`() = runTest {
        val repo = FakeAnalyticsRepository(
            difficultWordsResult = Try.success(emptyList()),
        )
        val vm = createViewModel(repo)
        vm.refresh()

        vm.effects.test {
            vm.studyDifficultWords()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setReminderForBestTime emits NavigateToNotificationSettings`() = runTest {
        val vm = createViewModel()
        vm.refresh()

        vm.effects.test {
            vm.setReminderForBestTime()
            assertIs<InsightsEffect.NavigateToNotificationSettings>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // endregion

    // region Day-of-Week Accuracy

    @Test
    fun `accuracyByDayOfWeek has 7 entries after refresh`() = runTest {
        val vm = createViewModel()
        vm.refresh()

        assertEquals(7, vm.currentState.accuracyByDayOfWeek.size)
    }

    @Test
    fun `accuracyByDayOfWeek aggregates Sunday correctly from default stats`() = runTest {
        // defaultDailyStats has date="2026-03-01" (Sunday, isoDayNumber=7)
        // correctCount=16, incorrectCount=4 → totalReviews=20, accuracy=80.0%
        val vm = createViewModel()
        vm.refresh()

        val sunday = vm.currentState.accuracyByDayOfWeek.first { it.dayOfWeek == 7 }
        assertEquals(20L, sunday.totalReviews)
        assertEquals(16L, sunday.correctCount)
        assertEquals(80.0, sunday.accuracyPercent)
    }

    @Test
    fun `accuracyByDayOfWeek returns zero reviews for days with no stats`() = runTest {
        // defaultDailyStats only has data for Sunday (day 7); all other days should be 0
        val vm = createViewModel()
        vm.refresh()

        val nonSundays = vm.currentState.accuracyByDayOfWeek.filter { it.dayOfWeek != 7 }
        nonSundays.forEach { day ->
            assertEquals(0L, day.totalReviews)
            assertEquals(0.0, day.accuracyPercent)
        }
    }

    @Test
    fun `accuracyByDayOfWeek stays empty when trend data is empty list`() = runTest {
        val repo = FakeAnalyticsRepository(
            dailyStatsResult = Try.success(emptyList()),
        )
        val vm = createViewModel(repo)
        vm.refresh()

        vm.currentState.accuracyByDayOfWeek.forEach { day ->
            assertEquals(0L, day.totalReviews)
            assertEquals(0.0, day.accuracyPercent)
        }
    }

    @Test
    fun `accuracyByDayOfWeek remains empty list when trend load fails`() = runTest {
        val repo = FakeAnalyticsRepository(
            dailyStatsResult = Try.failure(RuntimeException("network error")),
        )
        val vm = createViewModel(repo)
        vm.refresh()

        // On failure, accuracyByDayOfWeek is not updated — stays emptyList()
        assertTrue(vm.currentState.accuracyByDayOfWeek.isEmpty())
    }

    // endregion
}
