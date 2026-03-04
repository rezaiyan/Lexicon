package domain.word.usecase

import core.common.NoParamFlowUseCase
import domain.word.model.Word
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for retrieving due words for review
 */
class GetDueWordsUseCase(
    private val wordRepository: IWordRepository
) : NoParamFlowUseCase<List<Word>> {
    operator fun invoke(): Flow<List<Word>> {
        return wordRepository.getDueCards()
    }

    override operator fun invoke(params: Unit) = invoke()
}

