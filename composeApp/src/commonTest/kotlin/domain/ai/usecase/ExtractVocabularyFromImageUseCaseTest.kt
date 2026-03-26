package domain.ai.usecase

import core.common.Try
import domain.ai.repository.IAiRepository
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import domain.settings.usecase.GetCurrentLanguageUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExtractVocabularyFromImageUseCaseTest {

    private val fakeAiRepository = FakeExtractAiRepository()
    private val fakeSettingsRepository = FakeExtractSettingsRepository()
    private val getCurrentLanguageUseCase = GetCurrentLanguageUseCase(fakeSettingsRepository)
    private val useCase = ExtractVocabularyFromImageUseCase(fakeAiRepository, getCurrentLanguageUseCase)

    @Test
    fun `first emission is Loading`() = runTest {
        fakeAiRepository.extractResult = Try.success("hello,hola")

        val results = useCase(byteArrayOf(1, 2, 3)).toList()

        assertIs<ExtractVocabularyResult.Loading>(results.first())
    }

    @Test
    fun `second emission is Success with csv text on repo success`() = runTest {
        fakeAiRepository.extractResult = Try.success("hello,hola\nworld,mundo")

        val results = useCase(byteArrayOf(1, 2, 3)).toList()

        val success = results.last()
        assertIs<ExtractVocabularyResult.Success>(success)
        assertEquals("hello,hola\nworld,mundo", success.csvText)
    }

    @Test
    fun `second emission is Error when repository fails`() = runTest {
        fakeAiRepository.extractResult = Try.failure(RuntimeException("AI service unavailable"))

        val results = useCase(byteArrayOf(1, 2, 3)).toList()

        val error = results.last()
        assertIs<ExtractVocabularyResult.Error>(error)
        assertTrue(error.message.contains("AI service unavailable"))
    }

    @Test
    fun `uses targetLanguage from GetCurrentLanguageUseCase`() = runTest {
        fakeSettingsRepository.language = Language.GERMAN
        fakeAiRepository.extractResult = Try.success("wort,word")

        useCase(byteArrayOf(1, 2, 3)).toList()

        assertEquals(Language.GERMAN, fakeAiRepository.lastTargetLanguage)
    }

    @Test
    fun `invoke with Params delegates extraction options correctly`() = runTest {
        fakeAiRepository.extractResult = Try.success("hello,hola")

        val params = ExtractVocabularyFromImageUseCase.Params(
            imageBytes = byteArrayOf(1, 2, 3),
            extractWords = true,
            extractSentences = true,
        )
        val results = useCase(params).toList()

        assertTrue(results.isNotEmpty())
        assertTrue(fakeAiRepository.lastExtractWords == true)
        assertTrue(fakeAiRepository.lastExtractSentences == true)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fakes
    // ─────────────────────────────────────────────────────────────────────────

    private class FakeExtractAiRepository : IAiRepository {
        var extractResult: Try<String> = Try.success("")
        var lastTargetLanguage: Language? = null
        var lastExtractWords: Boolean? = null
        var lastExtractSentences: Boolean? = null

        override suspend fun extractVocabularyFromImage(
            imageBytes: ByteArray,
            targetLanguage: Language,
            extractWords: Boolean,
            extractSentences: Boolean,
        ): Try<String> {
            lastTargetLanguage = targetLanguage
            lastExtractWords = extractWords
            lastExtractSentences = extractSentences
            return extractResult
        }

    }

    private class FakeExtractSettingsRepository : ISettingsRepository {
        var language: Language = Language.ENGLISH

        override fun getLanguage(): Flow<Language> = flowOf(language)
        override suspend fun setLanguage(language: Language): Try<Unit> {
            this.language = language
            return Try.success(Unit)
        }
        override fun getThemeMode(): Flow<ThemeMode> = flowOf(ThemeMode.AUTO)
        override suspend fun setThemeMode(mode: ThemeMode): Try<Unit> = Try.success(Unit)
        override suspend fun clearSettings(): Try<Unit> = Try.success(Unit)
        override fun getNotificationsEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setNotificationsEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override fun getReviewRemindersEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setReviewRemindersEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override fun getMotivationalMessagesEnabled(): Flow<Boolean> = flowOf(true)
        override suspend fun setMotivationalMessagesEnabled(enabled: Boolean): Try<Unit> = Try.success(Unit)
        override suspend fun getDailyReminderTime(): Try<String> = Try.success("09:00")
        override suspend fun setDailyReminderTime(time: String): Try<Unit> = Try.success(Unit)
        override suspend fun getMinimumDueCards(): Try<Int> = Try.success(5)
        override suspend fun setMinimumDueCards(count: Int): Try<Unit> = Try.success(Unit)
    }
}
