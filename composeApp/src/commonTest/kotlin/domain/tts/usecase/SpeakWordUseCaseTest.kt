package domain.tts.usecase
import core.common.Try
import domain.settings.repository.ISettingsRepository
import domain.settings.usecase.GetCurrentLanguageUseCase
import domain.settings.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeakWordUseCaseTest {

    private val ttsRepo = FakeTtsRepository()
    private val settingsRepo = FakeSettingsRepo()
    private val getCurrentLanguageUseCase = GetCurrentLanguageUseCase(settingsRepo)
    private val useCase = SpeakWordUseCase(ttsRepo, getCurrentLanguageUseCase)

    @Test
    fun `speaks word with given language code`() = runTest {
        val result = useCase("hello", "en")

        assertTrue(result.isSuccess)
        assertTrue(ttsRepo.speakCalled)
        assertEquals("hello", ttsRepo.lastSpokenText)
        assertEquals("en", ttsRepo.lastSpokenLanguageCode)
    }

    @Test
    fun `uses fallback language when language code is blank`() = runTest {
        settingsRepo.language = Language.GERMAN

        val result = useCase("hallo", "")

        assertTrue(result.isSuccess)
        assertEquals("de", ttsRepo.lastSpokenLanguageCode)
    }

    @Test
    fun `skips speaking when language not supported`() = runTest {
        ttsRepo.languageSupported = false

        val result = useCase("hello", "en")

        assertTrue(result.isSuccess)
        assertFalse(ttsRepo.speakCalled)
    }

    @Test
    fun `downloads model before speaking if not downloaded`() = runTest {
        ttsRepo.modelDownloaded = false

        val result = useCase("hello", "en")

        assertTrue(result.isSuccess)
        assertTrue(ttsRepo.speakCalled)
    }

    @Test
    fun `invoke with Params delegates correctly`() = runTest {
        val result = useCase(SpeakWordUseCase.Params("test", "fr"))

        assertTrue(result.isSuccess)
        assertEquals("test", ttsRepo.lastSpokenText)
        assertEquals("fr", ttsRepo.lastSpokenLanguageCode)
    }

    @Test
    fun `returns failure on TTS error`() = runTest {
        ttsRepo.shouldThrow = true

        val result = useCase("hello", "en")

        assertTrue(result.isFailure)
    }

    private class FakeSettingsRepo : ISettingsRepository {
        var language: Language = Language.ENGLISH
        override fun getLanguage(): Flow<Language> = flowOf(language)
        override suspend fun setLanguage(language: Language): Try<Unit> = Try.success(Unit)
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
