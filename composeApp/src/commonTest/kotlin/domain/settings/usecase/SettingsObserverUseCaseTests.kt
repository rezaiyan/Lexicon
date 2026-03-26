package domain.settings.usecase

import app.cash.turbine.test
import core.common.Try
import core.common.getOrThrow
import domain.settings.model.ThemeMode
import domain.settings.repository.ISettingsRepository
import domain.tts.model.TtsSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// Standalone fake that implements ISettingsRepository directly — avoids
// extending the final FakeSettingsRepository from the test module while
// still providing controllable TTS and skip-tag-selector behaviour.
// ---------------------------------------------------------------------------
private class EnhancedFakeSettingsRepository : ISettingsRepository {

    // Non-TTS required overrides (sensible defaults)
    override fun getLanguage(): Flow<Language> = flowOf(Language.ENGLISH)
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

    var skipTagSelectorValue: Boolean = false
    var lastSetSkipTagSelector: Boolean? = null
    var setSkipTagSelectorResult: Try<Unit> = Try.success(Unit)

    var ttsSettings: TtsSettings = TtsSettings()
    var lastSetSpeechRate: Float? = null
    var setSpeechRateResult: Try<Unit> = Try.success(Unit)

    var lastSetVoiceLanguageCode: String? = null
    var lastSetVoiceSpeakerId: Int? = null
    var setVoiceResult: Try<Unit> = Try.success(Unit)

    override fun getSkipTagSelector(): Flow<Boolean> = flowOf(skipTagSelectorValue)

    override suspend fun setSkipTagSelector(skip: Boolean): Try<Unit> {
        lastSetSkipTagSelector = skip
        skipTagSelectorValue = skip
        return setSkipTagSelectorResult
    }

    override fun getTtsSettings(): Flow<TtsSettings> = flowOf(ttsSettings)

    override suspend fun setTtsSpeechRate(rate: Float): Try<Unit> {
        lastSetSpeechRate = rate
        ttsSettings = ttsSettings.copy(speechRate = rate)
        return setSpeechRateResult
    }

    override suspend fun setTtsVoiceForLanguage(languageCode: String, speakerId: Int): Try<Unit> {
        lastSetVoiceLanguageCode = languageCode
        lastSetVoiceSpeakerId = speakerId
        return setVoiceResult
    }
}

// ---------------------------------------------------------------------------
// GetSkipTagSelectorUseCase
// ---------------------------------------------------------------------------
class GetSkipTagSelectorUseCaseTest {

    private val repository = EnhancedFakeSettingsRepository()
    private val useCase = GetSkipTagSelectorUseCase(repository)

    @Test
    fun `emits false when skip tag selector is false`() = runTest {
        repository.skipTagSelectorValue = false

        useCase(Unit).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits true when skip tag selector is true`() = runTest {
        repository.skipTagSelectorValue = true

        useCase(Unit).test {
            assertEquals(true, awaitItem())
            awaitComplete()
        }
    }
}

// ---------------------------------------------------------------------------
// SetSkipTagSelectorUseCase
// ---------------------------------------------------------------------------
class SetSkipTagSelectorUseCaseTest {

    private val repository = EnhancedFakeSettingsRepository()
    private val useCase = SetSkipTagSelectorUseCase(repository)

    @Test
    fun `returns success and stores true`() = runTest {
        val result = useCase(true)

        assertTrue(result.isSuccess)
        assertEquals(true, repository.lastSetSkipTagSelector)
        assertEquals(true, repository.skipTagSelectorValue)
    }

    @Test
    fun `returns success and stores false`() = runTest {
        repository.skipTagSelectorValue = true

        val result = useCase(false)

        assertTrue(result.isSuccess)
        assertEquals(false, repository.lastSetSkipTagSelector)
        assertEquals(false, repository.skipTagSelectorValue)
    }

    @Test
    fun `propagates failure from repository`() = runTest {
        repository.setSkipTagSelectorResult = Try.failure(RuntimeException("DB error"))

        val result = useCase(true)

        assertTrue(result.isFailure)
    }
}

// ---------------------------------------------------------------------------
// ObserveSpeechRateUseCase
// ---------------------------------------------------------------------------
class ObserveSpeechRateUseCaseTest {

    private val repository = EnhancedFakeSettingsRepository()
    private val useCase = ObserveSpeechRateUseCase(repository)

    @Test
    fun `emits default speech rate from TtsSettings`() = runTest {
        repository.ttsSettings = TtsSettings(speechRate = TtsSettings.DEFAULT_SPEECH_RATE)

        useCase(Unit).test {
            assertEquals(TtsSettings.DEFAULT_SPEECH_RATE, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits configured speech rate`() = runTest {
        repository.ttsSettings = TtsSettings(speechRate = 1.5f)

        useCase(Unit).test {
            assertEquals(1.5f, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits minimum speech rate`() = runTest {
        repository.ttsSettings = TtsSettings(speechRate = TtsSettings.MIN_SPEECH_RATE)

        useCase(Unit).test {
            assertEquals(TtsSettings.MIN_SPEECH_RATE, awaitItem())
            awaitComplete()
        }
    }
}

// ---------------------------------------------------------------------------
// SetTtsSpeechRateUseCase
// ---------------------------------------------------------------------------
class SetTtsSpeechRateUseCaseTest {

    private val repository = EnhancedFakeSettingsRepository()
    private val useCase = SetTtsSpeechRateUseCase(repository)

    @Test
    fun `returns success and stores speech rate`() = runTest {
        val result = useCase(1.5f)

        assertTrue(result.isSuccess)
        assertEquals(1.5f, repository.lastSetSpeechRate)
        assertEquals(1.5f, repository.ttsSettings.speechRate)
    }

    @Test
    fun `stores max speech rate correctly`() = runTest {
        val result = useCase(TtsSettings.MAX_SPEECH_RATE)

        assertTrue(result.isSuccess)
        assertEquals(TtsSettings.MAX_SPEECH_RATE, repository.lastSetSpeechRate)
    }

    @Test
    fun `propagates failure from repository`() = runTest {
        repository.setSpeechRateResult = Try.failure(RuntimeException("Write error"))

        val result = useCase(1.0f)

        assertTrue(result.isFailure)
    }
}

// ---------------------------------------------------------------------------
// SetTtsVoiceUseCase
// ---------------------------------------------------------------------------
class SetTtsVoiceUseCaseTest {

    private val repository = EnhancedFakeSettingsRepository()
    private val useCase = SetTtsVoiceUseCase(repository)

    @Test
    fun `returns success and forwards both params to repository`() = runTest {
        val params = SetTtsVoiceUseCase.Params(languageCode = "de", speakerId = 2)

        val result = useCase(params)

        assertTrue(result.isSuccess)
        assertEquals("de", repository.lastSetVoiceLanguageCode)
        assertEquals(2, repository.lastSetVoiceSpeakerId)
    }

    @Test
    fun `forwards different language code and speaker ID correctly`() = runTest {
        val params = SetTtsVoiceUseCase.Params(languageCode = "es", speakerId = 0)

        useCase(params)

        assertEquals("es", repository.lastSetVoiceLanguageCode)
        assertEquals(0, repository.lastSetVoiceSpeakerId)
    }

    @Test
    fun `propagates failure from repository`() = runTest {
        repository.setVoiceResult = Try.failure(RuntimeException("Voice error"))

        val result = useCase(SetTtsVoiceUseCase.Params(languageCode = "fr", speakerId = 1))

        assertTrue(result.isFailure)
    }
}
