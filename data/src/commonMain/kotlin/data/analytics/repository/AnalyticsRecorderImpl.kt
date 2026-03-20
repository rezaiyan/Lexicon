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
        val session = mutex.withLock { sessions.remove(sessionId) }
            ?: return Try.success(Unit)

        val request = SyncAnalyticsRequest(
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

        return remoteDataSource.syncSessions(request).let { result ->
            when (result) {
                is Try.Success -> {
                    logNetwork("AnalyticsRecorder", "Session ${session.sessionId} sent to backend")
                    Try.success(Unit)
                }
                is Try.Failure -> {
                    logNetwork("AnalyticsRecorder", "Failed to send session: ${result.throwable.message}")
                    // Silently fail — analytics should not block the user
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
