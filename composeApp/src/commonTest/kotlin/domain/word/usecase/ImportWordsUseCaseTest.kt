package domain.word.usecase

import core.common.Try
import core.common.getOrThrow
import domain.word.model.Word
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.repository.IWordRepository
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.UpdateWordsLanguagesProgress
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import domain.settings.usecase.GetCurrentLanguageUseCase
import domain.word.service.ImportValidationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive tests for word import parsing logic
 * 
 * Tests cover:
 * - Basic word pairs
 * - Words with commas (phrases)
 * - Multiple words with descriptions
 * - Edge cases (empty, malformed, special characters)
 */
class ImportWordsUseCaseTest {
    
    private val fakeRepository = FakeWordRepositoryForImport()
    private val validationService = ImportValidationService()
    private val fakeSettingsRepository = FakeSettingsRepositoryForImport()
    private val getCurrentLanguageUseCase = GetCurrentLanguageUseCase(fakeSettingsRepository)
    private val useCase = ImportWordsUseCase(fakeRepository, validationService, getCurrentLanguageUseCase)
    
    private suspend fun executeImport(input: String): Try<Int> =
        useCase(input).first()

    private suspend fun executeImportSuccess(input: String): Int {
        val result = executeImport(input)
        return result.getOrThrow()
    }
    
    @Test
    fun `basic word pair should parse correctly`() = runTest {
        val input = "hello,hola"
        val result = executeImportSuccess(input)
        
        assertEquals(1, result)
        assertEquals(1, fakeRepository.insertedWords.size)

        val word = fakeRepository.insertedWords[0]
        assertEquals("hello", word.originalWord)
        assertEquals("hola", word.translation)
        assertEquals("", word.description)
    }
    
    @Test
    fun `multiple word pairs separated by semicolon should parse correctly`() = runTest {
        val input = "hello,hola;goodbye,adiós;thanks,gracias"
        val result = executeImportSuccess(input)
        
        assertEquals(3, result)
        assertEquals(3, fakeRepository.insertedWords.size)

        assertEquals("hello", fakeRepository.insertedWords[0].originalWord)
        assertEquals("hola", fakeRepository.insertedWords[0].translation)

        assertEquals("goodbye", fakeRepository.insertedWords[1].originalWord)
        assertEquals("adiós", fakeRepository.insertedWords[1].translation)

        assertEquals("thanks", fakeRepository.insertedWords[2].originalWord)
        assertEquals("gracias", fakeRepository.insertedWords[2].translation)
    }
    
    @Test
    fun `word with description should parse correctly`() = runTest {
        val input = "hello,hola,A common greeting"
        val result = executeImportSuccess(input)
        assertEquals(1, result)
        
        val word = fakeRepository.insertedWords[0]
        assertEquals("hello", word.originalWord)
        assertEquals("hola", word.translation)
        assertEquals("A common greeting", word.description)
    }
    
    @Test
    fun `phrase with commas using comma delimiter should parse with smart splitting`() = runTest {
        // With comma delimiter and limit=3, split on first 2 commas, preserve rest
        val input = "Hello my friend,Hola mi amigo"
        val result = executeImportSuccess(input)
        assertEquals(1, result)
        
        val word = fakeRepository.insertedWords[0]
        assertEquals("Hello my friend", word.originalWord)
        assertEquals("Hola mi amigo", word.translation)
        assertEquals("", word.description)
    }
    
    @Test
    fun `phrase with commas using comma delimiter splits on first TWO commas`() = runTest {
        // With comma delimiter and limit=3, split() splits on first 2 commas
        // This is expected behavior - users should use pipe for phrases with commas
        val input = "Hello, my friend,Hola, mi amigo"
        val result = executeImportSuccess(input)
        assertEquals(1, result)
        
        val word = fakeRepository.insertedWords[0]
        // split(",", limit=3) on "Hello, my friend,Hola, mi amigo" gives:
        // ["Hello", " my friend", "Hola, mi amigo"]
        assertEquals("Hello", word.originalWord)
        assertEquals("my friend", word.translation)
        assertEquals("Hola, mi amigo", word.description)
    }
    
    @Test
    fun `phrase with description using comma delimiter preserves commas in description`() = runTest {
        val input = "Hello my dear friend,Hola mi querido amigo,A formal greeting used in Spanish-speaking countries"
        val result = executeImportSuccess(input)
        assertEquals(1, result)
        
        val word = fakeRepository.insertedWords[0]
        assertEquals("Hello my dear friend", word.originalWord)
        assertEquals("Hola mi querido amigo", word.translation)
        assertEquals("A formal greeting used in Spanish-speaking countries", word.description)
    }
    
    @Test
    fun `multiple phrases using comma delimiter should parse correctly`() = runTest {
        val input = """
            Hello how are you,Hola cómo estás
            Good morning sir,Buenos días señor
            Thank you very much,Muchas gracias
        """.trimIndent()
        
        val result = executeImportSuccess(input)
        assertEquals(3, result)
        
        assertEquals("Hello how are you", fakeRepository.insertedWords[0].originalWord)
        assertEquals("Hola cómo estás", fakeRepository.insertedWords[0].translation)
        
        assertEquals("Good morning sir", fakeRepository.insertedWords[1].originalWord)
        assertEquals("Buenos días señor", fakeRepository.insertedWords[1].translation)
        
        assertEquals("Thank you very much", fakeRepository.insertedWords[2].originalWord)
        assertEquals("Muchas gracias", fakeRepository.insertedWords[2].translation)
    }
    
    @Test
    fun `very long phrase with description using comma delimiter should not truncate`() = runTest {
        val longPhrase = "This is a very long phrase with detailed information"
        val longTranslation = "Esta es una frase muy larga con información detallada"
        val longDescription = "This description has commas, subclauses, and extra details"
        val input = "$longPhrase,$longTranslation,$longDescription"
        
        val result = executeImportSuccess(input)
        assertEquals(1, result)
        
        val word = fakeRepository.insertedWords[0]
        assertEquals(longPhrase, word.originalWord)
        assertEquals(longTranslation, word.translation)
        assertEquals(longDescription, word.description)
    }
    
    @Test
    fun `description with commas should not split incorrectly using comma delimiter with limit`() = runTest {
        val input = "word,translation,This is a description with commas, multiple clauses, and details"
        val result = executeImportSuccess(input)
        assertEquals(1, result)
        
        val word = fakeRepository.insertedWords[0]
        assertEquals("word", word.originalWord)
        assertEquals("translation", word.translation)
        assertEquals("This is a description with commas, multiple clauses, and details", word.description)
    }
    
    @Test
    fun `description with commas using comma delimiter preserves commas in description`() = runTest {
        // With comma delimiter + limit=3, first 2 commas are delimiters, rest are preserved
        val input = "word,translation,This is a description with commas, multiple clauses, and details"
        val result = executeImportSuccess(input)
        assertEquals(1, result)
        
        val word = fakeRepository.insertedWords[0]
        assertEquals("word", word.originalWord)
        assertEquals("translation", word.translation)
        // The description should contain all the commas after the 2nd delimiter comma
        assertEquals("This is a description with commas, multiple clauses, and details", word.description)
    }
    
    @Test
    fun `empty input should return error`() = runTest {
        val result = executeImport("")
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `whitespace only should return error`() = runTest {
        val result = executeImport("   \n\n  \t  ")
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `malformed entry with only one part should be skipped`() = runTest {
        val input = "onlyoneword;hello,hola"
        val result = executeImportSuccess(input)
        assertEquals(1, result)
        assertEquals(1, fakeRepository.insertedWords.size)
        assertEquals("hello", fakeRepository.insertedWords[0].originalWord)
    }
    
    @Test
    fun `empty entries should be skipped`() = runTest {
        val input = "hello,hola;;;goodbye,adiós"
        val result = executeImportSuccess(input)
        assertEquals(2, result)
        assertEquals(2, fakeRepository.insertedWords.size)
    }
    
    @Test
    fun `entries with blank parts should be skipped`() = runTest {
        val input = " ,hola;hello, ;hello,hola"
        val result = executeImportSuccess(input)
        assertEquals(1, result)
        assertEquals(1, fakeRepository.insertedWords.size)
        assertEquals("hello", fakeRepository.insertedWords[0].originalWord)
    }
    
    @Test
    fun `mixed valid and invalid entries should parse valid ones only`() = runTest {
        val input = """
            hello,hola;
            invalid;
            good,bien,A positive word;
            ,empty;
            test,prueba
        """.trimIndent()
        
        val result = executeImportSuccess(input)
        assertEquals(3, result)
        assertEquals(3, fakeRepository.insertedWords.size)
    }
    
    @Test
    fun `real world example with comma delimiter should parse correctly`() = runTest {
        val input = """
            Hello,Hola,Common greeting
            How are you,Cómo estás
            I am fine thank you,Estoy bien gracias,Polite response
            See you later,Hasta luego
        """.trimIndent()
        
        val result = executeImportSuccess(input)
        assertEquals(4, result)
        
        // Verify first word
        assertEquals("Hello", fakeRepository.insertedWords[0].originalWord)
        assertEquals("Hola", fakeRepository.insertedWords[0].translation)
        assertEquals("Common greeting", fakeRepository.insertedWords[0].description)
        
        // Verify phrase with description
        assertEquals("I am fine thank you", fakeRepository.insertedWords[2].originalWord)
        assertEquals("Estoy bien gracias", fakeRepository.insertedWords[2].translation)
        assertEquals("Polite response", fakeRepository.insertedWords[2].description)
    }
    
    @Test
    fun `multiple entries with semicolon and newline separators should work`() = runTest {
        val input = """
            hello,hola
            How are you,Cómo estás;good,bien,positive word
        """.trimIndent()
        
        val result = executeImportSuccess(input)
        assertEquals(3, result)
        
        // First entry
        assertEquals("hello", fakeRepository.insertedWords[0].originalWord)
        assertEquals("hola", fakeRepository.insertedWords[0].translation)
        
        // Second entry
        assertEquals("How are you", fakeRepository.insertedWords[1].originalWord)
        assertEquals("Cómo estás", fakeRepository.insertedWords[1].translation)
        
        // Third entry with description
        assertEquals("good", fakeRepository.insertedWords[2].originalWord)
        assertEquals("bien", fakeRepository.insertedWords[2].translation)
        assertEquals("positive word", fakeRepository.insertedWords[2].description)
    }
    
    @Test
    fun `special characters should be preserved`() = runTest {
        val input = "¡Hola!,Hello!;¿Qué tal?,How are you?;Café,Coffee"
        val result = executeImportSuccess(input)
        assertEquals(3, result)
        
        assertEquals("¡Hola!", fakeRepository.insertedWords[0].originalWord)
        assertEquals("¿Qué tal?", fakeRepository.insertedWords[1].originalWord)
        assertEquals("Café", fakeRepository.insertedWords[2].originalWord)
    }
    
    @Test
    fun `newlines and extra whitespace should be handled`() = runTest {
        val input = """
            hello  ,  hola  ;

            goodbye,  adiós
        """.trimIndent()

        val result = executeImportSuccess(input)
        assertEquals(2, result)

        assertEquals("hello", fakeRepository.insertedWords[0].originalWord)
        assertEquals("hola", fakeRepository.insertedWords[0].translation)
    }

    @Test
    fun `explicit language params should be used when provided`() = runTest {
        val input = "bonjour,hola"
        val result = useCase.execute(
            text = input,
            sourceLanguage = Language.FRENCH,
            targetLanguage = Language.SPANISH
        )

        assertTrue(result.isSuccess)
        assertEquals(1, fakeRepository.insertedWords.size)

        val word = fakeRepository.insertedWords[0]
        assertEquals("bonjour", word.originalWord)
        assertEquals("hola", word.translation)
        assertEquals(Language.FRENCH, word.sourceLanguage)
        assertEquals(Language.SPANISH, word.targetLanguage)
    }

    @Test
    fun `default languages should be used when params are null`() = runTest {
        val input = "hello,hola"
        val result = useCase.execute(text = input)

        assertTrue(result.isSuccess)
        assertEquals(1, fakeRepository.insertedWords.size)

        val word = fakeRepository.insertedWords[0]
        assertEquals(Language.ENGLISH, word.sourceLanguage)
        // targetLanguage falls back to GetCurrentLanguageUseCase which returns ENGLISH
        assertEquals(Language.ENGLISH, word.targetLanguage)
    }
}

/**
 * Fake repository for testing ImportWordsUseCase
 */
internal class FakeWordRepositoryForImport : IWordRepository {
    val insertedWords = mutableListOf<Word>()
    
    override suspend fun insertWords(words: List<Word>): Int {
        insertedWords.addAll(words)
        return words.size
    }
    
    override suspend fun getAllWordsAsync(): List<Word> = insertedWords.toList()
    
    override fun getAllWords(): Flow<List<Word>> = flowOf(insertedWords)
    
    override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
    
    override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
    
    override suspend fun updateWord(word: Word) {
        // For testing purposes, just update the word in the list
        val index = insertedWords.indexOfFirst { it.id == word.id }
        if (index >= 0) {
            insertedWords[index] = word
        }
    }
    
    override suspend fun deleteWord(id: Int) {
        insertedWords.removeAll { it.id == id }
    }
    
    override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> =
        flowOf(DeleteWordsProgress.Completed(ids.size))
    
    override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
    override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
    override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
    
    override suspend fun getWordById(id: Int): Word? = insertedWords.find { it.id == id }
    
    override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats(
        totalWords = insertedWords.size,
        dueCards = 0,
        level0Count = 0,
        level1Count = 0,
        level2Count = 0,
        level3Count = 0,
        level4Count = 0,
        level5Count = 0,
        level6Count = 0
    ))
    
    override suspend fun getTotalCount(): Int = insertedWords.size

    override suspend fun getDueCount(): Int = 0

    override fun updateWordsLanguages(ids: List<Int>, sourceLanguage: String, targetLanguage: String): Flow<UpdateWordsLanguagesProgress> = flow { emit(UpdateWordsLanguagesProgress.Completed(ids.size)) }
}

internal class FakeSettingsRepositoryForImport : ISettingsRepository {
    override fun getLanguage(): Flow<Language> = flowOf(Language.ENGLISH)
    override suspend fun setLanguage(language: Language) {}
    override fun getThemeMode(): Flow<ThemeMode> = flowOf(ThemeMode.AUTO)
    override suspend fun setThemeMode(mode: ThemeMode) {}
    override suspend fun getLastInsightDate(): String? = null
    override suspend fun getCachedInsight(): String? = null
    override suspend fun updateDailyInsight(date: String, insight: String) {}
    override suspend fun getLastInsightDismissedTime(): Long = 0L
    override suspend fun setLastInsightDismissedTime(timestamp: Long) {}
    override suspend fun clearInsightData() {}
    override suspend fun clearSettings() {}
    override fun getNotificationsEnabled(): Flow<Boolean> = flowOf(true)
    override suspend fun setNotificationsEnabled(enabled: Boolean) {}
    override fun getReviewRemindersEnabled(): Flow<Boolean> = flowOf(true)
    override suspend fun setReviewRemindersEnabled(enabled: Boolean) {}
    override fun getMotivationalMessagesEnabled(): Flow<Boolean> = flowOf(true)
    override suspend fun setMotivationalMessagesEnabled(enabled: Boolean) {}
    override suspend fun getDailyReminderTime(): String = "18:00"
    override suspend fun setDailyReminderTime(time: String) {}
    override suspend fun getMinimumDueCards(): Int = 5
    override suspend fun setMinimumDueCards(count: Int) {}
}

