package domain.word.usecase

import core.common.NoParamFlowUseCase
import domain.word.model.Word
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for getting all words (for Word Manager screen)
 */
class GetAllWordsUseCase(
    private val wordRepository: IWordRepository
) : NoParamFlowUseCase<List<Word>> {
    operator fun invoke(): Flow<List<Word>> {
        return wordRepository.getAllWords()
    }

    override operator fun invoke(params: Unit) = invoke()
}

