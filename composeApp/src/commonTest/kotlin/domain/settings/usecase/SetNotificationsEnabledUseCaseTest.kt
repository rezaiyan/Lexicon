package domain.settings.usecase

import core.common.getOrThrow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SetNotificationsEnabledUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val useCase = SetNotificationsEnabledUseCase(repository)

    @Test
    fun `enables notifications`() = runTest {
        repository.notificationsEnabled = false

        val result = useCase(true)

        assertTrue(result.isSuccess)
        assertTrue(repository.notificationsEnabled)
    }

    @Test
    fun `disables notifications`() = runTest {
        repository.notificationsEnabled = true

        val result = useCase(false)

        assertTrue(result.isSuccess)
        assertFalse(repository.notificationsEnabled)
    }

    @Test
    fun `invoke with params delegates correctly`() = runTest {
        val result = useCase.invoke(true)

        assertTrue(result.isSuccess)
        assertTrue(repository.notificationsEnabled)
    }
}
