package domain.ai.usecase

import core.common.Try
import domain.ai.repository.IAiRepository
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import domain.settings.usecase.GetCurrentLanguageUseCase
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import domain.word.service.IImportValidationService
import domain.word.usecase.ImportWordsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ImportFromImageUseCaseTest {

    private val aiRepository = FakeAiRepository()
    private val wordRepository = FakeWordRepo()
    private val validationService = FakeValidationService()
    private val settingsRepository = FakeSettingsRepo()
    private val getCurrentLanguageUseCase = GetCurrentLanguageUseCase(settingsRepository)
    private val importWordsUseCase = ImportWordsUseCase(wordRepository, validationService, getCurrentLanguageUseCase)
    private val useCase = ImportFromImageUseCase(aiRepository, importWordsUseCase, getCurrentLanguageUseCase)

    @Test
    fun `emits Success on successful extraction and import`() = runTest {
        aiRepository.extractResult = Try.success("hello,hola")
        validationService.parseResult = Try.success(
            listOf(testWord("hello", "hola"))
        )
        wordRepository.insertResult = Try.success(1)

        val results = useCase(byteArrayOf(1, 2, 3)).toList()

        // flatMapLatest consumes the Loading emission; downstream gets inner flow results
        val success = results.last()
        assertIs<ImportImageResult.Success>(success)
        assertEquals(1, success.count)
    }

    @Test
    fun `emits Error when extraction fails`() = runTest {
        aiRepository.extractResult = Try.failure(RuntimeException("AI service unavailable"))

        val results = useCase(byteArrayOf(1, 2, 3)).toList()

        val error = results.last()
        assertIs<ImportImageResult.Error>(error)
        assertTrue(error.message.contains("AI service unavailable"))
    }

    @Test
    fun `passes extraction options correctly`() = runTest {
        aiRepository.extractResult = Try.success("hello,hola")
        validationService.parseResult = Try.success(
            listOf(testWord("hello", "hola"))
        )
        wordRepository.insertResult = Try.success(1)

        useCase(
            imageBytes = byteArrayOf(1, 2, 3),
            extractWords = true,
            extractSentences = true
        ).toList()

        assertTrue(aiRepository.lastExtractWords == true)
        assertTrue(aiRepository.lastExtractSentences == true)
    }

    @Test
    fun `invoke with Params delegates correctly`() = runTest {
        aiRepository.extractResult = Try.success("hello,hola")
        validationService.parseResult = Try.success(
            listOf(testWord("hello", "hola"))
        )
        wordRepository.insertResult = Try.success(1)

        val params = ImportFromImageUseCase.Params(
            imageBytes = byteArrayOf(1, 2, 3),
            extractWords = true,
            extractSentences = false
        )
        val results = useCase(params).toList()

        assertTrue(results.isNotEmpty())
    }

    @Test
    fun `uses target language from getCurrentLanguageUseCase`() = runTest {
        aiRepository.extractResult = Try.success("hello,hola")
        validationService.parseResult = Try.success(
            listOf(testWord("hello", "hola"))
        )
        wordRepository.insertResult = Try.success(1)

        useCase(byteArrayOf(1, 2, 3)).toList()

        assertEquals(Language.ENGLISH, aiRepository.lastTargetLanguage)
    }

    private fun testWord(original: String, translation: String) = Word(
        id = 0, originalWord = original, translation = translation,
        description = "", sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.ENGLISH, nextReviewDate = 0L
    )

    private class FakeAiRepository : IAiRepository {
        var extractResult: Try<String> = Try.success("")
        var lastExtractWords: Boolean? = null
        var lastExtractSentences: Boolean? = null
        var lastTargetLanguage: Language? = null

        override suspend fun extractVocabularyFromImage(
            imageBytes: ByteArray,
            targetLanguage: Language,
            extractWords: Boolean,
            extractSentences: Boolean
        ): Try<String> {
            lastExtractWords = extractWords
            lastExtractSentences = extractSentences
            lastTargetLanguage = targetLanguage
            return extractResult
        }
    }

    private class FakeWordRepo : IWordRepository {
        var insertResult: Try<Int> = Try.success(1)

        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(emptyList())
        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun insertWords(words: List<Word>): Try<Int> = insertResult
        override suspend fun updateWord(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun deleteWord(id: Int): Try<Unit> = Try.success(Unit)
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf(DeleteWordsProgress.Completed(0))
        override fun updateWordsLanguages(ids: List<Int>, sourceLanguage: String, targetLanguage: String): Flow<UpdateWordsLanguagesProgress> = flow { emit(UpdateWordsLanguagesProgress.Completed(ids.size)) }
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats())
        override suspend fun getTotalCount(): Try<Int> = Try.success(0)
        override suspend fun getDueCount(): Try<Int> = Try.success(0)
    }

    private class FakeValidationService : IImportValidationService {
        var parseResult: Try<List<Word>> = Try.success(emptyList())

        override fun validateAndParse(
            text: String,
            sourceLanguage: Language,
            targetLanguage: Language
        ): Try<List<Word>> = parseResult
    }

    private class FakeSettingsRepo : ISettingsRepository {
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
        override suspend fun getDailyReminderTime(): String = "09:00"
        override suspend fun setDailyReminderTime(time: String) {}
        override suspend fun getMinimumDueCards(): Int = 5
        override suspend fun setMinimumDueCards(count: Int) {}
    }
}
