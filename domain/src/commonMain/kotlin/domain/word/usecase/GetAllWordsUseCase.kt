package domain.word.usecase

import domain.word.model.Word
import domain.word.repository.IWordRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for getting all words (for Word Manager screen)
 */
class GetAllWordsUseCase(
    private val wordRepository: IWordRepository
) {
    operator fun invoke(): Flow<List<Word>> {
        return wordRepository.getAllWords()
    }
}

