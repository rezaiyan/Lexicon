package data.analytics.repository

import core.common.Try
import core.common.getOrThrow
import data.analytics.remote.IAnalyticsRemoteDataSource
import data.analytics.remote.model.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsRepositoryImplTest {

    private class FakeRemoteDataSource : IAnalyticsRemoteDataSource {
        var insightsResult: Try<StudyInsightsResponse> = Try.success(StudyInsightsResponse())
        var difficultWordsResult: Try<List<DifficultWordResponse>> = Try.success(emptyList())
        var accuracyByLevelResult: Try<List<AccuracyByLevelResponse>> = Try.success(emptyList())
        var accuracyByHourResult: Try<List<HourlyAccuracyResponse>> = Try.success(emptyList())

        override suspend fun syncSessions(request: SyncAnalyticsRequest) = Try.success(SyncAnalyticsResponse(emptyList()))
        override suspend fun getInsights() = insightsResult
        override suspend fun getDifficultWords(minReviews: Int, limit: Int) = difficultWordsResult
        override suspend fun getMostReviewedWords(limit: Int) = Try.success(emptyList<MostReviewedWordResponse>())
        override suspend fun getAccuracyByLevel() = accuracyByLevelResult
        override suspend fun getAccuracyByHour() = accuracyByHourResult
        override suspend fun getAccuracyByDayOfWeek() = Try.success(emptyList<DayOfWeekAccuracyResponse>())
        override suspend fun getRecentSessions(limit: Int) = Try.success(emptyList<StudySessionResponse>())
        override suspend fun getHeatmap(startMs: Long, endMs: Long) = Try.success(emptyList<HeatmapDayResponse>())
        override suspend fun getWordsMastered(limit: Int) = Try.success(emptyList<MasteredWordResponse>())
        override suspend fun getLanguageStats() = Try.success(emptyList<LanguagePairStatsResponse>())
        override suspend fun getMonthlyStats() = Try.success(emptyList<MonthlyStatsResponse>())
        override suspend fun getComebackWords() = Try.success(emptyList<ComebackWordResponse>())
    }

    @Test
    fun `getStudyInsights maps response to domain model`() = runTest {
        val remote = FakeRemoteDataSource()
        remote.insightsResult = Try.success(
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
        val repo = AnalyticsRepositoryImpl(remote)

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
        val remote = FakeRemoteDataSource()
        remote.insightsResult = Try.failure(RuntimeException("Network error"))
        val repo = AnalyticsRepositoryImpl(remote)

        val result = repo.getStudyInsights()

        assertTrue(result.isFailure)
    }

    @Test
    fun `getDifficultWords maps response correctly`() = runTest {
        val remote = FakeRemoteDataSource()
        remote.difficultWordsResult = Try.success(
            listOf(
                DifficultWordResponse(
                    wordId = 1, wordText = "hello", wordTranslation = "hola",
                    sourceLanguage = "EN", targetLanguage = "ES",
                    totalReviews = 10, errorCount = 4, errorRate = 0.4,
                )
            )
        )
        val repo = AnalyticsRepositoryImpl(remote)

        val result = repo.getDifficultWords(3, 20)

        assertTrue(result.isSuccess)
        val words = result.getOrThrow()
        assertEquals(1, words.size)
        assertEquals("hello", words.first().wordText)
        assertEquals(0.4, words.first().errorRate)
    }

    @Test
    fun `getAccuracyByLevel maps response correctly`() = runTest {
        val remote = FakeRemoteDataSource()
        remote.accuracyByLevelResult = Try.success(
            listOf(
                AccuracyByLevelResponse(level = 0, totalReviews = 20, correctCount = 15, accuracyPercent = 75.0),
                AccuracyByLevelResponse(level = 1, totalReviews = 10, correctCount = 9, accuracyPercent = 90.0),
            )
        )
        val repo = AnalyticsRepositoryImpl(remote)

        val result = repo.getAccuracyByLevel()

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
        assertEquals(75.0, result.getOrThrow().first().accuracyPercent)
    }

    @Test
    fun `syncToBackend returns 0 since no local data`() = runTest {
        val repo = AnalyticsRepositoryImpl(FakeRemoteDataSource())

        val result = repo.syncToBackend()

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
    }
}
