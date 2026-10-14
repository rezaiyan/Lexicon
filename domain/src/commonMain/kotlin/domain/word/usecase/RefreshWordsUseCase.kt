package domain.word.usecase

import core.common.Try
import core.common.UseCase
import core.common.flatMap
import domain.word.repository.IWordRepository

/**
 * Pulls words changed elsewhere since the last pull (another device, an AI client), for when the
 * app comes back to the foreground. The repository throttles the pull, so frequent resumes are cheap.
 *
 * Reviews not yet on the server are uploaded first. If that fails the pull is skipped: pulled words
 * replace local ones, so the server's older copy would overwrite the unsent progress.
 */
class RefreshWordsUseCase(
    private val flushReviewSyncQueue: FlushReviewSyncQueueUseCase,
    private val wordRepository: IWordRepository,
) : UseCase<Unit, Unit> {
    override suspend operator fun invoke(params: Unit): Try<Unit> =
        flushReviewSyncQueue().flatMap { wordRepository.syncWithRemote() }
}
