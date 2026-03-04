package domain.word.usecase

import core.common.Try
import domain.word.repository.IWordRepository

/**
 * Use case for deleting a single word
 */
class DeleteWordUseCase(
    private val wordRepository: IWordRepository
) {
    suspend operator fun invoke(wordId: Int): Try<Unit> {
        return Try {
            wordRepository.deleteWord(wordId)
        }
    }
}
