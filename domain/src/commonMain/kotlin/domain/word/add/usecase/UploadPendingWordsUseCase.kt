package domain.word.add.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.word.repository.IWordRepository

/**
 * Sends words added while offline (or whose upload failed) to the server. Returns how many were sent.
 * Safe to call any time: does nothing when signed out or when nothing is pending.
 */
class UploadPendingWordsUseCase(
    private val wordRepository: IWordRepository,
) : NoParamUseCase<Int> {
    override suspend fun invoke(params: Unit): Try<Int> = wordRepository.uploadPendingWords()
}
