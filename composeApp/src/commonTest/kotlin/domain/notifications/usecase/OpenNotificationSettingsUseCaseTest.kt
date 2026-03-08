package domain.notifications.usecase

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class OpenNotificationSettingsUseCaseTest {

    private val repository = FakeNotificationRepository()
    private val useCase = OpenNotificationSettingsUseCase(repository)

    @Test
    fun `opens notification settings successfully`() = runTest {
        val result = useCase()

        assertTrue(result.isSuccess)
        assertTrue(repository.openSettingsCalled)
    }

    @Test
    fun `invoke with Unit params delegates correctly`() = runTest {
        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        assertTrue(repository.openSettingsCalled)
    }

    @Test
    fun `returns failure on error`() = runTest {
        repository.shouldThrow = true

        val result = useCase()

        assertTrue(result.isFailure)
    }
}
