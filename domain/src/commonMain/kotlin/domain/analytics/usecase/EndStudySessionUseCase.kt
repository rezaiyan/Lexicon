package domain.analytics.usecase

import core.common.Try
import core.common.UseCase
import domain.analytics.repository.IAnalyticsRecorder

class EndStudySessionUseCase(
    private val analyticsRecorder: IAnalyticsRecorder,
) : UseCase<EndStudySessionUseCase.Params, Unit> {

    data class Params(
        val sessionId: String,
        val endedAt: Long,
        val durationMs: Long,
        val totalCards: Int,
        val correctCount: Int,
        val incorrectCount: Int,
        val completedNormally: Boolean,
    )

    override suspend fun invoke(params: Params): Try<Unit> =
        analyticsRecorder.endSession(
            sessionId = params.sessionId,
            endedAt = params.endedAt,
            durationMs = params.durationMs,
            totalCards = params.totalCards,
            correctCount = params.correctCount,
            incorrectCount = params.incorrectCount,
            completedNormally = params.completedNormally,
        )
}
