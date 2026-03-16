package presentation.feature.insights

import core.common.Try
import core.common.UiState
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
import domain.analytics.model.WordDifficulty
import domain.analytics.repository.IAnalyticsRepository
import domain.analytics.usecase.GetAccuracyByLevelUseCase
import domain.analytics.usecase.GetAccuracyTrendUseCase
import domain.analytics.usecase.GetBestStudyTimeUseCase
import domain.analytics.usecase.GetDifficultWordsUseCase
import domain.analytics.usecase.GetStudyHeatmapUseCase
import domain.analytics.usecase.GetStudyInsightsUseCase
import feature.insights.InsightsTab
import feature.insights.InsightsViewModel
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class InsightsViewModelTest : ViewModelTestBase() {

    // region Fake

    private class FakeAnalyticsRepository(
        var studyInsightsResult: Try<StudyInsights> = Try.success(defaultInsights()),
        var dailyStatsResult: Try<List<DailyStudyStats>> = Try.success(defaultDailyStats()),
        var difficultWordsResult: Try<List<WordDifficulty>> = Try.success(defaultDifficultWords()),
        var accuracyByLevelResult: Try<List<AccuracyByLevel>> = Try.success(defaultAccuracyByLevel()),
        var heatmapResult: Try<List<StudyHeatmapDay>> = Try.success(defaultHeatmap()),
        var accuracyByHourResult: Try<List<HourlyAccuracy>> = Try.success(defaultHourlyAccuracy()),
    ) : IAnalyticsRepository {
        var insightsCallCount = 0

        override suspend fun getStudyInsights(): Try<StudyInsights> {
            insightsCallCount++
            return studyInsightsResult
        }
        override suspend fun getDailyStats(startDate: String, endDate: String): Try<List<DailyStudyStats>> = dailyStatsResult
        override suspend fun getDifficultWords(minReviews: Int, limit: Int): Try<List<WordDifficulty>> = difficultWordsResult
        override suspend fun getMostReviewedWords(limit: Int): Try<List<MostReviewedWord>> = Try.success(emptyList())
        override suspend fun getAccuracyByLevel(): Try<List<AccuracyByLevel>> = accuracyByLevelResult
        override suspend fun getAccuracyByHourOfDay(): Try<List<HourlyAccuracy>> = accuracyByHourResult
        override suspend fun getAccuracyByDayOfWeek(): Try<List<DayOfWeekAccuracy>> = Try.success(emptyList())
        override suspend fun getRecentSessions(limit: Int): Try<List<StudySession>> = Try.success(emptyList())
        override suspend fun getStudyHeatmap(startDate: String, endDate: String): Try<List<StudyHeatmapDay>> = heatmapResult
        override suspend fun getWordsMastered(limit: Int): Try<List<MasteredWord>> = Try.success(emptyList())
        override suspend fun getLanguagePairStats(): Try<List<LanguagePairStats>> = Try.success(emptyList())
        override suspend fun getMonthlyStats(): Try<List<MonthlyStats>> = Try.success(emptyList())
        override suspend fun getComebackWords(): Try<List<ComebackWord>> = Try.success(emptyList())
        override suspend fun syncToBackend(): Try<Int> = Try.success(0)
    }

    // endregion

    // region Helpers

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
    }

    private fun createViewModel(
        repo: FakeAnalyticsRepository = FakeAnalyticsRepository(),
    ): InsightsViewModel {
        return InsightsViewModel(
            getStudyInsightsUseCase = GetStudyInsightsUseCase(repo),
            getDifficultWordsUseCase = GetDifficultWordsUseCase(repo),
            getAccuracyTrendUseCase = GetAccuracyTrendUseCase(repo),
            getAccuracyByLevelUseCase = GetAccuracyByLevelUseCase(repo),
            getStudyHeatmapUseCase = GetStudyHeatmapUseCase(repo),
            getBestStudyTimeUseCase = GetBestStudyTimeUseCase(repo),
        )
    }

    // endregion

    // region Init loading

    @Test
    fun `all states load to Loaded on init with successful use cases`() = runTest {
        val vm = createViewModel()

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

        val state = vm.currentState
        assertIs<UiState.Error>(state.accuracyTrend)
    }

    @Test
    fun `difficult words shows error when use case fails`() = runTest {
        val repo = FakeAnalyticsRepository(
            difficultWordsResult = Try.failure(RuntimeException("Query failed")),
        )
        val vm = createViewModel(repo)

        assertIs<UiState.Error>(vm.currentState.difficultWords)
    }

    @Test
    fun `best study time shows error when use case fails`() = runTest {
        val repo = FakeAnalyticsRepository(
            accuracyByHourResult = Try.failure(RuntimeException("Hour query failed")),
        )
        val vm = createViewModel(repo)

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

        val bestTime = assertIs<UiState.Loaded<HourlyAccuracy?>>(vm.currentState.bestStudyTime)
        assertNull(bestTime.value)
    }

    // endregion

    // region Tab selection

    @Test
    fun `selectTab updates selectedTab`() = runTest {
        val vm = createViewModel()

        assertEquals(InsightsTab.OVERVIEW, vm.currentState.selectedTab)

        vm.selectTab(InsightsTab.TRENDS)
        assertEquals(InsightsTab.TRENDS, vm.currentState.selectedTab)

        vm.selectTab(InsightsTab.WORDS)
        assertEquals(InsightsTab.WORDS, vm.currentState.selectedTab)

        vm.selectTab(InsightsTab.OVERVIEW)
        assertEquals(InsightsTab.OVERVIEW, vm.currentState.selectedTab)
    }

    // endregion

    // region Refresh

    @Test
    fun `refresh re-loads all data`() = runTest {
        val repo = FakeAnalyticsRepository()
        val vm = createViewModel(repo)

        // After init, insights should have been called once
        assertEquals(1, repo.insightsCallCount)

        vm.refresh()

        // After refresh, insights should be called again
        assertEquals(2, repo.insightsCallCount)
        assertIs<UiState.Loaded<StudyInsights>>(vm.currentState.overview)
    }

    @Test
    fun `refresh updates state when data changes`() = runTest {
        val repo = FakeAnalyticsRepository()
        val vm = createViewModel(repo)

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
}
