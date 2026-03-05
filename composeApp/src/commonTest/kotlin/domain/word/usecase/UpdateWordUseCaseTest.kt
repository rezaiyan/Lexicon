package domain.word.usecase

import core.common.Try
import core.common.exceptionOrNull
import core.common.getOrThrow
import domain.word.model.Word
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive tests for UpdateWordUseCase
 * 
 * Tests cover:
 * - Valid word updates
 * - Validation of required fields
 * - Error handling
 * - Repository interaction
 */
class UpdateWordUseCaseTest {
    
    private val fakeRepository = FakeWordRepositoryForUpdate()
    private val useCase = UpdateWordUseCase(fakeRepository)
    
    @Test
    fun `valid word update should succeed`() = runTest {
        // Given: A valid word
        val word = createTestWord(
            id = 1,
            originalWord = "hello",
            translation = "hola",
            description = "Common greeting"
        )
        
        // When: Updating the word
        val result = useCase(word)
        
        // Then: Should succeed
        assertTrue(result.isSuccess)
        assertEquals(word, result.getOrThrow())
        assertEquals(1, fakeRepository.updateCallCount)
        assertEquals(word, fakeRepository.lastUpdatedWord)
    }
    
    @Test
    fun `word with empty original word should fail`() = runTest {
        // Given: A word with empty original word
        val word = createTestWord(
            originalWord = "",
            translation = "hola"
        )
        
        // When: Updating the word
        val result = useCase(word)
        
        // Then: Should fail with validation error
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("cannot be empty") == true)
        assertEquals(0, fakeRepository.updateCallCount)
    }
    
    @Test
    fun `word with blank original word should fail`() = runTest {
        // Given: A word with blank original word
        val word = createTestWord(
            originalWord = "   ",
            translation = "hola"
        )
        
        // When: Updating the word
        val result = useCase(word)
        
        // Then: Should fail with validation error
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("cannot be empty") == true)
        assertEquals(0, fakeRepository.updateCallCount)
    }
    
    @Test
    fun `word with empty translation should fail`() = runTest {
        // Given: A word with empty translation
        val word = createTestWord(
            originalWord = "hello",
            translation = ""
        )
        
        // When: Updating the word
        val result = useCase(word)
        
        // Then: Should fail with validation error
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("cannot be empty") == true)
        assertEquals(0, fakeRepository.updateCallCount)
    }
    
    @Test
    fun `word with blank translation should fail`() = runTest {
        // Given: A word with blank translation
        val word = createTestWord(
            originalWord = "hello",
            translation = "   \t  "
        )
        
        // When: Updating the word
        val result = useCase(word)
        
        // Then: Should fail with validation error
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("cannot be empty") == true)
        assertEquals(0, fakeRepository.updateCallCount)
    }
    
    @Test
    fun `word with empty description should succeed`() = runTest {
        // Given: A word with empty description
        val word = createTestWord(
            originalWord = "hello",
            translation = "hola",
            description = ""
        )
        
        // When: Updating the word
        val result = useCase(word)
        
        // Then: Should succeed (description is optional)
        assertTrue(result.isSuccess)
        assertEquals(word, result.getOrThrow())
        assertEquals(1, fakeRepository.updateCallCount)
    }
    
    @Test
    fun `word with special characters should succeed`() = runTest {
        // Given: A word with special characters
        val word = createTestWord(
            originalWord = "¡Hola! ¿Cómo estás?",
            translation = "Hello! How are you?",
            description = "Spanish greeting with punctuation"
        )
        
        // When: Updating the word
        val result = useCase(word)
        
        // Then: Should succeed
        assertTrue(result.isSuccess)
        assertEquals(word, result.getOrThrow())
        assertEquals(1, fakeRepository.updateCallCount)
    }
    
    @Test
    fun `word with unicode characters should succeed`() = runTest {
        // Given: A word with unicode characters
        val word = createTestWord(
            originalWord = "你好",
            translation = "Hello",
            description = "Chinese greeting"
        )
        
        // When: Updating the word
        val result = useCase(word)
        
        // Then: Should succeed
        assertTrue(result.isSuccess)
        assertEquals(word, result.getOrThrow())
        assertEquals(1, fakeRepository.updateCallCount)
    }
    
    @Test
    fun `word with very long text should succeed`() = runTest {
        // Given: A word with very long text
        val longText = "This is a very long phrase that could potentially cause issues with validation or storage"
        val word = createTestWord(
            originalWord = longText,
            translation = longText,
            description = longText
        )
        
        // When: Updating the word
        val result = useCase(word)
        
        // Then: Should succeed
        assertTrue(result.isSuccess)
        assertEquals(word, result.getOrThrow())
        assertEquals(1, fakeRepository.updateCallCount)
    }
    
    @Test
    fun `repository exception should be propagated`() = runTest {
        // Given: A valid word but repository throws exception
        val word = createTestWord(
            originalWord = "hello",
            translation = "hola"
        )
        fakeRepository.shouldThrowException = true
        
        // When: Updating the word
        val result = useCase(word)
        
        // Then: Should fail with repository exception
        assertTrue(result.isFailure)
        assertEquals("Repository error", result.exceptionOrNull()?.message)
        assertEquals(1, fakeRepository.updateCallCount)
    }
    
    @Test
    fun `multiple updates should work correctly`() = runTest {
        // Given: Multiple words to update
        val word1 = createTestWord(id = 1, originalWord = "hello", translation = "hola")
        val word2 = createTestWord(id = 2, originalWord = "goodbye", translation = "adiós")
        val word3 = createTestWord(id = 3, originalWord = "thanks", translation = "gracias")
        
        // When: Updating multiple words
        val result1 = useCase(word1)
        val result2 = useCase(word2)
        val result3 = useCase(word3)
        
        // Then: All should succeed
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertTrue(result3.isSuccess)
        assertEquals(3, fakeRepository.updateCallCount)
        assertEquals(word3, fakeRepository.lastUpdatedWord)
    }
    
    @Test
    fun `word with learning progress should preserve it`() = runTest {
        // Given: A word with learning progress
        val word = createTestWord(
            id = 1,
            originalWord = "hello",
            translation = "hola",
            level = 3,
            easeFactor = 2.2f,
            interval = 7,
            repetitions = 2
        )
        
        // When: Updating the word
        val result = useCase(word)
        
        // Then: Should succeed and preserve learning progress
        assertTrue(result.isSuccess)
        val updatedWord = result.getOrThrow()
        assertEquals(3, updatedWord.level)
        assertEquals(2.2f, updatedWord.easeFactor)
        assertEquals(7, updatedWord.interval)
        assertEquals(2, updatedWord.repetitions)
    }
    
    // Helper function to create test words
    private fun createTestWord(
        id: Int = 1,
        originalWord: String,
        translation: String,
        description: String = "",
        sourceLanguage: Language = Language.ENGLISH,
        targetLanguage: Language = Language.SPANISH,
        level: Int = 0,
        easeFactor: Float = 2.5f,
        interval: Int = 0,
        repetitions: Int = 0,
        lastReviewDate: Long = 0L,
        nextReviewDate: Long = 0L
    ) = Word(
        id = id,
        originalWord = originalWord,
        translation = translation,
        description = description,
        sourceLanguage = sourceLanguage,
        targetLanguage = targetLanguage,
        level = level,
        easeFactor = easeFactor,
        interval = interval,
        repetitions = repetitions,
        lastReviewDate = lastReviewDate,
        nextReviewDate = nextReviewDate
    )
}

/**
 * Fake repository for testing UpdateWordUseCase
 */
internal class FakeWordRepositoryForUpdate : IWordRepository {
    var updateCallCount = 0
    var lastUpdatedWord: Word? = null
    var shouldThrowException = false
    
    override suspend fun updateWord(word: Word): Try<Unit> {
        updateCallCount++
        lastUpdatedWord = word

        if (shouldThrowException) {
            return Try.failure(Exception("Repository error"))
        }
        return Try.success(Unit)
    }

    // Other methods not needed for this test
    override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(emptyList())
    override fun getAllWords() = kotlinx.coroutines.flow.flowOf<List<Word>>(emptyList())
    override fun getDueCards() = kotlinx.coroutines.flow.flowOf<List<Word>>(emptyList())
    override fun getWordsByStage(stage: domain.word.model.LearningStage) = kotlinx.coroutines.flow.flowOf<List<Word>>(emptyList())
    override suspend fun getWordById(id: Int) = null
    override suspend fun insertWords(words: List<Word>): Try<Int> = Try.success(words.size)
    override suspend fun deleteWord(id: Int): Try<Unit> = Try.success(Unit)
    override fun deleteWords(ids: List<Int>) = kotlinx.coroutines.flow.flowOf(domain.word.repository.DeleteWordsProgress.Completed(0))
    override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
    override suspend fun syncWithRemote() = Try.success(Unit)
    override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
    override fun getProgressStats() = kotlinx.coroutines.flow.flowOf(domain.word.model.ProgressStats())
    override suspend fun getTotalCount(): Try<Int> = Try.success(0)
    override suspend fun getDueCount(): Try<Int> = Try.success(0)
    override fun updateWordsLanguages(ids: List<Int>, sourceLanguage: String, targetLanguage: String): kotlinx.coroutines.flow.Flow<UpdateWordsLanguagesProgress> = kotlinx.coroutines.flow.flow { emit(UpdateWordsLanguagesProgress.Completed(ids.size)) }
}
