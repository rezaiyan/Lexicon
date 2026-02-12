package domain.word.usecase

import domain.word.model.Word
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for retrieving due words for review
 */
class GetDueWordsUseCase(
    private val wordRepository: IWordRepository
) {
    operator fun invoke(): Flow<List<Word>> {
        return wordRepository.getDueCards()
    }
}

