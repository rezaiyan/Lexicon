package domain.auth.usecase

import app.cash.turbine.test
import fakes.FakeAuthRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveAuthStateUseCaseTest {

    private val fakeAuthRepository = FakeAuthRepository()
    private val useCase = ObserveAuthStateUseCase(fakeAuthRepository)

    @Test
    fun `emits false when not authenticated`() = runTest {
        fakeAuthRepository.authenticatedFlow = flowOf(false)

        useCase(Unit).test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits true when authenticated`() = runTest {
        fakeAuthRepository.authenticatedFlow = flowOf(true)

        useCase(Unit).test {
            assertEquals(true, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits multiple values from flow`() = runTest {
        fakeAuthRepository.authenticatedFlow = flowOf(false, true, false)

        useCase(Unit).test {
            assertEquals(false, awaitItem())
            assertEquals(true, awaitItem())
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `delegates directly to repository isAuthenticatedAsFlow`() = runTest {
        fakeAuthRepository.authenticatedFlow = flowOf(true, false)

        val collected = mutableListOf<Boolean>()
        useCase(Unit).test {
            collected.add(awaitItem())
            collected.add(awaitItem())
            awaitComplete()
        }

        assertEquals(listOf(true, false), collected)
    }
}
