package data.analytics.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import core.common.Try
import data.analytics.remote.IAnalyticsStatsDataSource
import data.analytics.remote.model.SyncAnalyticsRequest
import data.analytics.remote.model.SyncReviewEventRequest
import data.analytics.remote.model.SyncSessionRequest
import data.core.database.LexiconQueries
import domain.analytics.model.ReviewEventParams
import domain.analytics.repository.IAnalyticsRecorder
import expects.logNetwork
import kotlin.time.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Analytics recorder that buffers events during a session and syncs to the backend when the
 * session ends. Failed syncs are persisted to SQLDelight so they survive app restarts and are
 * retried on the next session end.
 */
class AnalyticsRecorderImpl(
    private val remoteDataSource: IAnalyticsStatsDataSource,
    private val queries: LexiconQueries,
) : IAnalyticsRecorder {

    private data class SessionData(
        val sessionId: String,
        val reviewType: String,
        val startedAt: Long,
        val events: MutableList<SyncReviewEventRequest> = mutableListOf(),
    )

    private val mutex = Mutex()
    private val sessions: MutableMap<String, SessionData> = mutableMapOf()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun startSession(
        sessionId: String,
        reviewType: String,
        startedAt: Long,
    ): Try<Unit> {
        mutex.withLock {
            sessions[sessionId] = SessionData(
                sessionId = sessionId,
                reviewType = reviewType,
                startedAt = startedAt,
            )
        }
        return Try.success(Unit)
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
        val session = mutex.withLock { sessions.remove(sessionId) } ?: return Try.success(Unit)

        val newRequest = SyncAnalyticsRequest(
            sessions = listOf(
                SyncSessionRequest(
                    clientSessionId = session.sessionId,
                    startedAt = session.startedAt,
                    endedAt = endedAt,
                    durationMs = durationMs,
                    totalCards = totalCards,
                    correctCount = correctCount,
                    incorrectCount = incorrectCount,
                    reviewType = session.reviewType,
                    completedNormally = completedNormally,
                    events = session.events.toList(),
                )
            )
        )

        // Persist to DB before attempting sync so the session survives an app restart on failure
        queries.insertAnalyticsSyncRequest(
            requestJson = json.encodeToString(newRequest),
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )

        // Load all pending requests (includes the one just inserted + any from prior failures)
        val pendingItems = queries.getAllAnalyticsSyncRequests().awaitAsList()
        val allSessions = pendingItems
            .mapNotNull { item ->
                runCatching { json.decodeFromString<SyncAnalyticsRequest>(item.requestJson) }
                    .getOrNull()
            }
            .flatMap { it.sessions }

        val combinedRequest = SyncAnalyticsRequest(sessions = allSessions)

        return remoteDataSource.syncSessions(combinedRequest).let { result ->
            when (result) {
                is Try.Success -> {
                    queries.clearAnalyticsSyncQueue()
                    logNetwork(
                        "AnalyticsRecorder",
                        "Session ${session.sessionId} sent to backend (${pendingItems.size} total in batch)",
                    )
                    Try.success(Unit)
                }
                is Try.Failure -> {
                    logNetwork(
                        "AnalyticsRecorder",
                        "Failed to send session: ${result.throwable.message}. Persisted for retry on next session.",
                    )
                    Try.success(Unit)
                }
            }
        }
    }

    override suspend fun recordReviewEvent(params: ReviewEventParams): Try<Unit> {
        mutex.withLock {
            sessions[params.sessionId]?.events?.add(
                SyncReviewEventRequest(
                    wordId = params.wordId.toLong(),
                    wordText = params.wordText,
                    wordTranslation = params.wordTranslation,
                    sourceLanguage = params.sourceLanguage,
                    targetLanguage = params.targetLanguage,
                    rating = params.rating,
                    previousLevel = params.previousLevel,
                    newLevel = params.newLevel,
                    responseTimeMs = params.responseTimeMs,
                    reviewedAt = params.reviewedAt,
                )
            )
        }
        return Try.success(Unit)
    }
}
