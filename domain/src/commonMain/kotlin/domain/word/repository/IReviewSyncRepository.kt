package domain.word.repository

import core.common.Try

/**
 * Outbox queue for deferred remote sync of reviewed words.
 *
 * When a word is reviewed, its ID is enqueued here instead of immediately syncing to the
 * server. At session boundaries (complete, abandon, or next session start) the queue is
 * flushed as a single batch request. Failed flushes re-enqueue the IDs so they survive
 * app restarts.
 */
interface IReviewSyncRepository {
    /** Add a word ID to the pending-sync queue. Idempotent — duplicate IDs are ignored. */
    suspend fun enqueue(wordId: Int): Try<Unit>

    /**
     * Return all pending word IDs and clear the queue atomically.
     * On flush failure the caller is responsible for re-enqueuing.
     */
    suspend fun dequeueAll(): Try<List<Int>>
}
