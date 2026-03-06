package domain.word.usecase

import core.common.Try
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BatchUpdateLanguagesUseCaseTest {

    private val repository = FakeWordRepo()
    private val useCase = BatchUpdateLanguagesUseCase(repository)

    @Test
    fun `empty word ids returns error`() = runTest {
        val result = useCase(emptyList(), "en", "de").first()

        assertTrue(result is BatchUpdateLanguagesResult.Error)
        assertEquals("No words selected", (result as BatchUpdateLanguagesResult.Error).message)
    }

    @Test
    fun `emits Updating then maps repository progress`() = runTest {
        repository.updateLanguagesFlow = flow {
            emit(UpdateWordsLanguagesProgress.UpdatingBackend(3))
            emit(UpdateWordsLanguagesProgress.UpdatingLocal(3))
            emit(UpdateWordsLanguagesProgress.Completed(3))
        }

        val results = useCase(listOf(1, 2, 3), "en", "de").toList()

        assertTrue(results[0] is BatchUpdateLanguagesResult.Updating)
        assertEquals(3, (results[0] as BatchUpdateLanguagesResult.Updating).count)
        assertTrue(results[1] is BatchUpdateLanguagesResult.UpdatingBackend)
        assertTrue(results[2] is BatchUpdateLanguagesResult.UpdatingLocal)
        assertTrue(results[3] is BatchUpdateLanguagesResult.Success)
    }

    @Test
    fun `maps Failed progress to Error result`() = runTest {
        repository.updateLanguagesFlow = flow {
            emit(UpdateWordsLanguagesProgress.Failed("Update failed"))
        }

        val results = useCase(listOf(1), "en", "de").toList()

        // First is Updating, second is Error
        assertTrue(results.last() is BatchUpdateLanguagesResult.Error)
    }

    @Test
    fun `invoke with Params delegates correctly`() = runTest {
        repository.updateLanguagesFlow = flow {
            emit(UpdateWordsLanguagesProgress.Completed(2))
        }

        val results = useCase(BatchUpdateLanguagesUseCase.Params(listOf(1, 2), "en", "fr")).toList()

        assertTrue(results.any { it is BatchUpdateLanguagesResult.Success })
    }

    private class FakeWordRepo : IWordRepository {
        var updateLanguagesFlow: Flow<UpdateWordsLanguagesProgress> = flow {
            emit(UpdateWordsLanguagesProgress.Completed(0))
        }

        override fun updateWordsLanguages(
            ids: List<Int>,
            sourceLanguage: String,
            targetLanguage: String
        ): Flow<UpdateWordsLanguagesProgress> = updateLanguagesFlow

        override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(emptyList())
        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun insertWords(words: List<Word>): Try<Int> = Try.success(0)
        override suspend fun updateWord(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun deleteWord(id: Int): Try<Unit> = Try.success(Unit)
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf(DeleteWordsProgress.Completed(0))
        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats())
        override suspend fun getTotalCount(): Try<Int> = Try.success(0)
        override suspend fun getDueCount(): Try<Int> = Try.success(0)
    }
}
