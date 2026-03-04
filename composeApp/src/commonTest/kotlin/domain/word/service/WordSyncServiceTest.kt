package domain.word.service

import core.common.Try
import core.common.getOrThrow
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WordSyncServiceTest {

    @Test
    fun `new remote words are inserted and count returned`() = runTest {
        val repository = FakeWordRepository(existing = emptyList())
        val service = WordSyncService(repository)
        val remote = listOf(
            createWord(id = 1, original = "Hello", translation = "Hola"),
            createWord(id = 2, original = "World", translation = "Mundo")
        )

        val result = service.syncWords(remote).first()

        assertEquals(2, repository.lastInserted.size)
        assertEquals(2, result.getOrThrow())
        assertTrue(repository.lastInserted.containsAll(remote))
    }

    @Test
    fun `existing words are skipped using case insensitive comparison`() = runTest {
        val existing = listOf(createWord(id = 10, original = "Hello", translation = "Hola"))
        val repository = FakeWordRepository(existing = existing)
        val service = WordSyncService(repository)
        val remote = listOf(
            createWord(id = 1, original = " hello  ", translation = " HOLA "),
            createWord(id = 2, original = "Goodbye", translation = "Adiós")
        )

        val result = service.syncWords(remote).first()

        assertEquals(1, repository.lastInserted.size)
        assertEquals("Goodbye", repository.lastInserted.first().originalWord)
        assertEquals(1, result.getOrThrow())
    }

    @Test
    fun `no remote words results in zero sync and no repository insertion`() = runTest {
        val repository = FakeWordRepository(existing = emptyList())
        val service = WordSyncService(repository)

        val result = service.syncWords(emptyList()).first()

        assertTrue(repository.lastInserted.isEmpty())
        assertEquals(0, result.getOrThrow())
    }

    private fun createWord(
        id: Int,
        original: String,
        translation: String,
        description: String = "",
        level: Int = 0,
        easeFactor: Float = 2.5f,
        interval: Int = 0,
        repetitions: Int = 0
    ): Word = Word(
        id = id,
        originalWord = original,
        translation = translation,
        description = description,
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.SPANISH,
        level = level,
        easeFactor = easeFactor,
        interval = interval,
        repetitions = repetitions,
        lastReviewDate = 0L,
        nextReviewDate = 0L
    )

    private class FakeWordRepository(
        existing: List<Word>
    ) : IWordRepository {
        private val existingFlow = MutableStateFlow(existing)
        val lastInserted = mutableListOf<Word>()

        override suspend fun getAllWordsAsync(): List<Word> = existingFlow.value
        override fun getAllWords(): Flow<List<Word>> = existingFlow
        override suspend fun insertWords(words: List<Word>): Int {
            if (words.isNotEmpty()) {
                lastInserted.clear()
                lastInserted.addAll(words)
                existingFlow.value = existingFlow.value + words
            }
            return words.size
        }

        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun updateWord(word: Word) {}
        override suspend fun deleteWord(id: Int) {}
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> =
            flowOf(DeleteWordsProgress.Completed(0))
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats())
        override suspend fun getTotalCount(): Int = existingFlow.value.size
        override suspend fun getDueCount(): Int = 0
        override fun updateWordsLanguages(ids: List<Int>, sourceLanguage: String, targetLanguage: String): Flow<UpdateWordsLanguagesProgress> = flow { emit(UpdateWordsLanguagesProgress.Completed(ids.size)) }
    }
}

