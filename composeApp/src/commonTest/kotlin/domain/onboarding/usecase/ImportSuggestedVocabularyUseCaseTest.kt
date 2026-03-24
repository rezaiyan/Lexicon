package domain.onboarding.usecase

import core.common.Try
import core.common.getOrNull
import domain.onboarding.model.SuggestedVocabulary
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
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImportSuggestedVocabularyUseCaseTest {

    private val repository = FakeWordRepository()
    private val useCase = ImportSuggestedVocabularyUseCase(repository)
    private fun params(suggestions: List<SuggestedVocabulary>, tagId: Long? = null) =
        ImportSuggestedVocabularyUseCase.Params(suggestions, tagId)

    @Test
    fun `imports suggestions as words`() = runTest {
        val suggestions = listOf(
            createSuggestion(original = "Hello", translation = "Hola"),
            createSuggestion(original = "Goodbye", translation = "Adiós")
        )

        val result = useCase(params(suggestions))

        assertTrue(result.isSuccess)
        assertEquals(2, repository.insertedWords.size)
    }

    @Test
    fun `returns correct count`() = runTest {
        val suggestions = listOf(
            createSuggestion(original = "One", translation = "Uno"),
            createSuggestion(original = "Two", translation = "Dos"),
            createSuggestion(original = "Three", translation = "Tres")
        )

        val result = useCase(params(suggestions))

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())
    }

    @Test
    fun `maps fields correctly`() = runTest {
        val suggestions = listOf(
            SuggestedVocabulary(
                originalWord = "Cat",
                translation = "Gato",
                description = "A domestic animal",
                sourceLanguage = "en",
                targetLanguage = "es"
            )
        )

        val result = useCase(params(suggestions))

        assertTrue(result.isSuccess)
        val word = repository.insertedWords.first()
        assertEquals(0, word.id)
        assertEquals("Cat", word.originalWord)
        assertEquals("Gato", word.translation)
        assertEquals("A domestic animal", word.description)
        assertEquals(Language.ENGLISH, word.sourceLanguage)
        assertEquals(Language.SPANISH, word.targetLanguage)
        assertEquals(0, word.level)
        assertEquals(2.5f, word.easeFactor)
        assertEquals(0, word.interval)
        assertEquals(0, word.repetitions)
        assertEquals(0L, word.lastReviewDate)
    }

    @Test
    fun `returns success for empty list`() = runTest {
        val result = useCase(params(emptyList()))

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
        assertEquals(0, repository.insertedWords.size)
    }

    private fun createSuggestion(
        original: String,
        translation: String,
        description: String = ""
    ) = SuggestedVocabulary(
        originalWord = original,
        translation = translation,
        description = description,
        sourceLanguage = "en",
        targetLanguage = "es"
    )

    private class FakeWordRepository : IWordRepository {
        val insertedWords = mutableListOf<Word>()

        override suspend fun insertWords(words: List<Word>): Try<Int> {
            insertedWords.addAll(words)
            return Try.success(words.size)
        }

        override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(insertedWords.toList())
        override fun getAllWords(): Flow<List<Word>> = flowOf(insertedWords)
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCardsByTag(tagId: Long): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun updateWord(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun deleteWord(id: Int): Try<Unit> = Try.success(Unit)
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf(DeleteWordsProgress.Completed(0))
        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats())
        override suspend fun getTotalCount(): Try<Int> = Try.success(insertedWords.size)
        override suspend fun getDueCount(): Try<Int> = Try.success(0)
        override fun updateWordsLanguages(ids: List<Int>, sourceLanguage: String, targetLanguage: String): Flow<UpdateWordsLanguagesProgress> = flow { emit(UpdateWordsLanguagesProgress.Completed(ids.size)) }
        override suspend fun getMostCommonSourceLanguage(): Try<String?> = Try.success(null)
    }
}
