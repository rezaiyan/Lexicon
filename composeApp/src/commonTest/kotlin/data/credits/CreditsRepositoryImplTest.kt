package data.credits

import app.cash.turbine.test
import core.common.Try
import core.common.exceptionOrNull
import core.common.getOrThrow
import core.error.DomainError
import data.credits.remote.ICreditsRemoteDataSource
import data.credits.remote.model.CreditBalanceDto
import data.credits.repository.CreditsRepositoryImpl
import domain.auth.model.AuthUser
import domain.credits.model.CreditAction
import domain.credits.model.CreditTier
import fakes.FakeUserManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class CreditsRepositoryImplTest {

    private val alice = AuthUser(1L, "a@b.c", "Alice")
    private val bob = AuthUser(2L, "b@b.c", "Bob")
    private val userManager = FakeUserManager(alice)
    private val remote = FakeCreditsRemoteDataSource()
    private val testScope = TestScope()

    // On the test's scheduler, so advanceUntilIdle runs the re-reads invalidate() starts
    private val repository = CreditsRepositoryImpl(
        remote, userManager, CoroutineScope(StandardTestDispatcher(testScope.testScheduler)),
    )

    @Test
    fun `refresh publishes the server balance mapped to the domain`() = testScope.runTest {
        remote.result = Try.success(dto(allowance = 2, bonus = 15))

        val balance = repository.refresh().getOrThrow()

        assertEquals(17, balance.balance)
        assertEquals(CreditTier.FREE, balance.tier)
        assertEquals(3, balance.costOf(CreditAction.PHOTO_EXTRACTION))
        assertEquals(300, balance.premiumMonthlyAllowance)
        assertEquals(1_793_872_800_000, balance.periodEndsAtMillis)
        repository.observeBalance().test { assertEquals(balance, awaitItem()) }
    }

    @Test
    fun `balance is unknown until loaded`() = testScope.runTest {
        repository.observeBalance().test { assertNull(awaitItem()) }
    }

    @Test
    fun `a failed refresh keeps the last known balance`() = testScope.runTest {
        remote.result = Try.success(dto(allowance = 5))
        repository.refresh()
        remote.result = Try.failure(DomainError.Network.NoConnection)

        assertIs<DomainError.Network.NoConnection>(repository.refresh().exceptionOrNull())
        repository.observeBalance().test { assertEquals(20, awaitItem()?.balance) }
    }

    @Test
    fun `one account's balance is never shown to another`() = testScope.runTest {
        remote.result = Try.success(dto(allowance = 5))
        repository.refresh()

        repository.observeBalance().test {
            assertEquals(20, awaitItem()?.balance)
            userManager.setUser(bob)
            assertNull(awaitItem())
            userManager.setUser(null)
            expectNoEvents() // still null
        }
    }

    @Test
    fun `refresh when signed out fails without calling the server`() = testScope.runTest {
        userManager.setUser(null)

        assertIs<DomainError.Auth.NotAuthenticated>(repository.refresh().exceptionOrNull())
        assertEquals(0, remote.calls)
    }

    @Test
    fun `unknown actions and tiers from a newer server are ignored`() = testScope.runTest {
        remote.result = Try.success(
            dto().copy(
                tier = "PLATINUM",
                costs = mapOf("PHOTO_EXTRACTION" to 3, "VOICE_CHAT" to 9),
            )
        )

        val balance = repository.refresh().getOrThrow()

        assertEquals(CreditTier.FREE, balance.tier)
        assertEquals(mapOf(CreditAction.PHOTO_EXTRACTION to 3), balance.costs)
    }

    @Test
    fun `invalidate re-reads the balance in the background`() = testScope.runTest {
        remote.result = Try.success(dto(allowance = 5))
        repository.refresh()
        remote.result = Try.success(dto(allowance = 4))

        repository.invalidate()
        advanceUntilIdle()

        repository.observeBalance().test { assertEquals(19, awaitItem()?.balance) }
    }

    @Test
    fun `a slow earlier read can't overwrite a later one`() = testScope.runTest {
        val slow = CompletableDeferred<Unit>()
        remote.responses += { slow.await(); Try.success(dto(allowance = 5)) } // read before a spend
        remote.responses += { Try.success(dto(allowance = 4)) } // read after it

        repository.invalidate()
        runCurrent()
        repository.invalidate()
        runCurrent()
        slow.complete(Unit)
        advanceUntilIdle()

        repository.observeBalance().test { assertEquals(19, awaitItem()?.balance) }
    }

    private fun dto(allowance: Int = 5, bonus: Int = 15) = CreditBalanceDto(
        allowanceRemaining = allowance,
        monthlyAllowance = 5,
        bonusBalance = bonus,
        periodEndsAt = "2026-11-05T10:00:00Z",
        tier = "FREE",
        costs = mapOf("PHOTO_EXTRACTION" to 3, "AI_SUGGESTION" to 1),
        monthlyAllowances = mapOf("FREE" to 5, "TRIAL" to 30, "PREMIUM" to 300),
    )

    private class FakeCreditsRemoteDataSource : ICreditsRemoteDataSource {
        var result: Try<CreditBalanceDto> = Try.failure(IllegalStateException("not set"))
        /** Answers for the next calls, in order; [result] once they run out. */
        val responses = ArrayDeque<suspend () -> Try<CreditBalanceDto>>()
        var calls = 0
        override suspend fun fetchBalance(): Try<CreditBalanceDto> {
            calls++
            return responses.removeFirstOrNull()?.invoke() ?: result
        }
    }
}
