package data.analytics.repository

import core.common.Try
import data.analytics.remote.IAnalyticsRemoteDataSource
import data.analytics.remote.model.SyncAnalyticsRequest
import data.analytics.remote.model.SyncReviewEventRequest
import data.analytics.remote.model.SyncSessionRequest
import domain.analytics.repository.IAnalyticsRecorder
import expects.logNetwork

/**
 * In-memory analytics recorder that buffers events during a session
 * and sends everything to the backend when the session ends.
 */
class AnalyticsRecorderImpl(
    private val remoteDataSource: IAnalyticsRemoteDataSource,
) : IAnalyticsRecorder {

    private data class SessionData(
        val sessionId: String,
        val reviewType: String,
        val startedAt: Long,
        val events: MutableList<SyncReviewEventRequest> = mutableListOf(),
    )

    private var activeSession: SessionData? = null

    override suspend fun startSession(
        sessionId: String,
        reviewType: String,
        startedAt: Long,
    ): Try<Unit> = Try {
        activeSession = SessionData(
            sessionId = sessionId,
            reviewType = reviewType,
            startedAt = startedAt,
        )
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
        val session = activeSession ?: return Try.success(Unit)
        activeSession = null

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

    override suspend fun recordReviewEvent(
        sessionId: String,
        wordId: Int,
        wordText: String,
        wordTranslation: String,
        sourceLanguage: String,
        targetLanguage: String,
        rating: Int,
        previousLevel: Int,
        newLevel: Int,
        responseTimeMs: Long,
        reviewedAt: Long,
    ): Try<Unit> = Try {
        activeSession?.events?.add(
            SyncReviewEventRequest(
                wordId = wordId.toLong(),
                wordText = wordText,
                wordTranslation = wordTranslation,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                rating = rating,
                previousLevel = previousLevel,
                newLevel = newLevel,
                responseTimeMs = responseTimeMs,
                reviewedAt = reviewedAt,
            )
        )
    }
}
