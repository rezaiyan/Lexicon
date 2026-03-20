package domain.analytics.usecase

import core.common.Try
import core.common.getOrThrow
import domain.analytics.model.AccuracyByLevel
import domain.analytics.model.ComebackWord
import domain.analytics.model.DailyStudyStats
import domain.analytics.model.DayOfWeekAccuracy
import domain.analytics.model.HourlyAccuracy
import domain.analytics.model.LanguagePairStats
import domain.analytics.model.MasteredWord
import domain.analytics.model.MonthlyStats
import domain.analytics.model.MostReviewedWord
import domain.analytics.model.ReviewEventParams
import domain.analytics.model.StudyHeatmapDay
import domain.analytics.model.StudyInsights
import domain.analytics.model.StudySession
import domain.analytics.model.WeeklyReport
import domain.analytics.model.WordDifficulty
import domain.analytics.repository.IAnalyticsRecorder
import domain.analytics.repository.IAnalyticsStatsRepository
import domain.analytics.repository.IAnalyticsWordRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnalyticsUseCaseTests {

    // region Fakes

    private class FakeAnalyticsRecorder : IAnalyticsRecorder {
        var startSessionResult: Try<Unit> = Try.success(Unit)
        var endSessionResult: Try<Unit> = Try.success(Unit)
        var recordReviewEventResult: Try<Unit> = Try.success(Unit)

        var lastStartSessionId: String? = null
        var lastStartReviewType: String? = null
        var lastEndSessionId: String? = null
        var lastRecordSessionId: String? = null
        var lastRecordWordId: Int? = null

        override suspend fun startSession(
            sessionId: String,
            reviewType: String,
            startedAt: Long,
        ): Try<Unit> {
            lastStartSessionId = sessionId
            lastStartReviewType = reviewType
            return startSessionResult
        }

        override suspend fun endSession(
            sessionId: String,
            endedAt: Long,
            durationMs: Long,
            totalCards: Int,
            correctCount: Int,
            incorrectCount: Int,
            completedNormally: Boolean,
        ): Try<Unit> {
            lastEndSessionId = sessionId
            return endSessionResult
        }

        override suspend fun recordReviewEvent(params: ReviewEventParams): Try<Unit> {
            lastRecordSessionId = params.sessionId
            lastRecordWordId = params.wordId
            return recordReviewEventResult
        }
    }

    private class FakeAnalyticsRepository : IAnalyticsStatsRepository, IAnalyticsWordRepository {
        var studyInsightsResult: Try<StudyInsights> = Try.success(
            StudyInsights(
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
        )
        var dailyStatsResult: Try<List<DailyStudyStats>> = Try.success(emptyList())
        var difficultWordsResult: Try<List<WordDifficulty>> = Try.success(emptyList())
        var mostReviewedWordsResult: Try<List<MostReviewedWord>> = Try.success(emptyList())
        var accuracyByLevelResult: Try<List<AccuracyByLevel>> = Try.success(emptyList())
        var accuracyByHourResult: Try<List<HourlyAccuracy>> = Try.success(emptyList())
        var accuracyByDayOfWeekResult: Try<List<DayOfWeekAccuracy>> = Try.success(emptyList())
        var recentSessionsResult: Try<List<StudySession>> = Try.success(emptyList())
        var heatmapResult: Try<List<StudyHeatmapDay>> = Try.success(emptyList())
        var wordsMasteredResult: Try<List<MasteredWord>> = Try.success(emptyList())
        var languagePairStatsResult: Try<List<LanguagePairStats>> = Try.success(emptyList())
        var monthlyStatsResult: Try<List<MonthlyStats>> = Try.success(emptyList())
        var comebackWordsResult: Try<List<ComebackWord>> = Try.success(emptyList())
        // IAnalyticsStatsRepository
        override suspend fun getStudyInsights(): Try<StudyInsights> = studyInsightsResult
        override suspend fun getDailyStats(startDate: String, endDate: String): Try<List<DailyStudyStats>> = dailyStatsResult
        override suspend fun getRecentSessions(limit: Int): Try<List<StudySession>> = recentSessionsResult
        override suspend fun getStudyHeatmap(startDate: String, endDate: String): Try<List<StudyHeatmapDay>> = heatmapResult
        override suspend fun getWeeklyReport(): Try<WeeklyReport> = Try.success(
            WeeklyReport(0, 0, null, 0.0, 0, 0, 0, null, "", "")
        )
        override suspend fun getMonthlyStats(): Try<List<MonthlyStats>> = monthlyStatsResult
        override suspend fun syncToBackend(): Try<Int> = Try.success(0)

        // IAnalyticsWordRepository
        override suspend fun getDifficultWords(minReviews: Int, limit: Int): Try<List<WordDifficulty>> = difficultWordsResult
        override suspend fun getMostReviewedWords(limit: Int): Try<List<MostReviewedWord>> = mostReviewedWordsResult
        override suspend fun getAccuracyByLevel(): Try<List<AccuracyByLevel>> = accuracyByLevelResult
        override suspend fun getAccuracyByHourOfDay(): Try<List<HourlyAccuracy>> = accuracyByHourResult
        override suspend fun getAccuracyByDayOfWeek(): Try<List<DayOfWeekAccuracy>> = accuracyByDayOfWeekResult
        override suspend fun getWordsMastered(limit: Int): Try<List<MasteredWord>> = wordsMasteredResult
        override suspend fun getLanguagePairStats(): Try<List<LanguagePairStats>> = languagePairStatsResult
        override suspend fun getComebackWords(): Try<List<ComebackWord>> = comebackWordsResult
    }

    // endregion

    // region StartStudySessionUseCase

    @Test
    fun `StartStudySessionUseCase - success returns sessionId`() = runTest {
        val recorder = FakeAnalyticsRecorder()
        val useCase = StartStudySessionUseCase(recorder)

        val result = useCase(StartStudySessionUseCase.Params(sessionId = "session-1", reviewType = "flashcard"))

        assertTrue(result.isSuccess)
        assertEquals("session-1", result.getOrThrow())
        assertEquals("session-1", recorder.lastStartSessionId)
        assertEquals("flashcard", recorder.lastStartReviewType)
    }

    @Test
    fun `StartStudySessionUseCase - failure propagates`() = runTest {
        val recorder = FakeAnalyticsRecorder()
        recorder.startSessionResult = Try.failure(RuntimeException("DB error"))
        val useCase = StartStudySessionUseCase(recorder)

        val result = useCase(StartStudySessionUseCase.Params(sessionId = "session-1", reviewType = "flashcard"))

        assertTrue(result.isFailure)
    }

    // endregion

    // region EndStudySessionUseCase

    @Test
    fun `EndStudySessionUseCase - success ends session`() = runTest {
        val recorder = FakeAnalyticsRecorder()
        val useCase = EndStudySessionUseCase(recorder)

        val result = useCase(
            EndStudySessionUseCase.Params(
                sessionId = "session-1",
                endedAt = 1000L,
                durationMs = 500L,
                totalCards = 10,
                correctCount = 8,
                incorrectCount = 2,
                completedNormally = true,
            )
        )

        assertTrue(result.isSuccess)
        assertEquals("session-1", recorder.lastEndSessionId)
    }

    @Test
    fun `EndStudySessionUseCase - recorder failure propagates`() = runTest {
        val recorder = FakeAnalyticsRecorder()
        recorder.endSessionResult = Try.failure(RuntimeException("DB error"))
        val useCase = EndStudySessionUseCase(recorder)

        val result = useCase(
            EndStudySessionUseCase.Params(
                sessionId = "session-1",
                endedAt = 1000L,
                durationMs = 500L,
                totalCards = 10,
                correctCount = 8,
                incorrectCount = 2,
                completedNormally = true,
            )
        )

        assertTrue(result.isFailure)
    }

    // endregion

    // region RecordReviewEventUseCase

    @Test
    fun `RecordReviewEventUseCase - success records event`() = runTest {
        val recorder = FakeAnalyticsRecorder()
        val useCase = RecordReviewEventUseCase(recorder)

        val result = useCase(
            ReviewEventParams(
                sessionId = "session-1",
                wordId = 42,
                wordText = "hello",
                wordTranslation = "hola",
                sourceLanguage = "EN",
                targetLanguage = "ES",
                rating = 4,
                previousLevel = 2,
                newLevel = 3,
                responseTimeMs = 1500,
                reviewedAt = 1000L,
            )
        )

        assertTrue(result.isSuccess)
        assertEquals("session-1", recorder.lastRecordSessionId)
        assertEquals(42, recorder.lastRecordWordId)
    }

    @Test
    fun `RecordReviewEventUseCase - failure propagates`() = runTest {
        val recorder = FakeAnalyticsRecorder()
        recorder.recordReviewEventResult = Try.failure(RuntimeException("DB error"))
        val useCase = RecordReviewEventUseCase(recorder)

        val result = useCase(
            ReviewEventParams(
                sessionId = "session-1",
                wordId = 42,
                wordText = "hello",
                wordTranslation = "hola",
                sourceLanguage = "EN",
                targetLanguage = "ES",
                rating = 4,
                previousLevel = 2,
                newLevel = 3,
                responseTimeMs = 1500,
                reviewedAt = 1000L,
            )
        )

        assertTrue(result.isFailure)
    }

    // endregion

    // region GetStudyInsightsUseCase

    @Test
    fun `GetStudyInsightsUseCase - delegates to repository`() = runTest {
        val repository = FakeAnalyticsRepository()
        val useCase = GetStudyInsightsUseCase(repository)

        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        assertEquals(100, result.getOrThrow().totalCardsReviewed)
        assertEquals(80.0, result.getOrThrow().accuracyPercent)
    }

    @Test
    fun `GetStudyInsightsUseCase - failure propagates`() = runTest {
        val repository = FakeAnalyticsRepository()
        repository.studyInsightsResult = Try.failure(RuntimeException("DB error"))
        val useCase = GetStudyInsightsUseCase(repository)

        val result = useCase(Unit)

        assertTrue(result.isFailure)
    }

    // endregion

    // region GetBestStudyTimeUseCase

    @Test
    fun `GetBestStudyTimeUseCase - returns highest accuracy hour with min 5 reviews`() = runTest {
        val repository = FakeAnalyticsRepository()
        repository.accuracyByHourResult = Try.success(
            listOf(
                HourlyAccuracy(hour = 9, totalReviews = 10, correctCount = 8, accuracyPercent = 80.0),
                HourlyAccuracy(hour = 14, totalReviews = 20, correctCount = 19, accuracyPercent = 95.0),
                HourlyAccuracy(hour = 21, totalReviews = 3, correctCount = 3, accuracyPercent = 100.0),
            )
        )
        val useCase = GetBestStudyTimeUseCase(repository)

        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        val bestTime = result.getOrThrow()
        assertEquals(14, bestTime?.hour)
        assertEquals(95.0, bestTime?.accuracyPercent)
    }

    @Test
    fun `GetBestStudyTimeUseCase - returns null when no hours have min 5 reviews`() = runTest {
        val repository = FakeAnalyticsRepository()
        repository.accuracyByHourResult = Try.success(
            listOf(
                HourlyAccuracy(hour = 9, totalReviews = 2, correctCount = 2, accuracyPercent = 100.0),
                HourlyAccuracy(hour = 14, totalReviews = 4, correctCount = 3, accuracyPercent = 75.0),
            )
        )
        val useCase = GetBestStudyTimeUseCase(repository)

        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }

    @Test
    fun `GetBestStudyTimeUseCase - returns null when no data`() = runTest {
        val repository = FakeAnalyticsRepository()
        repository.accuracyByHourResult = Try.success(emptyList())
        val useCase = GetBestStudyTimeUseCase(repository)

        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }

    @Test
    fun `GetBestStudyTimeUseCase - failure propagates`() = runTest {
        val repository = FakeAnalyticsRepository()
        repository.accuracyByHourResult = Try.failure(RuntimeException("DB error"))
        val useCase = GetBestStudyTimeUseCase(repository)

        val result = useCase(Unit)

        assertTrue(result.isFailure)
    }

    // endregion
}
