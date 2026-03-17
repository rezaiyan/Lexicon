package data.analytics.repository

import core.common.Try
import data.analytics.remote.IAnalyticsRemoteDataSource
import data.analytics.remote.model.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsRecorderImplTest {

    private class FakeRemoteDataSource : IAnalyticsRemoteDataSource {
        var syncResult: Try<SyncAnalyticsResponse> = Try.success(SyncAnalyticsResponse(emptyList()))
        val syncRequests = mutableListOf<SyncAnalyticsRequest>()

        override suspend fun syncSessions(request: SyncAnalyticsRequest): Try<SyncAnalyticsResponse> {
            syncRequests.add(request)
            return syncResult
        }

        override suspend fun getInsights() = Try.success(StudyInsightsResponse())
        override suspend fun getDifficultWords(minReviews: Int, limit: Int) = Try.success(emptyList<DifficultWordResponse>())
        override suspend fun getMostReviewedWords(limit: Int) = Try.success(emptyList<MostReviewedWordResponse>())
        override suspend fun getAccuracyByLevel() = Try.success(emptyList<AccuracyByLevelResponse>())
        override suspend fun getAccuracyByHour() = Try.success(emptyList<HourlyAccuracyResponse>())
        override suspend fun getAccuracyByDayOfWeek() = Try.success(emptyList<DayOfWeekAccuracyResponse>())
        override suspend fun getRecentSessions(limit: Int) = Try.success(emptyList<StudySessionResponse>())
        override suspend fun getHeatmap(startMs: Long, endMs: Long) = Try.success(emptyList<HeatmapDayResponse>())
        override suspend fun getWordsMastered(limit: Int) = Try.success(emptyList<MasteredWordResponse>())
        override suspend fun getLanguageStats() = Try.success(emptyList<LanguagePairStatsResponse>())
        override suspend fun getMonthlyStats() = Try.success(emptyList<MonthlyStatsResponse>())
        override suspend fun getComebackWords() = Try.success(emptyList<ComebackWordResponse>())
        override suspend fun getDailyStats(start: String, end: String) = Try.success(emptyList<DailyStatsRemoteResponse>())
        override suspend fun getWeeklyReport() = Try.success(WeeklyReportRemoteResponse())
    }

    @Test
    fun `startSession buffers session in memory`() = runTest {
        val remote = FakeRemoteDataSource()
        val recorder = AnalyticsRecorderImpl(remote)

        val result = recorder.startSession("s-1", "REVIEW", 1000L)

        assertTrue(result.isSuccess)
        // Nothing sent to remote yet
        assertTrue(remote.syncRequests.isEmpty())
    }

    @Test
    fun `recordReviewEvent buffers events in memory`() = runTest {
        val remote = FakeRemoteDataSource()
        val recorder = AnalyticsRecorderImpl(remote)
        recorder.startSession("s-1", "REVIEW", 1000L)

        recorder.recordReviewEvent("s-1", 42, "hello", "hola", "EN", "ES", 1, 2, 3, 1500, 2000L)

        // Still nothing sent
        assertTrue(remote.syncRequests.isEmpty())
    }

    @Test
    fun `endSession sends buffered session and events to backend`() = runTest {
        val remote = FakeRemoteDataSource()
        remote.syncResult = Try.success(SyncAnalyticsResponse(listOf("s-1")))
        val recorder = AnalyticsRecorderImpl(remote)

        recorder.startSession("s-1", "REVIEW", 1000L)
        recorder.recordReviewEvent("s-1", 42, "hello", "hola", "EN", "ES", 1, 2, 3, 1500, 2000L)
        recorder.recordReviewEvent("s-1", 43, "world", "mundo", "EN", "ES", 0, 1, 0, 2000, 3000L)
        val result = recorder.endSession("s-1", 5000L, 4000L, 2, 1, 1, true)

        assertTrue(result.isSuccess)
        assertEquals(1, remote.syncRequests.size)

        val request = remote.syncRequests.first()
        assertEquals(1, request.sessions.size)

        val session = request.sessions.first()
        assertEquals("s-1", session.clientSessionId)
        assertEquals(1000L, session.startedAt)
        assertEquals(5000L, session.endedAt)
        assertEquals(4000L, session.durationMs)
        assertEquals(2, session.totalCards)
        assertEquals(1, session.correctCount)
        assertEquals(1, session.incorrectCount)
        assertEquals("REVIEW", session.reviewType)
        assertTrue(session.completedNormally)
        assertEquals(2, session.events.size)

        val event1 = session.events[0]
        assertEquals(42L, event1.wordId)
        assertEquals("hello", event1.wordText)
        assertEquals(1, event1.rating)
    }

    @Test
    fun `endSession succeeds even when remote fails`() = runTest {
        val remote = FakeRemoteDataSource()
        remote.syncResult = Try.failure(RuntimeException("Network error"))
        val recorder = AnalyticsRecorderImpl(remote)

        recorder.startSession("s-1", "REVIEW", 1000L)
        val result = recorder.endSession("s-1", 5000L, 4000L, 0, 0, 0, true)

        // Should not propagate failure — analytics are best-effort
        assertTrue(result.isSuccess)
    }

    @Test
    fun `endSession without startSession is no-op`() = runTest {
        val remote = FakeRemoteDataSource()
        val recorder = AnalyticsRecorderImpl(remote)

        val result = recorder.endSession("s-unknown", 5000L, 4000L, 0, 0, 0, false)

        assertTrue(result.isSuccess)
        assertTrue(remote.syncRequests.isEmpty())
    }
}
