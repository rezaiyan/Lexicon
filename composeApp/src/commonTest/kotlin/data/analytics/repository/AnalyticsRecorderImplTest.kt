package data.analytics.repository

import core.common.Try
import data.analytics.local.IAnalyticsLocalQueue
import data.analytics.remote.IAnalyticsStatsDataSource
import data.analytics.remote.model.DailyStatsRemoteResponse
import data.analytics.remote.model.HeatmapDayResponse
import data.analytics.remote.model.MonthlyStatsResponse
import data.analytics.remote.model.ResponseTimeTrendRemoteResponse
import data.analytics.remote.model.StudyInsightsResponse
import data.analytics.remote.model.StudySessionResponse
import data.analytics.remote.model.SyncAnalyticsRequest
import data.analytics.remote.model.SyncAnalyticsResponse
import data.analytics.remote.model.WeeklyReportRemoteResponse
import domain.analytics.model.ReviewEventParams
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsRecorderImplTest {

    private class FakeAnalyticsLocalQueue : IAnalyticsLocalQueue {
        val requests = mutableListOf<String>()

        override suspend fun insertRequest(requestJson: String, createdAt: Long) {
            requests.add(requestJson)
        }

        override suspend fun getAllRequests(): List<String> = requests.toList()

        override suspend fun clearQueue() {
            requests.clear()
        }
    }

    private class FakeStatsDataSource : IAnalyticsStatsDataSource {
        var syncResult: Try<SyncAnalyticsResponse> = Try.success(SyncAnalyticsResponse(emptyList()))
        val syncRequests = mutableListOf<SyncAnalyticsRequest>()

        override suspend fun syncSessions(request: SyncAnalyticsRequest): Try<SyncAnalyticsResponse> {
            syncRequests.add(request)
            return syncResult
        }

        override suspend fun getInsights() = Try.success(StudyInsightsResponse())
        override suspend fun getRecentSessions(limit: Int) = Try.success(emptyList<StudySessionResponse>())
        override suspend fun getHeatmap(startMs: Long, endMs: Long) = Try.success(emptyList<HeatmapDayResponse>())
        override suspend fun getMonthlyStats() = Try.success(emptyList<MonthlyStatsResponse>())
        override suspend fun getDailyStats(start: String, end: String) = Try.success(emptyList<DailyStatsRemoteResponse>())
        override suspend fun getWeeklyReport() = Try.success(WeeklyReportRemoteResponse())
        override suspend fun getResponseTimeTrend() = Try.success(emptyList<ResponseTimeTrendRemoteResponse>())
    }

    private val defaultEvent = ReviewEventParams(
        sessionId = "s-1",
        wordId = 42,
        wordText = "hello",
        wordTranslation = "hola",
        sourceLanguage = "EN",
        targetLanguage = "ES",
        rating = 1,
        previousLevel = 2,
        newLevel = 3,
        responseTimeMs = 1500,
        reviewedAt = 2000L,
    )

    private fun recorder(
        remote: FakeStatsDataSource = FakeStatsDataSource(),
        queue: FakeAnalyticsLocalQueue = FakeAnalyticsLocalQueue(),
    ) = AnalyticsRecorderImpl(remoteDataSource = remote, localQueue = queue)

    // ── Existing tests ────────────────────────────────────────────────────────

    @Test
    fun `startSession buffers session in memory`() = runTest {
        val remote = FakeStatsDataSource()
        val rec = recorder(remote = remote)

        val result = rec.startSession("s-1", "REVIEW", 1000L)

        assertTrue(result.isSuccess)
        assertTrue(remote.syncRequests.isEmpty())
    }

    @Test
    fun `recordReviewEvent buffers events in memory`() = runTest {
        val remote = FakeStatsDataSource()
        val rec = recorder(remote = remote)
        rec.startSession("s-1", "REVIEW", 1000L)

        rec.recordReviewEvent(defaultEvent)

        assertTrue(remote.syncRequests.isEmpty())
    }

    @Test
    fun `endSession sends buffered session and events to backend`() = runTest {
        val remote = FakeStatsDataSource()
        remote.syncResult = Try.success(SyncAnalyticsResponse(listOf("s-1")))
        val rec = recorder(remote = remote)

        rec.startSession("s-1", "REVIEW", 1000L)
        rec.recordReviewEvent(defaultEvent)
        rec.recordReviewEvent(
            defaultEvent.copy(
                wordId = 43, wordText = "world", wordTranslation = "mundo",
                rating = 0, previousLevel = 1, newLevel = 0,
                responseTimeMs = 2000, reviewedAt = 3000L,
            )
        )
        val result = rec.endSession("s-1", 5000L, 4000L, 2, 1, 1, true)

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
        val remote = FakeStatsDataSource()
        remote.syncResult = Try.failure(RuntimeException("Network error"))
        val rec = recorder(remote = remote)

        rec.startSession("s-1", "REVIEW", 1000L)
        rec.recordReviewEvent(defaultEvent)
        val result = rec.endSession("s-1", 5000L, 4000L, 1, 1, 0, true)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `endSession without startSession is no-op`() = runTest {
        val remote = FakeStatsDataSource()
        val rec = recorder(remote = remote)

        val result = rec.endSession("s-unknown", 5000L, 4000L, 0, 0, 0, false)

        assertTrue(result.isSuccess)
        assertTrue(remote.syncRequests.isEmpty())
    }

    // ── Empty session skip tests ───────────────────────────────────────────────

    @Test
    fun `endSession skips insert and sync for 0-card 0-event session`() = runTest {
        val remote = FakeStatsDataSource()
        val queue = FakeAnalyticsLocalQueue()
        val rec = recorder(remote = remote, queue = queue)

        rec.startSession("s-empty", "REVIEW", 1000L)
        val result = rec.endSession("s-empty", 2000L, 1000L, 0, 0, 0, false)

        assertTrue(result.isSuccess)
        assertTrue(remote.syncRequests.isEmpty(), "Empty session should not be sent to backend")
        assertTrue(queue.requests.isEmpty(), "Empty session should not be stored in queue")
    }

    @Test
    fun `endSession still sends when totalCards is 0 but events exist`() = runTest {
        val remote = FakeStatsDataSource()
        val rec = recorder(remote = remote)

        rec.startSession("s-1", "REVIEW", 1000L)
        rec.recordReviewEvent(defaultEvent)
        // totalCards=0 but we have an event — should still sync
        val result = rec.endSession("s-1", 5000L, 4000L, 0, 0, 0, true)

        assertTrue(result.isSuccess)
        assertEquals(1, remote.syncRequests.size)
    }

    // ── retryPendingSync tests ────────────────────────────────────────────────

    @Test
    fun `retryPendingSync is no-op when queue is empty`() = runTest {
        val remote = FakeStatsDataSource()
        val rec = recorder(remote = remote)

        val result = rec.retryPendingSync()

        assertTrue(result.isSuccess)
        assertTrue(remote.syncRequests.isEmpty())
    }

    @Test
    fun `retryPendingSync sends queued sessions to backend`() = runTest {
        val remote = FakeStatsDataSource()
        val queue = FakeAnalyticsLocalQueue()
        val rec = recorder(remote = remote, queue = queue)

        // Simulate a session that was queued but never synced (e.g. app was killed)
        rec.startSession("s-1", "REVIEW", 1000L)
        rec.recordReviewEvent(defaultEvent)
        remote.syncResult = Try.failure(RuntimeException("offline")) // first sync fails
        rec.endSession("s-1", 5000L, 4000L, 1, 1, 0, true)
        remote.syncRequests.clear()

        // Now app restarts, comes back online
        remote.syncResult = Try.success(SyncAnalyticsResponse(listOf("s-1")))
        val result = rec.retryPendingSync()

        assertTrue(result.isSuccess)
        assertEquals(1, remote.syncRequests.size)
        assertEquals(1, remote.syncRequests.first().sessions.size)
        assertEquals("s-1", remote.syncRequests.first().sessions.first().clientSessionId)
    }

    @Test
    fun `retryPendingSync clears queue on success`() = runTest {
        val remote = FakeStatsDataSource()
        val queue = FakeAnalyticsLocalQueue()
        val rec = recorder(remote = remote, queue = queue)

        rec.startSession("s-1", "REVIEW", 1000L)
        rec.recordReviewEvent(defaultEvent)
        remote.syncResult = Try.failure(RuntimeException("offline"))
        rec.endSession("s-1", 5000L, 4000L, 1, 1, 0, true)

        remote.syncResult = Try.success(SyncAnalyticsResponse(listOf("s-1")))
        rec.retryPendingSync()

        assertTrue(queue.requests.isEmpty(), "Queue should be cleared after successful retry")
    }

    @Test
    fun `retryPendingSync keeps queue on network failure`() = runTest {
        val remote = FakeStatsDataSource()
        val queue = FakeAnalyticsLocalQueue()
        val rec = recorder(remote = remote, queue = queue)

        rec.startSession("s-1", "REVIEW", 1000L)
        rec.recordReviewEvent(defaultEvent)
        remote.syncResult = Try.failure(RuntimeException("offline"))
        rec.endSession("s-1", 5000L, 4000L, 1, 1, 0, true)
        val queueSizeBefore = queue.requests.size

        // Still offline
        rec.retryPendingSync()

        assertEquals(queueSizeBefore, queue.requests.size, "Queue should be preserved when retry fails")
    }
}
