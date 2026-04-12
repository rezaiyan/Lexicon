package domain.word.usecase

import core.common.Try
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IReviewSyncRepository
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlushReviewSyncQueueUseCaseTest {

    // ---------------------------------------------------------------------------
    // Fakes
    // ---------------------------------------------------------------------------

    private class FakeReviewSyncRepository : IReviewSyncRepository {
        private val queue = mutableListOf<Int>()

        var dequeueAllResult: Try<List<Int>>? = null // null = use queue contents
        var enqueueCallCount = 0
        val enqueuedIds = mutableListOf<Int>()

        override suspend fun enqueue(wordId: Int): Try<Unit> {
            enqueueCallCount++
            enqueuedIds.add(wordId)
            queue.add(wordId)
            return Try.success(Unit)
        }

        override suspend fun dequeueAll(): Try<List<Int>> {
            val explicitResult = dequeueAllResult
            if (explicitResult != null) return explicitResult
            val ids = queue.toList()
            queue.clear()
            return Try.success(ids)
        }

        fun seed(vararg ids: Int) {
            queue.addAll(ids.toList())
        }
    }

    private class FakeWordRepository : IWordRepository {
        private val words = mutableListOf<Word>()
        var getAllWordsResult: Try<List<Word>>? = null // null = use stored words
        var batchSyncResult: Try<Unit> = Try.success(Unit)
        var batchSyncCallCount = 0
        var lastBatchSyncedWords: List<Word> = emptyList()

        fun seed(vararg seedWords: Word) {
            words.addAll(seedWords)
        }

        override suspend fun getAllWordsAsync(): Try<List<Word>> =
            getAllWordsResult ?: Try.success(words.toList())

        override suspend fun batchSyncWords(syncWords: List<Word>): Try<Unit> {
            batchSyncCallCount++
            lastBatchSyncedWords = syncWords
            return batchSyncResult
        }

        // Unused methods — minimal stubs
        override suspend fun updateWordLocal(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun updateWord(word: Word): Try<Unit> = Try.success(Unit)
        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCardsByTag(tagId: Long): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun insertWords(words: List<Word>): Try<Int> = Try.success(words.size)
        override suspend fun deleteWord(id: Int): Try<Unit> = Try.success(Unit)
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> =
            flowOf(DeleteWordsProgress.Completed(0))
        override fun updateWordsLanguages(
            ids: List<Int>,
            sourceLanguage: String,
            targetLanguage: String,
        ): Flow<UpdateWordsLanguagesProgress> =
            flow { emit(UpdateWordsLanguagesProgress.Completed(ids.size)) }
        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats())
        override suspend fun getTotalCount(): Try<Int> = Try.success(0)
        override suspend fun getDueCount(): Try<Int> = Try.success(0)
        override suspend fun getNextDueAt(): Try<Long?> = Try.success(null)
        override suspend fun getMostCommonSourceLanguage(): Try<String?> = Try.success(null)
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private val syncRepo = FakeReviewSyncRepository()
    private val wordRepo = FakeWordRepository()
    private val useCase = FlushReviewSyncQueueUseCase(syncRepo, wordRepo)

    private fun testWord(id: Int) = Word(
        id = id,
        originalWord = "word$id",
        translation = "trans$id",
        description = "",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.GERMAN,
        nextReviewDate = 0L,
    )

    // ---------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    @Test
    fun `empty queue returns success without calling batchSyncWords`() = runTest {
        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(0, wordRepo.batchSyncCallCount)
    }

    @Test
    fun `queue with matching word IDs calls batchSyncWords with those words`() = runTest {
        val word1 = testWord(1)
        val word2 = testWord(2)
        syncRepo.seed(1, 2)
        wordRepo.seed(word1, word2)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(1, wordRepo.batchSyncCallCount)
        assertEquals(setOf(1, 2), wordRepo.lastBatchSyncedWords.map { it.id }.toSet())
    }

    @Test
    fun `queued IDs not present in local DB are silently skipped — returns success`() = runTest {
        // IDs 1 and 2 are enqueued but the local DB is empty (words deleted)
        syncRepo.seed(1, 2)
        // wordRepo has no words seeded → getAllWordsAsync returns empty list

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(0, wordRepo.batchSyncCallCount)
    }

    @Test
    fun `only queued word IDs that exist locally are synced — extras ignored`() = runTest {
        // Enqueue IDs 1, 2, 3 but only 1 and 3 exist locally
        val word1 = testWord(1)
        val word3 = testWord(3)
        syncRepo.seed(1, 2, 3)
        wordRepo.seed(word1, word3)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(1, wordRepo.batchSyncCallCount)
        assertEquals(setOf(1, 3), wordRepo.lastBatchSyncedWords.map { it.id }.toSet())
    }

    @Test
    fun `dequeueAll failure returns failure without re-enqueuing`() = runTest {
        val error = RuntimeException("DB error")
        syncRepo.dequeueAllResult = Try.failure(error)

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(0, syncRepo.enqueueCallCount)
        assertEquals(0, wordRepo.batchSyncCallCount)
    }

    @Test
    fun `getAllWordsAsync failure re-enqueues original IDs and returns failure`() = runTest {
        syncRepo.seed(10, 20)
        wordRepo.getAllWordsResult = Try.failure(RuntimeException("network error"))

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(2, syncRepo.enqueueCallCount)
        assertTrue(syncRepo.enqueuedIds.containsAll(listOf(10, 20)))
        assertEquals(0, wordRepo.batchSyncCallCount)
    }

    @Test
    fun `batchSyncWords failure re-enqueues original IDs and returns failure`() = runTest {
        val word5 = testWord(5)
        syncRepo.seed(5)
        wordRepo.seed(word5)
        wordRepo.batchSyncResult = Try.failure(RuntimeException("sync failed"))

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(1, syncRepo.enqueueCallCount)
        assertTrue(syncRepo.enqueuedIds.contains(5))
    }

    @Test
    fun `batchSyncWords success does NOT re-enqueue IDs`() = runTest {
        val word7 = testWord(7)
        syncRepo.seed(7)
        wordRepo.seed(word7)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(0, syncRepo.enqueueCallCount)
    }

    @Test
    fun `getAllWordsAsync failure preserves all original queued IDs for re-enqueue`() = runTest {
        syncRepo.seed(100, 200, 300)
        wordRepo.getAllWordsResult = Try.failure(RuntimeException("timeout"))

        useCase()

        assertEquals(3, syncRepo.enqueueCallCount)
        assertEquals(setOf(100, 200, 300), syncRepo.enqueuedIds.toSet())
    }

    @Test
    fun `batchSyncWords failure preserves all original queued IDs for re-enqueue`() = runTest {
        val words = listOf(testWord(11), testWord(12), testWord(13))
        syncRepo.seed(11, 12, 13)
        words.forEach { wordRepo.seed(it) }
        wordRepo.batchSyncResult = Try.failure(RuntimeException("server error"))

        useCase()

        assertEquals(3, syncRepo.enqueueCallCount)
        assertEquals(setOf(11, 12, 13), syncRepo.enqueuedIds.toSet())
    }

    @Test
    fun `single queued word synced successfully`() = runTest {
        val word = testWord(42)
        syncRepo.seed(42)
        wordRepo.seed(word)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(1, wordRepo.batchSyncCallCount)
        assertEquals(listOf(word), wordRepo.lastBatchSyncedWords)
        assertEquals(0, syncRepo.enqueueCallCount)
    }

    @Test
    fun `result is success Unit when batch sync succeeds`() = runTest {
        val word = testWord(1)
        syncRepo.seed(1)
        wordRepo.seed(word)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
    }
}
