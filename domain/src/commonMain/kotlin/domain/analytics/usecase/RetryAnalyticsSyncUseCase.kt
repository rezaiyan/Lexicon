package domain.analytics.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.analytics.repository.IAnalyticsRecorder

class RetryAnalyticsSyncUseCase(
    private val analyticsRecorder: IAnalyticsRecorder,
) : NoParamUseCase<Unit> {
    override suspend fun invoke(params: Unit): Try<Unit> =
        analyticsRecorder.retryPendingSync()
}
