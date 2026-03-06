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

class SyncRemoteToLocalUseCaseTest {

    private val repository = FakeWordRepo()
    private val useCase = SyncRemoteToLocalUseCase(repository)

    @Test
    fun `syncs with clearFirst false by default`() = runTest {
        val result = useCase.invoke()

        assertTrue(result.isSuccess)
        assertEquals(false, repository.lastClearFirst)
    }

    @Test
    fun `syncs with clearFirst true when specified`() = runTest {
        val result = useCase(true)

        assertTrue(result.isSuccess)
        assertEquals(true, repository.lastClearFirst)
    }

    @Test
    fun `syncs with clearFirst false when specified`() = runTest {
        val result = useCase(false)

        assertTrue(result.isSuccess)
        assertEquals(false, repository.lastClearFirst)
    }

    @Test
    fun `returns failure when repository fails`() = runTest {
        repository.syncResult = Try.failure(RuntimeException("Sync failed"))

        val result = useCase(false)

        assertTrue(result.isFailure)
    }

    private class FakeWordRepo : IWordRepository {
        var syncResult: Try<Unit> = Try.success(Unit)
        var lastClearFirst: Boolean? = null

        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> {
            lastClearFirst = clearFirst
            return syncResult
        }

        override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(emptyList())
        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun insertWords(words: List<Word>): Try<Int> = Try.success(0)
        override suspend fun updateWord(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun deleteWord(id: Int): Try<Unit> = Try.success(Unit)
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf(DeleteWordsProgress.Completed(0))
        override fun updateWordsLanguages(ids: List<Int>, sourceLanguage: String, targetLanguage: String): Flow<UpdateWordsLanguagesProgress> = flow { emit(UpdateWordsLanguagesProgress.Completed(ids.size)) }
        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats())
        override suspend fun getTotalCount(): Try<Int> = Try.success(0)
        override suspend fun getDueCount(): Try<Int> = Try.success(0)
    }
}
