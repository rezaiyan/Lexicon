package domain.word.usecase

import core.common.Try
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeleteWordUseCaseTest {

    private val repository = FakeWordRepository()
    private val useCase = DeleteWordUseCase(repository)

    @Test
    fun `successful deletion returns success result`() = runTest {
        val result = useCase(42)

        assertTrue(result.isSuccess)
        assertEquals(1, repository.deleteWordCallCount)
        assertEquals(42, repository.lastDeletedId)
    }

    @Test
    fun `repository exception returns failure result`() = runTest {
        repository.shouldThrow = true

        val result = useCase(99)

        assertTrue(result.isFailure)
        assertEquals(1, repository.deleteWordCallCount)
    }

    @Test
    fun `negative id returns failure when repository rejects`() = runTest {
        repository.throwOnNegative = true

        val result = useCase(-5)

        assertTrue(result.isFailure)
        assertEquals(1, repository.deleteWordCallCount)
        assertEquals(-5, repository.lastDeletedId)
    }

    private class FakeWordRepository : IWordRepository {
        var deleteWordCallCount = 0
        var lastDeletedId: Int? = null
        var shouldThrow = false
        var throwOnNegative = false

        override suspend fun deleteWord(id: Int): Try<Unit> {
            deleteWordCallCount++
            lastDeletedId = id
            if (throwOnNegative && id < 0) {
                return Try.failure(IllegalArgumentException("Invalid id"))
            }
            if (shouldThrow) {
                return Try.failure(IllegalStateException("Repository failure"))
            }
            return Try.success(Unit)
        }

        override suspend fun updateWord(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun insertWords(words: List<Word>): Try<Int> = Try.success(words.size)
        override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(emptyList())
        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf(DeleteWordsProgress.Completed(0))
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats())
        override suspend fun getTotalCount(): Try<Int> = Try.success(0)
        override suspend fun getDueCount(): Try<Int> = Try.success(0)
        override fun updateWordsLanguages(ids: List<Int>, sourceLanguage: String, targetLanguage: String): Flow<UpdateWordsLanguagesProgress> = flow { emit(UpdateWordsLanguagesProgress.Completed(ids.size)) }
        override suspend fun getMostCommonSourceLanguage(): Try<String?> = Try.success(null)
    }
}

