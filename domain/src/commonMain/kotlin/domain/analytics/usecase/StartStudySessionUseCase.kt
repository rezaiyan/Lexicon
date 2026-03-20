package domain.analytics.usecase

import core.common.Try
import core.common.UseCase
import domain.analytics.repository.IAnalyticsRecorder
import kotlin.time.Clock

class StartStudySessionUseCase(
    private val analyticsRecorder: IAnalyticsRecorder,
) : UseCase<StartStudySessionUseCase.Params, String> {

    data class Params(val sessionId: String, val reviewType: String)

    override suspend fun invoke(params: Params): Try<String> {
        val now = Clock.System.now().toEpochMilliseconds()
        return analyticsRecorder.startSession(
            sessionId = params.sessionId,
            reviewType = params.reviewType,
            startedAt = now,
        ).let { result ->
            when (result) {
                is Try.Success -> Try.Success(params.sessionId)
                is Try.Failure -> result
            }
        }
    }
}
