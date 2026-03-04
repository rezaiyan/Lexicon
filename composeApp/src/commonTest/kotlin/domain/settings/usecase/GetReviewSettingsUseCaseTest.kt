package domain.settings.usecase

import core.common.getOrThrow
import domain.settings.model.ReviewSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetReviewSettingsUseCaseTest {

    private val useCase = GetReviewSettingsUseCase()

    @Test
    fun `invoke returns BALANCED settings`() = runTest {
        val settings = useCase(Unit).getOrThrow()
        assertEquals(ReviewSettings.BALANCED, settings)
    }

    @Test
    fun `invoke returns successesToAdvance of 1`() = runTest {
        val settings = useCase(Unit).getOrThrow()
        assertEquals(1, settings.successesToAdvance)
    }

    @Test
    fun `invoke returns forgotPenalty of 2`() = runTest {
        val settings = useCase(Unit).getOrThrow()
        assertEquals(2, settings.forgotPenalty)
    }

    @Test
    fun `multiple calls always return the same settings`() = runTest {
        val first = useCase(Unit).getOrThrow()
        val second = useCase(Unit).getOrThrow()
        assertEquals(first, second)
    }
}
