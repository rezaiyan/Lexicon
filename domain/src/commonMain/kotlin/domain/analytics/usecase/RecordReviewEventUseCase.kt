package domain.analytics.usecase

import core.common.Try
import core.common.UseCase
import domain.analytics.repository.IAnalyticsRecorder

class RecordReviewEventUseCase(
    private val analyticsRecorder: IAnalyticsRecorder,
) : UseCase<RecordReviewEventUseCase.Params, Unit> {

    data class Params(
        val sessionId: String,
        val wordId: Int,
        val wordText: String,
        val wordTranslation: String,
        val sourceLanguage: String,
        val targetLanguage: String,
        val rating: Int,
        val previousLevel: Int,
        val newLevel: Int,
        val responseTimeMs: Long,
        val reviewedAt: Long,
    )

    override suspend fun invoke(params: Params): Try<Unit> {
        return analyticsRecorder.recordReviewEvent(
            sessionId = params.sessionId,
            wordId = params.wordId,
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
    }
}
