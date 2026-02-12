package domain.word.usecase

import domain.word.repository.IWordRepository

/**
 * Use case for deleting a single word
 */
class DeleteWordUseCase(
    private val wordRepository: IWordRepository
) {
    suspend operator fun invoke(wordId: Int): Result<Unit> {
        return try {
            wordRepository.deleteWord(wordId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

