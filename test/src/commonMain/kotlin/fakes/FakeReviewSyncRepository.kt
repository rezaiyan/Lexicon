package fakes

import core.common.Try
import domain.word.repository.IReviewSyncRepository

class FakeReviewSyncRepository : IReviewSyncRepository {
    private val queue = mutableListOf<Int>()

    val pendingIds: List<Int> get() = queue.toList()

    override suspend fun enqueue(wordId: Int): Try<Unit> {
        if (wordId !in queue) queue.add(wordId)
        return Try.success(Unit)
    }

    override suspend fun dequeueAll(): Try<List<Int>> {
        val ids = queue.toList()
        queue.clear()
        return Try.success(ids)
    }
}
