package domain.auth.usecase

import app.cash.turbine.test
import domain.auth.model.FeatureAccessResponse
import domain.auth.model.FeatureFlags
import domain.auth.model.PremiumSource
import domain.auth.model.UserFeatureAccess
import fakes.FakeSubscriptionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetFeatureAccessUseCaseTest {

    private val subscriptionManager = FakeSubscriptionManager()

    private fun access(premium: Boolean, push: Boolean = true) = FeatureAccessResponse(
        featureFlags = FeatureFlags(pushNotificationsEnabled = push),
        userAccess = UserFeatureAccess(hasPremiumAccess = premium)
    )

    private fun useCase(backend: FeatureAccessResponse) = GetFeatureAccessUseCase(
        authRepository = FakeAuthRepository().apply { featureAccessFlow = flowOf(backend) },
        subscriptionManager = subscriptionManager,
    )

    @Test
    fun `emits feature access response from repository`() = runTest {
        val expected = access(premium = true)

        assertEquals(expected, useCase(expected)().first())
    }

    @Test
    fun `invoke with Unit params delegates correctly`() = runTest {
        val expected = access(premium = false, push = false)

        assertEquals(expected, useCase(expected)(Unit).first())
    }

    @Test
    fun `premium access is false when backend and store both say no`() = runTest {
        val result = useCase(FeatureAccessResponse(FeatureFlags(), UserFeatureAccess()))().first()

        assertEquals(false, result.userAccess.hasPremiumAccess)
        assertEquals(true, result.featureFlags.pushNotificationsEnabled)
    }

    @Test
    fun `premium access is true when store entitlement is active but backend not yet updated`() = runTest {
        subscriptionManager.setSubscribed(true)

        val result = useCase(access(premium = false))().first()

        assertEquals(true, result.userAccess.hasPremiumAccess)
        assertEquals(PremiumSource.STORE, result.userAccess.premiumSource)
    }

    @Test
    fun `premium access flips to true right after a purchase`() = runTest {
        useCase(access(premium = false))().test {
            assertEquals(false, awaitItem().userAccess.hasPremiumAccess)

            subscriptionManager.setSubscribed(true)

            assertEquals(true, awaitItem().userAccess.hasPremiumAccess)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `backend premium grant is kept when store has no entitlement`() = runTest {
        subscriptionManager.setSubscribed(false)

        val result = useCase(access(premium = true))().first()

        assertEquals(true, result.userAccess.hasPremiumAccess)
    }
}
