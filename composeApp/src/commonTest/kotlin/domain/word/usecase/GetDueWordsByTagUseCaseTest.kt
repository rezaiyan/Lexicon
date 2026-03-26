package domain.word.usecase

import core.common.Try
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetDueWordsByTagUseCaseTest {

    private val repository = FakeWordRepository()
    private val useCase = GetDueWordsByTagUseCase(repository)

    @Test
    fun `returns words due for the given tag id`() = runTest {
        val words = listOf(
            createWord(id = 1, original = "Hola"),
            createWord(id = 2, original = "Gracias")
        )
        repository.setDueWordsByTag(tagId = 5L, words = words)

        val emitted = useCase(5L).first()

        assertEquals(words, emitted)
    }

    @Test
    fun `returns empty list when no words are due for tag`() = runTest {
        repository.setDueWordsByTag(tagId = 99L, words = emptyList())

        val emitted = useCase(99L).first()

        assertTrue(emitted.isEmpty())
    }

    @Test
    fun `passes correct tag id to repository`() = runTest {
        useCase(42L).first()

        assertEquals(42L, repository.lastRequestedTagId)
    }

    private fun createWord(
        id: Int,
        original: String,
        translation: String = "translated"
    ) = Word(
        id = id,
        originalWord = original,
        translation = translation,
        description = "",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.SPANISH,
        level = 0,
        easeFactor = 2.5f,
        interval = 0,
        repetitions = 0,
        lastReviewDate = 0L,
        nextReviewDate = 0L
    )

    private class FakeWordRepository : IWordRepository {
        private val dueByTagFlows = mutableMapOf<Long, MutableStateFlow<List<Word>>>()
        var lastRequestedTagId: Long? = null

        fun setDueWordsByTag(tagId: Long, words: List<Word>) {
            dueByTagFlows.getOrPut(tagId) { MutableStateFlow(emptyList()) }.value = words
        }

        override fun getDueCardsByTag(tagId: Long): Flow<List<Word>> {
            lastRequestedTagId = tagId
            return dueByTagFlows.getOrPut(tagId) { MutableStateFlow(emptyList()) }
        }

        override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(emptyList())
        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun insertWords(words: List<Word>): Try<Int> = Try.success(words.size)
        override suspend fun updateWord(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun deleteWord(id: Int): Try<Unit> = Try.success(Unit)
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> =
            flowOf(DeleteWordsProgress.Completed(ids.size))
        override fun updateWordsLanguages(
            ids: List<Int>,
            sourceLanguage: String,
            targetLanguage: String
        ): Flow<UpdateWordsLanguagesProgress> = flow {
            emit(UpdateWordsLanguagesProgress.Completed(ids.size))
        }
        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats())
        override suspend fun getTotalCount(): Try<Int> = Try.success(0)
        override suspend fun getDueCount(): Try<Int> = Try.success(0)
        override suspend fun getMostCommonSourceLanguage(): Try<String?> = Try.success(null)
        override suspend fun updateWordLocal(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun batchSyncWords(words: List<Word>): Try<Unit> = Try.success(Unit)
    }
}
