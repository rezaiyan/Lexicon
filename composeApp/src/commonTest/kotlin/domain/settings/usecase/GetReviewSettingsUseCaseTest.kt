package domain.settings.usecase

import domain.settings.model.ReviewSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class GetReviewSettingsUseCaseTest {

    private val useCase = GetReviewSettingsUseCase()

    @Test
    fun `invoke returns BALANCED settings`() {
        val settings = useCase()
        assertEquals(ReviewSettings.BALANCED, settings)
    }

    @Test
    fun `invoke returns successesToAdvance of 1`() {
        val settings = useCase()
        assertEquals(1, settings.successesToAdvance)
    }

    @Test
    fun `invoke returns forgotPenalty of 2`() {
        val settings = useCase()
        assertEquals(2, settings.forgotPenalty)
    }

    @Test
    fun `multiple calls always return the same settings`() {
        val first = useCase()
        val second = useCase()
        assertEquals(first, second)
    }
}
