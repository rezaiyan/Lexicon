package domain.word.usecase

import core.common.Try
import core.common.UseCase
import domain.word.repository.IWordRepository

/**
 * Use case for deleting a single word
 */
class DeleteWordUseCase(
    private val wordRepository: IWordRepository
) : UseCase<Int, Unit> {
    override suspend operator fun invoke(wordId: Int): Try<Unit> {
        return wordRepository.deleteWord(wordId)
    }
}
