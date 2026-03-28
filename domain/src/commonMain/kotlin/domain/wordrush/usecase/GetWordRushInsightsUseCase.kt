package domain.wordrush.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.wordrush.model.WordRushInsights
import domain.wordrush.repository.IWordRushStatsRepository

class GetWordRushInsightsUseCase(
    private val statsRepository: IWordRushStatsRepository,
) : NoParamUseCase<WordRushInsights> {
    override suspend fun invoke(params: Unit): Try<WordRushInsights> =
        statsRepository.getInsights()
}
