package domain.word.usecase

import core.common.Try
import core.common.getOrThrow
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetSourceLanguageUseCaseTest {

    private var mostCommonSourceLanguage: Try<String?> = Try.success(null)

    private fun createUseCase() = GetSourceLanguageUseCase(
        wordRepository = FakeWordRepository()
    )

    @Test
    fun `returns ENGLISH when no words exist`() = runTest {
        mostCommonSourceLanguage = Try.success(null)
        val result = createUseCase()()

        assertTrue(result.isSuccess)
        assertEquals(Language.ENGLISH, result.getOrThrow())
    }

    @Test
    fun `returns language from most common source code`() = runTest {
        mostCommonSourceLanguage = Try.success("es")
        val result = createUseCase()()

        assertTrue(result.isSuccess)
        assertEquals(Language.SPANISH, result.getOrThrow())
    }

    @Test
    fun `returns language for German code`() = runTest {
        mostCommonSourceLanguage = Try.success("de")
        val result = createUseCase()()

        assertTrue(result.isSuccess)
        assertEquals(Language.GERMAN, result.getOrThrow())
    }

    @Test
    fun `returns ENGLISH when repository returns failure`() = runTest {
        mostCommonSourceLanguage = Try.failure(RuntimeException("DB error"))
        val result = createUseCase()()

        assertTrue(result.isSuccess)
        assertEquals(Language.ENGLISH, result.getOrThrow())
    }

    @Test
    fun `invoke with Unit params delegates to parameterless invoke`() = runTest {
        mostCommonSourceLanguage = Try.success("fr")
        val result = createUseCase().invoke(Unit)

        assertTrue(result.isSuccess)
        assertEquals(Language.FRENCH, result.getOrThrow())
    }

    private inner class FakeWordRepository : IWordRepository {
        override suspend fun getMostCommonSourceLanguage(): Try<String?> = mostCommonSourceLanguage
        override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(emptyList())
        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCardsByTag(tagId: Long): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun insertWords(words: List<Word>): Try<Int> = Try.success(0)
        override suspend fun updateWord(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun deleteWord(id: Int): Try<Unit> = Try.success(Unit)
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf()
        override fun updateWordsLanguages(
            ids: List<Int>,
            sourceLanguage: String,
            targetLanguage: String
        ): Flow<UpdateWordsLanguagesProgress> = flowOf()
        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override fun getProgressStats(): Flow<ProgressStats> = flowOf()
        override suspend fun getTotalCount(): Try<Int> = Try.success(0)
        override suspend fun getDueCount(): Try<Int> = Try.success(0)
        override suspend fun getNextDueAt(): Try<Long?> = Try.success(null)
        override suspend fun updateWordLocal(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun batchSyncWords(words: List<Word>): Try<Unit> = Try.success(Unit)
    }
}
