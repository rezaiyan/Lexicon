package domain.settings.usecase

import fakes.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SetReviewRemindersEnabledUseCaseTest {

    private val repository = FakeSettingsRepository()
    private val useCase = SetReviewRemindersEnabledUseCase(repository)

    @Test
    fun `enables review reminders`() = runTest {
        repository.reviewRemindersEnabled = false

        val result = useCase(true)

        assertTrue(result.isSuccess)
        assertTrue(repository.reviewRemindersEnabled)
    }

    @Test
    fun `disables review reminders`() = runTest {
        repository.reviewRemindersEnabled = true

        val result = useCase(false)

        assertTrue(result.isSuccess)
        assertFalse(repository.reviewRemindersEnabled)
    }

    @Test
    fun `invoke with params delegates correctly`() = runTest {
        val result = useCase.invoke(true)

        assertTrue(result.isSuccess)
        assertTrue(repository.reviewRemindersEnabled)
    }
}
