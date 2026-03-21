package domain.analytics.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.analytics.model.LevelTransition
import domain.analytics.repository.IAnalyticsWordRepository

class GetLevelTransitionsUseCase(
    private val analyticsRepository: IAnalyticsWordRepository,
) : NoParamUseCase<List<LevelTransition>> {
    override suspend fun invoke(params: Unit): Try<List<LevelTransition>> =
        analyticsRepository.getLevelTransitions()
}
