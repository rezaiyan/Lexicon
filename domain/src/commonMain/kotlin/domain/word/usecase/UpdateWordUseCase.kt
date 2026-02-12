package domain.word.usecase

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
    ): Result<Word> {
        return try {
            if (word.originalWord.isBlank() || word.translation.isBlank()) {
                return Result.failure(Exception("Word and translation cannot be empty"))
            }
            
            // Update the word (preserves learning progress)
            wordRepository.updateWord(word)
            Result.success(word)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

