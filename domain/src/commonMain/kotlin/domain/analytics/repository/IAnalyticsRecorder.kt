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
}
