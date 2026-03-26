package data.word.repository

import core.common.Try
import data.core.database.LexiconQueries
import app.cash.sqldelight.async.coroutines.awaitAsList
import domain.word.repository.IReviewSyncRepository

class ReviewSyncRepository(
    private val queries: LexiconQueries,
) : IReviewSyncRepository {

    override suspend fun enqueue(wordId: Int): Try<Unit> = Try {
        queries.insertReviewSyncEntry(wordId.toLong())
    }

    /**
     * Reads all pending word IDs and clears the queue in a single transaction.
     * The caller must re-enqueue on flush failure.
     */
    override suspend fun dequeueAll(): Try<List<Int>> = Try {
        val ids = queries.getAllReviewSyncEntries().awaitAsList().map { it.toInt() }
        if (ids.isNotEmpty()) {
            queries.clearReviewSyncQueue()
        }
        ids
    }
}
