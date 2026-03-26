package domain.word.usecase

import core.common.Try
import core.common.flatMap
import core.common.getOrElse
import core.common.onFailure
import domain.word.repository.IReviewSyncRepository
import domain.word.repository.IWordRepository

/**
 * Sends all pending reviewed words to the backend in a single batch request.
 *
 * Called at session boundaries (complete, abandon) and at the start of the next session
 * to retry any previously failed flush.
 *
 * On failure the dequeued IDs are re-enqueued so they survive app restarts and are
 * retried on the next call.
 */
class FlushReviewSyncQueueUseCase(
    private val reviewSyncRepository: IReviewSyncRepository,
    private val wordRepository: IWordRepository,
) {
    suspend operator fun invoke(): Try<Unit> {
        val wordIds = reviewSyncRepository.dequeueAll()
            .getOrElse { return Try.failure(it) }

        if (wordIds.isEmpty()) return Try.success(Unit)

        val allWords = wordRepository.getAllWordsAsync()
            .getOrElse { return reEnqueueAndFail(wordIds, it) }

        val wordsToSync = allWords.filter { it.id in wordIds.toSet() }

        return if (wordsToSync.isEmpty()) {
            Try.success(Unit)
        } else {
            wordRepository.batchSyncWords(wordsToSync)
                .flatMap { Try.success(Unit) }
                .onFailure { reEnqueueAndFail(wordIds, it) }
        }
    }

    private suspend fun reEnqueueAndFail(wordIds: List<Int>, cause: Throwable): Try<Unit> {
        wordIds.forEach { id -> reviewSyncRepository.enqueue(id) }
        return Try.failure(cause)
    }
}
