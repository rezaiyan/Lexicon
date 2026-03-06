package domain.settings.usecase

import domain.settings.model.ThemeMode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetThemeModeUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val useCase = SetThemeModeUseCase(repository)

    @Test
    fun `sets theme mode to dark`() = runTest {
        val result = useCase(ThemeMode.DARK)

        assertTrue(result.isSuccess)
        assertEquals(ThemeMode.DARK, repository.themeMode)
    }

    @Test
    fun `sets theme mode to light`() = runTest {
        val result = useCase(ThemeMode.LIGHT)

        assertTrue(result.isSuccess)
        assertEquals(ThemeMode.LIGHT, repository.themeMode)
    }

    @Test
    fun `sets theme mode to auto`() = runTest {
        repository.themeMode = ThemeMode.DARK

        val result = useCase(ThemeMode.AUTO)

        assertTrue(result.isSuccess)
        assertEquals(ThemeMode.AUTO, repository.themeMode)
    }
}
