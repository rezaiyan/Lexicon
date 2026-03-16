package domain.analytics.usecase

import core.common.Try
import core.common.UseCase
import domain.analytics.model.StudyHeatmapDay
import domain.analytics.repository.IAnalyticsRepository

class GetStudyHeatmapUseCase(
    private val analyticsRepository: IAnalyticsRepository,
) : UseCase<GetStudyHeatmapUseCase.Params, List<StudyHeatmapDay>> {

    data class Params(val startDate: String, val endDate: String)

    override suspend fun invoke(params: Params): Try<List<StudyHeatmapDay>> =
        analyticsRepository.getStudyHeatmap(params.startDate, params.endDate)
}
