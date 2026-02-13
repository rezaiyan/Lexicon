package domain.word.usecase

import domain.common.Try
import domain.word.model.Word
import domain.word.repository.IWordRepository

/**
 * Use case for updating a word's content
 */
class UpdateWordUseCase(
    private val wordRepository: IWordRepository
) {
    suspend operator fun invoke(
        word: Word
    ): Try<Word> {
        if (word.originalWord.isBlank() || word.translation.isBlank()) {
            return Try.failure(Exception("Word and translation cannot be empty"))
        }

        return Try {
            // Update the word (preserves learning progress)
            wordRepository.updateWord(word)
            word
        }
    }
}
