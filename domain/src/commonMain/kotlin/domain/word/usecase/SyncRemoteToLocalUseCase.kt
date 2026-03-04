package domain.word.usecase

import core.common.Try
import core.common.UseCase
import domain.word.repository.IWordRepository

class SyncRemoteToLocalUseCase(
    private val wordRepository: IWordRepository
) : UseCase<Boolean, Unit> {
    override suspend operator fun invoke(clearFirst: Boolean): Try<Unit> {
        return wordRepository.syncRemoteToLocal(clearFirst)
    }

    suspend fun invoke(): Try<Unit> = invoke(clearFirst = false)
}
