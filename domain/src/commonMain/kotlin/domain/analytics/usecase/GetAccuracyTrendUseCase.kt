package domain.analytics.usecase

import core.common.Try
import core.common.UseCase
import domain.analytics.model.DailyStudyStats
import domain.analytics.repository.IAnalyticsStatsRepository

class GetAccuracyTrendUseCase(
    private val analyticsRepository: IAnalyticsStatsRepository,
) : UseCase<GetAccuracyTrendUseCase.Params, List<DailyStudyStats>> {

    data class Params(val startDate: String, val endDate: String)

    override suspend fun invoke(params: Params): Try<List<DailyStudyStats>> =
        analyticsRepository.getDailyStats(params.startDate, params.endDate)
}
