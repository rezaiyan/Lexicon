package data.analytics.repository

import core.common.Try
import data.analytics.local.IAnalyticsLocalQueue
import data.analytics.remote.IAnalyticsStatsDataSource
import data.analytics.remote.model.SyncAnalyticsRequest
import data.analytics.remote.model.SyncReviewEventRequest
import data.analytics.remote.model.SyncSessionRequest
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
 * retried on the next session end or on app startup via [retryPendingSync].
 */
class AnalyticsRecorderImpl(
    private val remoteDataSource: IAnalyticsStatsDataSource,
    private val localQueue: IAnalyticsLocalQueue,
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

        // Skip empty sessions — no cards reviewed and no events buffered.
        // These happen when the user navigates away immediately (onCleared with 0 activity).
        // Storing them would pollute the backend with zero-value rows.
        if (totalCards == 0 && session.events.isEmpty()) {
            logNetwork(
                "AnalyticsRecorder",
                "Skipping empty session ${session.sessionId} (0 cards, 0 events)",
            )
            return Try.success(Unit)
        }

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

        // Persist to local queue before attempting sync so the session survives an app restart on failure
        localQueue.insertRequest(
            requestJson = json.encodeToString(newRequest),
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )

        return syncQueueToBackend(context = "endSession(${session.sessionId})")
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

    /**
     * Retries any sessions that failed to sync in a previous run.
     * Should be called on app startup (after authentication) so data never stays stuck in the queue.
     */
    override suspend fun retryPendingSync(): Try<Unit> {
        val pending = localQueue.getAllRequests()
        if (pending.isEmpty()) {
            logNetwork("AnalyticsRecorder", "retryPendingSync: queue is empty, nothing to retry")
            return Try.success(Unit)
        }
        logNetwork("AnalyticsRecorder", "retryPendingSync: ${pending.size} pending request(s) found")
        return syncQueueToBackend(context = "retryPendingSync")
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private suspend fun syncQueueToBackend(context: String): Try<Unit> {
        val allRequestJsons = localQueue.getAllRequests()
        val allSessions = allRequestJsons
            .mapNotNull { requestJson ->
                runCatching { json.decodeFromString<SyncAnalyticsRequest>(requestJson) }
                    .getOrNull()
            }
            .flatMap { it.sessions }

        if (allSessions.isEmpty()) return Try.success(Unit)

        val combinedRequest = SyncAnalyticsRequest(sessions = allSessions)

        return remoteDataSource.syncSessions(combinedRequest).let { result ->
            when (result) {
                is Try.Success -> {
                    localQueue.clearQueue()
                    logNetwork(
                        "AnalyticsRecorder",
                        "[$context] Synced ${allSessions.size} session(s) to backend",
                    )
                    Try.success(Unit)
                }
                is Try.Failure -> {
                    logNetwork(
                        "AnalyticsRecorder",
                        "[$context] Sync failed: ${result.throwable.message}. Kept in queue for retry.",
                    )
                    Try.success(Unit)
                }
            }
        }
    }
}
