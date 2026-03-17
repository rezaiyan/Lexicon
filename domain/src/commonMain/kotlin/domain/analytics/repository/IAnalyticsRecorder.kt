package domain.analytics.repository

import core.common.Try

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

    suspend fun recordReviewEvent(
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
    ): Try<Unit>
}
