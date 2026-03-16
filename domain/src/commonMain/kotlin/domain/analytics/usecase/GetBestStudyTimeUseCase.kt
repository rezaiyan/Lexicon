package domain.analytics.usecase

import core.common.NoParamUseCase
import core.common.Try
import core.common.map
import domain.analytics.model.HourlyAccuracy
import domain.analytics.repository.IAnalyticsRepository

class GetBestStudyTimeUseCase(
    private val analyticsRepository: IAnalyticsRepository,
) : NoParamUseCase<HourlyAccuracy?> {
    override suspend fun invoke(params: Unit): Try<HourlyAccuracy?> =
        analyticsRepository.getAccuracyByHourOfDay().map { hours ->
            hours.filter { it.totalReviews >= 5 }.maxByOrNull { it.accuracyPercent }
        }
}
