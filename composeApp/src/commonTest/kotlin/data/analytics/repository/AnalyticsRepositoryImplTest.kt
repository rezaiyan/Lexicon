package data.analytics.repository

import core.common.Try
import core.common.getOrThrow
import data.analytics.remote.IAnalyticsStatsDataSource
import data.analytics.remote.IAnalyticsWordDataSource
import data.analytics.remote.model.AccuracyByLevelResponse
import data.analytics.remote.model.ComebackWordResponse
import data.analytics.remote.model.DailyStatsRemoteResponse
import data.analytics.remote.model.DayOfWeekAccuracyResponse
import data.analytics.remote.model.DifficultWordResponse
import data.analytics.remote.model.HeatmapDayResponse
import data.analytics.remote.model.HourlyAccuracyResponse
import data.analytics.remote.model.LanguagePairStatsResponse
import data.analytics.remote.model.MasteredWordResponse
import data.analytics.remote.model.MonthlyStatsResponse
import data.analytics.remote.model.MostReviewedWordResponse
import data.analytics.remote.model.StudyInsightsResponse
import data.analytics.remote.model.StudySessionResponse
import data.analytics.remote.model.SyncAnalyticsRequest
import data.analytics.remote.model.SyncAnalyticsResponse
import data.analytics.remote.model.WeeklyReportRemoteResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsRepositoryImplTest {

    private class FakeStatsDataSource(
        var insightsResult: Try<StudyInsightsResponse> = Try.success(StudyInsightsResponse()),
    ) : IAnalyticsStatsDataSource {
        override suspend fun syncSessions(request: SyncAnalyticsRequest) = Try.success(SyncAnalyticsResponse(emptyList()))
        override suspend fun getInsights() = insightsResult
        override suspend fun getRecentSessions(limit: Int) = Try.success(emptyList<StudySessionResponse>())
        override suspend fun getHeatmap(startMs: Long, endMs: Long) = Try.success(emptyList<HeatmapDayResponse>())
        override suspend fun getMonthlyStats() = Try.success(emptyList<MonthlyStatsResponse>())
        override suspend fun getDailyStats(start: String, end: String) = Try.success(emptyList<DailyStatsRemoteResponse>())
        override suspend fun getWeeklyReport() = Try.success(WeeklyReportRemoteResponse())
        override suspend fun getResponseTimeTrend() = Try.success(emptyList<data.analytics.remote.model.ResponseTimeTrendRemoteResponse>())
    }

    private class FakeWordDataSource(
        var difficultWordsResult: Try<List<DifficultWordResponse>> = Try.success(emptyList()),
        var accuracyByLevelResult: Try<List<AccuracyByLevelResponse>> = Try.success(emptyList()),
        var accuracyByHourResult: Try<List<HourlyAccuracyResponse>> = Try.success(emptyList()),
    ) : IAnalyticsWordDataSource {
        override suspend fun getDifficultWords(minReviews: Int, limit: Int) = difficultWordsResult
        override suspend fun getMostReviewedWords(limit: Int) = Try.success(emptyList<MostReviewedWordResponse>())
        override suspend fun getAccuracyByLevel() = accuracyByLevelResult
        override suspend fun getAccuracyByHour() = accuracyByHourResult
        override suspend fun getAccuracyByDayOfWeek() = Try.success(emptyList<DayOfWeekAccuracyResponse>())
        override suspend fun getWordsMastered(limit: Int) = Try.success(emptyList<MasteredWordResponse>())
        override suspend fun getLanguageStats() = Try.success(emptyList<LanguagePairStatsResponse>())
        override suspend fun getComebackWords() = Try.success(emptyList<ComebackWordResponse>())
        override suspend fun getLevelTransitions() = Try.success(emptyList<data.analytics.remote.model.LevelTransitionRemoteResponse>())
    }

    private fun makeRepo(
        stats: FakeStatsDataSource = FakeStatsDataSource(),
        words: FakeWordDataSource = FakeWordDataSource(),
    ) = AnalyticsRepositoryImpl(statsDataSource = stats, wordDataSource = words)

    @Test
    fun `getStudyInsights maps response to domain model`() = runTest {
        val stats = FakeStatsDataSource(
            insightsResult = Try.success(
                StudyInsightsResponse(
                    totalCardsReviewed = 100,
                    totalCorrect = 80,
                    accuracyPercent = 80.0,
                    totalStudyTimeMs = 60000,
                    totalSessions = 5,
                    daysStudied = 3,
                    uniqueWordsReviewed = 50,
                    wordsMasteredCount = 10,
                )
            )
        )
        val repo = makeRepo(stats = stats)

        val result = repo.getStudyInsights()

        assertTrue(result.isSuccess)
        val insights = result.getOrThrow()
        assertEquals(100L, insights.totalCardsReviewed)
        assertEquals(80L, insights.totalCorrect)
        assertEquals(80.0, insights.accuracyPercent)
        assertEquals(5L, insights.totalSessions)
        assertEquals(3L, insights.daysStudied)
        assertEquals(10L, insights.wordsMasteredCount)
    }

    @Test
    fun `getStudyInsights returns failure on remote error`() = runTest {
        val stats = FakeStatsDataSource(
            insightsResult = Try.failure(RuntimeException("Network error"))
        )
        val repo = makeRepo(stats = stats)

        val result = repo.getStudyInsights()

        assertTrue(result.isFailure)
    }

    @Test
    fun `getDifficultWords maps response correctly`() = runTest {
        val words = FakeWordDataSource(
            difficultWordsResult = Try.success(
                listOf(
                    DifficultWordResponse(
                        wordId = 1, wordText = "hello", wordTranslation = "hola",
                        sourceLanguage = "EN", targetLanguage = "ES",
                        totalReviews = 10, errorCount = 4, errorRate = 0.4,
                    )
                )
            )
        )
        val repo = makeRepo(words = words)

        val result = repo.getDifficultWords(3, 20)

        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertEquals(1, list.size)
        assertEquals("hello", list.first().wordText)
        assertEquals(0.4, list.first().errorRate)
    }

    @Test
    fun `getAccuracyByLevel maps response correctly`() = runTest {
        val words = FakeWordDataSource(
            accuracyByLevelResult = Try.success(
                listOf(
                    AccuracyByLevelResponse(level = 0, totalReviews = 20, correctCount = 15, accuracyPercent = 75.0),
                    AccuracyByLevelResponse(level = 1, totalReviews = 10, correctCount = 9, accuracyPercent = 90.0),
                )
            )
        )
        val repo = makeRepo(words = words)

        val result = repo.getAccuracyByLevel()

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
        assertEquals(75.0, result.getOrThrow().first().accuracyPercent)
    }

    @Test
    fun `syncToBackend returns 0 since no local data`() = runTest {
        val repo = makeRepo()

        val result = repo.syncToBackend()

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
    }
}
