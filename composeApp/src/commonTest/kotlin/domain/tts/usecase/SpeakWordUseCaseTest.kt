package domain.tts.usecase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeakWordUseCaseTest {

    private val ttsRepo = FakeTtsRepository()
    private val useCase = SpeakWordUseCase(ttsRepo)

    @Test
    fun `speaks word with given language code`() = runTest {
        val result = useCase("hello", "en")

        assertTrue(result.isSuccess)
        assertTrue(ttsRepo.speakCalled)
        assertEquals("hello", ttsRepo.lastSpokenText)
        assertEquals("en", ttsRepo.lastSpokenLanguageCode)
    }

    @Test
    fun `skips speaking when language code is blank instead of guessing a voice`() = runTest {
        val result = useCase("hallo", " ")

        assertTrue(result.isSuccess)
        assertFalse(ttsRepo.speakCalled)
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
}
