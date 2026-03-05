package domain.word.usecase

import core.common.Try
import core.common.map
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

        return wordRepository.updateWord(word).map { word }
    }
}
