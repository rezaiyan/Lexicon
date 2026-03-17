package domain.analytics.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.analytics.model.WeeklyReport
import domain.analytics.repository.IAnalyticsRepository

class GetWeeklyReportUseCase(
    private val analyticsRepository: IAnalyticsRepository,
) : NoParamUseCase<WeeklyReport> {
    override suspend fun invoke(params: Unit): Try<WeeklyReport> =
        analyticsRepository.getWeeklyReport()
}
