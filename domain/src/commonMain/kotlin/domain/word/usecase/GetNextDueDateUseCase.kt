package domain.word.usecase

import core.common.Try
import core.common.UseCase
import domain.word.repository.IWordRepository

/**
 * Returns the epoch-millisecond timestamp of the next word due for review,
 * or null if there are no future-scheduled words.
 */
class GetNextDueDateUseCase(
    private val wordRepository: IWordRepository,
) : UseCase<Unit, Long?> {
    override suspend operator fun invoke(params: Unit): Try<Long?> {
        return wordRepository.getNextDueAt()
    }
}
