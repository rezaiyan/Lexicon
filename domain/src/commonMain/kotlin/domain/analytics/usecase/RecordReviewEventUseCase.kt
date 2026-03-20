package domain.analytics.usecase

import core.common.Try
import core.common.UseCase
import domain.analytics.model.ReviewEventParams
import domain.analytics.repository.IAnalyticsRecorder

class RecordReviewEventUseCase(
    private val analyticsRecorder: IAnalyticsRecorder,
) : UseCase<ReviewEventParams, Unit> {
    override suspend fun invoke(params: ReviewEventParams): Try<Unit> =
        analyticsRecorder.recordReviewEvent(params)
}
