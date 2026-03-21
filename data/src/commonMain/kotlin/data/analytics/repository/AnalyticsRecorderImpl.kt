package data.analytics.repository

import core.common.Try
import data.analytics.remote.IAnalyticsStatsDataSource
import data.analytics.remote.model.SyncAnalyticsRequest
import data.analytics.remote.model.SyncReviewEventRequest
import data.analytics.remote.model.SyncSessionRequest
import domain.analytics.model.ReviewEventParams
import domain.analytics.repository.IAnalyticsRecorder
import expects.logNetwork
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory analytics recorder that buffers events during a session
 * and sends everything to the backend when the session ends.
 * Failed syncs are added to a retry queue and re-attempted on the next session end.
 */
class AnalyticsRecorderImpl(
    private val remoteDataSource: IAnalyticsStatsDataSource,
) : IAnalyticsRecorder {

    private data class SessionData(
        val sessionId: String,
        val reviewType: String,
        val startedAt: Long,
        val events: MutableList<SyncReviewEventRequest> = mutableListOf(),
    )

    private val mutex = Mutex()
    private val sessions: MutableMap<String, SessionData> = mutableMapOf()
    private val retryQueue: MutableList<SyncAnalyticsRequest> = mutableListOf()

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
        val (session, pending) = mutex.withLock {
            val s = sessions.remove(sessionId) ?: return Try.success(Unit)
            val p = retryQueue.toList().also { retryQueue.clear() }
            s to p
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

        // Merge retry sessions + current session into one request (backend accepts a list)
        val allSessions = pending.flatMap { it.sessions } + newRequest.sessions
        val combinedRequest = SyncAnalyticsRequest(sessions = allSessions)

        return remoteDataSource.syncSessions(combinedRequest).let { result ->
            when (result) {
                is Try.Success -> {
                    logNetwork(
                        "AnalyticsRecorder",
                        "Session ${session.sessionId} sent to backend (retried ${pending.size} pending)",
                    )
                    Try.success(Unit)
                }
                is Try.Failure -> {
                    logNetwork(
                        "AnalyticsRecorder",
                        "Failed to send session: ${result.throwable.message}. Queuing for retry.",
                    )
                    // Keep current session in retry queue — do not block the user
                    mutex.withLock { retryQueue.addAll(pending + listOf(newRequest)) }
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
