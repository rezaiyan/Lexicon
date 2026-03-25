package domain.analytics.repository

import core.common.Try
import domain.analytics.model.ReviewEventParams

interface IAnalyticsRecorder {
    suspend fun startSession(
        sessionId: String,
        reviewType: String,
        startedAt: Long,
    ): Try<Unit>

    suspend fun endSession(
        sessionId: String,
        endedAt: Long,
        durationMs: Long,
        totalCards: Int,
        correctCount: Int,
        incorrectCount: Int,
        completedNormally: Boolean,
    ): Try<Unit>

    suspend fun recordReviewEvent(params: ReviewEventParams): Try<Unit>

    /**
     * Retries any study sessions that are sitting in the local queue from previous failed syncs.
     * Safe to call on app startup — no-op when the queue is empty.
     */
    suspend fun retryPendingSync(): Try<Unit>
}
