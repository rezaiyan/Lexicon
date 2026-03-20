package domain.analytics.usecase

import core.common.Try
import core.common.UseCase
import domain.analytics.model.WordDifficulty
import domain.analytics.repository.IAnalyticsWordRepository

class GetDifficultWordsUseCase(
    private val analyticsRepository: IAnalyticsWordRepository,
) : UseCase<GetDifficultWordsUseCase.Params, List<WordDifficulty>> {

    data class Params(val minReviews: Int = 3, val limit: Int = 20)

    override suspend fun invoke(params: Params): Try<List<WordDifficulty>> =
        analyticsRepository.getDifficultWords(params.minReviews, params.limit)
}
