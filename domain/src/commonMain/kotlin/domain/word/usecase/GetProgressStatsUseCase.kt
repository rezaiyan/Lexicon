package domain.word.usecase

import core.common.NoParamFlowUseCase
import domain.word.model.ProgressStats
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for retrieving progress statistics
 */
class GetProgressStatsUseCase(
    private val wordRepository: IWordRepository
) : NoParamFlowUseCase<ProgressStats> {
    operator fun invoke(): Flow<ProgressStats> {
        return wordRepository.getProgressStats()
    }

    override operator fun invoke(params: Unit) = invoke()
}

