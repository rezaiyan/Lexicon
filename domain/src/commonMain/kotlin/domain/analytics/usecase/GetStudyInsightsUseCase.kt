package domain.analytics.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.analytics.model.StudyInsights
import domain.analytics.repository.IAnalyticsStatsRepository

class GetStudyInsightsUseCase(
    private val analyticsRepository: IAnalyticsStatsRepository,
) : NoParamUseCase<StudyInsights> {
    override suspend fun invoke(params: Unit): Try<StudyInsights> =
        analyticsRepository.getStudyInsights()
}
