package domain.word.usecase

import core.common.Try
import domain.word.repository.IWordRepository

class SyncRemoteToLocalUseCase(
    private val wordRepository: IWordRepository
) {
    suspend operator fun invoke(clearFirst: Boolean = false): Try<Unit> {
        return wordRepository.syncRemoteToLocal(clearFirst)
    }
}
