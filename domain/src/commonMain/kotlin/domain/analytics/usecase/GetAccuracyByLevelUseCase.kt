package domain.analytics.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.analytics.model.AccuracyByLevel
import domain.analytics.repository.IAnalyticsRepository

class GetAccuracyByLevelUseCase(
    private val analyticsRepository: IAnalyticsRepository,
) : NoParamUseCase<List<AccuracyByLevel>> {
    override suspend fun invoke(params: Unit): Try<List<AccuracyByLevel>> =
        analyticsRepository.getAccuracyByLevel()
}
