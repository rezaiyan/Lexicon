package domain.support

import core.common.Try
import core.error.DomainError
import domain.support.model.MAX_SUPPORT_MESSAGE_LENGTH
import domain.support.usecase.SendSupportMessageUseCase
import fakes.FakeSupportRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SendSupportMessageUseCaseTest {

    private val repository = FakeSupportRepository()
    private val useCase = SendSupportMessageUseCase(repository)

    @Test
    fun `invoke trims the message before sending`() = runTest {
        useCase("  hello  ")

        assertEquals(listOf("hello"), repository.sent)
    }

    @Test
    fun `invoke with blank text fails without sending`() = runTest {
        val result = useCase("   ")

        assertIs<DomainError.Validation.BlankField>((result as Try.Failure).throwable)
        assertTrue(repository.sent.isEmpty())
    }

    @Test
    fun `invoke cuts overlong text to the backend limit`() = runTest {
        useCase("y".repeat(MAX_SUPPORT_MESSAGE_LENGTH + 1))

        assertEquals(MAX_SUPPORT_MESSAGE_LENGTH, repository.sent.single().length)
    }
}
