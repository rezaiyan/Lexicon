package domain.word.usecase

import core.common.Try
import core.common.UseCase
import core.common.flatMap
import domain.tag.usecase.SyncTagsFromRemoteUseCase
import domain.word.repository.IWordRepository

/**
 * Pulls words and tags changed elsewhere since the last pull (another device, an AI client), for when
 * the app comes back to the foreground. The repository throttles the word pull, so frequent resumes
 * are cheap. Tags are pulled too: a word tagged elsewhere can point at a tag this device has never seen.
 *
 * Reviews not yet on the server are uploaded first. If that fails the pull is skipped: pulled words
 * replace local ones, so the server's older copy would overwrite the unsent progress.
 */
class RefreshWordsUseCase(
    private val flushReviewSyncQueue: FlushReviewSyncQueueUseCase,
    private val wordRepository: IWordRepository,
    private val syncTagsFromRemote: SyncTagsFromRemoteUseCase,
) : UseCase<Unit, Unit> {
    override suspend operator fun invoke(params: Unit): Try<Unit> =
        flushReviewSyncQueue().flatMap {
            val words = wordRepository.syncWithRemote()
            val tags = syncTagsFromRemote(Unit)
            words.flatMap { tags }
        }
}
