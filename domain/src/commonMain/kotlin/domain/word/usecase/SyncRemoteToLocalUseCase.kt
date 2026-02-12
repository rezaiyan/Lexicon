package domain.word.usecase

import domain.word.repository.IWordRepository

class SyncRemoteToLocalUseCase(
    private val wordRepository: IWordRepository
) {
    suspend operator fun invoke(clearFirst: Boolean = false): Result<Unit> {
        return wordRepository.syncRemoteToLocal(clearFirst)
    }
}


