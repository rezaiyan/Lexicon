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
import data.analytics.remote.model.SyncSessionRequest
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

    @Test
    fun `startSession buffers session in memory`() = runTest {
        val remote = FakeStatsDataSource()
        val recorder = AnalyticsRecorderImpl(remoteDataSource = remote, localQueue = FakeAnalyticsLocalQueue())

        val result = recorder.startSession("s-1", "REVIEW", 1000L)

        assertTrue(result.isSuccess)
        // Nothing sent to remote yet
        assertTrue(remote.syncRequests.isEmpty())
    }

    @Test
    fun `recordReviewEvent buffers events in memory`() = runTest {
        val remote = FakeStatsDataSource()
        val recorder = AnalyticsRecorderImpl(remoteDataSource = remote, localQueue = FakeAnalyticsLocalQueue())
        recorder.startSession("s-1", "REVIEW", 1000L)

        recorder.recordReviewEvent(defaultEvent)

        // Still nothing sent
        assertTrue(remote.syncRequests.isEmpty())
    }

    @Test
    fun `endSession sends buffered session and events to backend`() = runTest {
        val remote = FakeStatsDataSource()
        remote.syncResult = Try.success(SyncAnalyticsResponse(listOf("s-1")))
        val recorder = AnalyticsRecorderImpl(remoteDataSource = remote, localQueue = FakeAnalyticsLocalQueue())

        recorder.startSession("s-1", "REVIEW", 1000L)
        recorder.recordReviewEvent(defaultEvent)
        recorder.recordReviewEvent(
            defaultEvent.copy(
                wordId = 43, wordText = "world", wordTranslation = "mundo",
                rating = 0, previousLevel = 1, newLevel = 0,
                responseTimeMs = 2000, reviewedAt = 3000L,
            )
        )
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
        val remote = FakeStatsDataSource()
        remote.syncResult = Try.failure(RuntimeException("Network error"))
        val recorder = AnalyticsRecorderImpl(remoteDataSource = remote, localQueue = FakeAnalyticsLocalQueue())

        recorder.startSession("s-1", "REVIEW", 1000L)
        val result = recorder.endSession("s-1", 5000L, 4000L, 0, 0, 0, true)

        // Should not propagate failure — analytics are best-effort
        assertTrue(result.isSuccess)
    }

    @Test
    fun `endSession without startSession is no-op`() = runTest {
        val remote = FakeStatsDataSource()
        val recorder = AnalyticsRecorderImpl(remoteDataSource = remote, localQueue = FakeAnalyticsLocalQueue())

        val result = recorder.endSession("s-unknown", 5000L, 4000L, 0, 0, 0, false)

        assertTrue(result.isSuccess)
        assertTrue(remote.syncRequests.isEmpty())
    }

    // ─── retryPendingSync ────────────────────────────────────────────────────────

    @Test
    fun `retryPendingSync is no-op when queue is empty`() = runTest {
        val remote = FakeStatsDataSource()
        val recorder = AnalyticsRecorderImpl(remoteDataSource = remote, localQueue = FakeAnalyticsLocalQueue())

        val result = recorder.retryPendingSync()

        assertTrue(result.isSuccess)
        assertTrue(remote.syncRequests.isEmpty())
    }

    @Test
    fun `retryPendingSync sends all queued sessions to backend`() = runTest {
        val queue = FakeAnalyticsLocalQueue()
        val remote = FakeStatsDataSource()
        remote.syncResult = Try.success(SyncAnalyticsResponse(listOf("s-1")))
        val recorder = AnalyticsRecorderImpl(remoteDataSource = remote, localQueue = queue)

        // Simulate a previously-failed sync by pre-populating the queue
        val request = SyncAnalyticsRequest(
            sessions = listOf(
                SyncSessionRequest(
                    clientSessionId = "s-1",
                    startedAt = 1000L,
                    endedAt = 5000L,
                    durationMs = 4000L,
                    totalCards = 3,
                    correctCount = 2,
                    incorrectCount = 1,
                    reviewType = "REVIEW",
                    completedNormally = true,
                    events = emptyList(),
                )
            )
        )
        val json = kotlinx.serialization.json.Json.encodeToString(request)
        queue.insertRequest(json, 1000L)

        val result = recorder.retryPendingSync()

        assertTrue(result.isSuccess)
        assertEquals(1, remote.syncRequests.size)
        assertEquals(1, remote.syncRequests.first().sessions.size)
        assertEquals("s-1", remote.syncRequests.first().sessions.first().clientSessionId)
    }

    @Test
    fun `retryPendingSync clears queue on success`() = runTest {
        val queue = FakeAnalyticsLocalQueue()
        val remote = FakeStatsDataSource()
        remote.syncResult = Try.success(SyncAnalyticsResponse(listOf("s-1")))
        val recorder = AnalyticsRecorderImpl(remoteDataSource = remote, localQueue = queue)

        val request = SyncAnalyticsRequest(
            sessions = listOf(
                SyncSessionRequest(
                    clientSessionId = "s-1",
                    startedAt = 1000L,
                    endedAt = 5000L,
                    durationMs = 4000L,
                    totalCards = 2,
                    correctCount = 2,
                    incorrectCount = 0,
                    reviewType = "REVIEW",
                    completedNormally = true,
                    events = emptyList(),
                )
            )
        )
        queue.insertRequest(kotlinx.serialization.json.Json.encodeToString(request), 1000L)

        recorder.retryPendingSync()

        assertTrue(queue.requests.isEmpty())
    }

    @Test
    fun `retryPendingSync keeps queue intact on network failure`() = runTest {
        val queue = FakeAnalyticsLocalQueue()
        val remote = FakeStatsDataSource()
        remote.syncResult = Try.failure(RuntimeException("Network error"))
        val recorder = AnalyticsRecorderImpl(remoteDataSource = remote, localQueue = queue)

        val request = SyncAnalyticsRequest(
            sessions = listOf(
                SyncSessionRequest(
                    clientSessionId = "s-1",
                    startedAt = 1000L,
                    endedAt = 5000L,
                    durationMs = 4000L,
                    totalCards = 2,
                    correctCount = 2,
                    incorrectCount = 0,
                    reviewType = "REVIEW",
                    completedNormally = true,
                    events = emptyList(),
                )
            )
        )
        queue.insertRequest(kotlinx.serialization.json.Json.encodeToString(request), 1000L)

        val result = recorder.retryPendingSync()

        // Should not propagate failure — analytics are best-effort
        assertTrue(result.isSuccess)
        // Queue must still have the item so it can be retried next time
        assertEquals(1, queue.requests.size)
    }

    // ─── empty session skip ──────────────────────────────────────────────────────

    @Test
    fun `endSession skips queue insert for 0-card 0-event session`() = runTest {
        val queue = FakeAnalyticsLocalQueue()
        val remote = FakeStatsDataSource()
        val recorder = AnalyticsRecorderImpl(remoteDataSource = remote, localQueue = queue)

        recorder.startSession("s-empty", "REVIEW", 1000L)
        // No reviewEvents recorded — user backed out immediately
        val result = recorder.endSession("s-empty", 5000L, 4000L, 0, 0, 0, false)

        assertTrue(result.isSuccess)
        assertTrue(queue.requests.isEmpty())
        assertTrue(remote.syncRequests.isEmpty())
    }

    @Test
    fun `endSession still queues session with 0 cards when it has events`() = runTest {
        val queue = FakeAnalyticsLocalQueue()
        val remote = FakeStatsDataSource()
        remote.syncResult = Try.success(SyncAnalyticsResponse(listOf("s-1")))
        val recorder = AnalyticsRecorderImpl(remoteDataSource = remote, localQueue = queue)

        recorder.startSession("s-1", "REVIEW", 1000L)
        recorder.recordReviewEvent(defaultEvent.copy(sessionId = "s-1"))
        // totalCards param is 0 but there are events — should still sync
        val result = recorder.endSession("s-1", 5000L, 4000L, 0, 0, 0, false)

        assertTrue(result.isSuccess)
        assertEquals(1, remote.syncRequests.size)
    }
}
