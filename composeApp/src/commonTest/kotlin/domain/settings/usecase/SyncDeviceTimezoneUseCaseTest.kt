package domain.settings.usecase

import fakes.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncDeviceTimezoneUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val useCase = SyncDeviceTimezoneUseCase(repository)

    @Test
    fun `invoke asks the repository to sync the device timezone`() = runTest {
        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(1, repository.timezoneSyncCount)
    }
}
