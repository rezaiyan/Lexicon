package domain.settings.usecase

import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetLanguageUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val useCase = SetLanguageUseCase(repository)

    @Test
    fun `sets language successfully`() = runTest {
        val result = useCase(Language.FRENCH)

        assertTrue(result.isSuccess)
        assertEquals(Language.FRENCH, repository.language)
    }

    @Test
    fun `sets different languages`() = runTest {
        useCase(Language.JAPANESE)
        assertEquals(Language.JAPANESE, repository.language)

        useCase(Language.ARABIC)
        assertEquals(Language.ARABIC, repository.language)
    }
}
