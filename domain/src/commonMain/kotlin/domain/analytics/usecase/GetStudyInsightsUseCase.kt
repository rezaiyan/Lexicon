package domain.analytics.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.analytics.model.StudyInsights
import domain.analytics.repository.IAnalyticsRepository

class GetStudyInsightsUseCase(
    private val analyticsRepository: IAnalyticsRepository,
) : NoParamUseCase<StudyInsights> {
    override suspend fun invoke(params: Unit): Try<StudyInsights> =
        analyticsRepository.getStudyInsights()
}
