package presentation.feature.subscription

import fakes.FakeSubscriptionAccessRepository
import domain.subscription.usecase.SyncSubscriptionWithServerUseCase
import core.common.Try
import domain.subscription.ISubscriptionManager
import domain.subscription.model.SubscriptionCustomerInfo
import domain.subscription.model.SubscriptionEntitlement
import domain.subscription.model.SubscriptionOffering
import domain.subscription.model.SubscriptionPackage
import domain.subscription.model.SubscriptionProduct
import domain.subscription.model.PackagePeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import fakes.FakeAuthRepository
import domain.auth.model.FeatureAccessResponse
import domain.auth.model.FeatureFlags
import domain.auth.model.UserFeatureAccess
import domain.auth.usecase.GetFeatureAccessUseCase
import kotlinx.coroutines.test.runTest
import fakes.FakeAnalyticsTracker
import presentation.ViewModelTestBase
import core.common.UiState
import feature.subscription.SubscriptionViewModel
import feature.subscription.ui.SubscriptionData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import core.error.DomainError

class SubscriptionViewModelTest : ViewModelTestBase() {

    private val testPackage = SubscriptionPackage(
        identifier = "monthly",
        packagePeriod = PackagePeriod.MONTHLY,
        product = SubscriptionProduct(
            title = "Monthly",
            description = "Monthly subscription",
            priceFormatted = "$4.99"
        )
    )

    private val testOffering = SubscriptionOffering(
        availablePackages = listOf(testPackage)
    )

    private val testCustomerInfo = SubscriptionCustomerInfo(
        activeEntitlements = emptyMap()
    )

    private var offeringsResult: Try<SubscriptionOffering> = Try.success(testOffering)
    private var purchaseResult: Try<SubscriptionCustomerInfo> = Try.success(testCustomerInfo)
    private var restoreResult: Try<SubscriptionCustomerInfo> = Try.success(testCustomerInfo)
    private var manageResult: Try<Unit> = Try.success(Unit)
    private var cancelResult: Try<Unit> = Try.success(Unit)
    private val customerInfoFlow = MutableStateFlow<SubscriptionCustomerInfo?>(null)

    private fun fakeSubscriptionManager() = object : ISubscriptionManager {
        override val customerInfo = customerInfoFlow
        override suspend fun getOfferings(): Try<SubscriptionOffering> = offeringsResult
        override suspend fun purchase(packageToPurchase: SubscriptionPackage): Try<SubscriptionCustomerInfo> = purchaseResult
        override suspend fun restore(): Try<SubscriptionCustomerInfo> = restoreResult
        override fun isSubscribed(): Flow<Boolean> = customerInfoFlow.map { it?.isSubscribed == true }
        override suspend fun logIn(userId: String): Try<SubscriptionCustomerInfo> = Try.success(testCustomerInfo)
        override suspend fun logOut(): Try<SubscriptionCustomerInfo> = Try.success(testCustomerInfo)
        override fun getCurrentCustomerInfo(): SubscriptionCustomerInfo? = customerInfoFlow.value
        override suspend fun manageSubscription(): Try<Unit> = manageResult
        override suspend fun cancelSubscription(): Try<Unit> = cancelResult
    }

    private val backendAccess = MutableStateFlow(UserFeatureAccess())
    private val subscriptionAccessRepository = FakeSubscriptionAccessRepository()

    private fun createViewModel(): SubscriptionViewModel {
        val manager = fakeSubscriptionManager()
        val authRepository = FakeAuthRepository().apply {
            featureAccessFlow = backendAccess.map { FeatureAccessResponse(FeatureFlags(), it) }
        }
        return SubscriptionViewModel(
            subscriptionManager = manager,
            getFeatureAccessUseCase = GetFeatureAccessUseCase(authRepository, manager),
            syncSubscriptionWithServerUseCase = SyncSubscriptionWithServerUseCase(subscriptionAccessRepository),
            analyticsTracker = FakeAnalyticsTracker(),
        )
    }

    @Test
    fun `init loads offerings into Loaded state`() = runTest {
        val vm = createViewModel()
        val state = vm.currentState.content
        assertIs<UiState.Loaded<SubscriptionData>>(state)
        assertEquals(1, state.value.packages.size)
        assertEquals("monthly", state.value.packages.first().identifier)
    }

    @Test
    fun `loadOfferings failure sets Error state`() = runTest {
        offeringsResult = Try.failure(RuntimeException("Network error"))
        val vm = createViewModel()
        assertIs<UiState.Error>(vm.currentState.content)
    }

    @Test
    fun `purchasePackage success clears purchasing flag`() = runTest {
        val vm = createViewModel()

        vm.purchasePackage(testPackage)

        assertEquals(false, vm.currentState.isPurchasing)
        assertNull(vm.currentState.errorMessage)
    }

    @Test
    fun `purchasePackage failure sets error`() = runTest {
        purchaseResult = Try.failure(RuntimeException("Payment declined"))
        val vm = createViewModel()

        vm.purchasePackage(testPackage)

        assertEquals(false, vm.currentState.isPurchasing)
        assertEquals("Payment declined", vm.currentState.errorMessage)
    }

    @Test
    fun `restorePurchases with no entitlements sets error`() = runTest {
        val vm = createViewModel()

        vm.restorePurchases()

        assertEquals("NO_PURCHASES_TO_RESTORE", vm.currentState.errorMessage)
    }

    @Test
    fun `restorePurchases with active entitlements sets success`() = runTest {
        restoreResult = Try.success(
            SubscriptionCustomerInfo(
                activeEntitlements = mapOf(
                    "pro" to SubscriptionEntitlement(
                        identifier = "pro",
                        isActive = true,
                        expirationDateMillis = null,
                        productIdentifier = "monthly"
                    )
                )
            )
        )
        val vm = createViewModel()

        vm.restorePurchases()

        assertEquals("PURCHASES_RESTORED_SUCCESS", vm.currentState.successMessage)
        assertNull(vm.currentState.errorMessage)
    }

    @Test
    fun `clearError clears error message`() {
        purchaseResult = Try.failure(RuntimeException("error"))
        val vm = createViewModel()
        vm.purchasePackage(testPackage)
        assertEquals("error", vm.currentState.errorMessage)

        vm.clearError()
        assertNull(vm.currentState.errorMessage)
    }

    @Test
    fun `clearSuccess clears success message`() = runTest {
        restoreResult = Try.success(
            SubscriptionCustomerInfo(
                activeEntitlements = mapOf(
                    "pro" to SubscriptionEntitlement("pro", true, null, "monthly")
                )
            )
        )
        val vm = createViewModel()
        vm.restorePurchases()
        assertEquals("PURCHASES_RESTORED_SUCCESS", vm.currentState.successMessage)

        vm.clearSuccess()
        assertNull(vm.currentState.successMessage)
    }

    @Test
    fun `loadOfferings with web unsupported error shows specific message`() = runTest {
        offeringsResult = Try.failure(UnsupportedOperationException("WEB_SUBSCRIPTIONS_NOT_AVAILABLE"))
        val vm = createViewModel()
        val state = vm.currentState.content
        assertIs<UiState.Error>(state)
        assertEquals("WEB_SUBSCRIPTIONS_NOT_AVAILABLE", state.message)
    }

    @Test
    fun `retry reloads offerings`() = runTest {
        offeringsResult = Try.failure(RuntimeException("error"))
        val vm = createViewModel()
        assertIs<UiState.Error>(vm.currentState.content)

        offeringsResult = Try.success(testOffering)
        vm.retry()

        assertIs<UiState.Loaded<SubscriptionData>>(vm.currentState.content)
    }

    @Test
    fun `cancelSubscription failure sets error`() = runTest {
        cancelResult = Try.failure(RuntimeException("Cancel failed"))
        val vm = createViewModel()

        vm.cancelSubscription()

        assertEquals("Cancel failed", vm.currentState.errorMessage)
    }

    private val activeInfo = SubscriptionCustomerInfo(
        activeEntitlements = mapOf(
            "premium" to SubscriptionEntitlement(
                identifier = "premium",
                isActive = true,
                expirationDateMillis = 1_900_000_000_000L,
                productIdentifier = "monthly",
                willRenew = false,
            )
        )
    )

    @Test
    fun `purchasePackage when user cancels shows no error`() = runTest {
        purchaseResult = Try.failure(DomainError.Commerce.PurchaseCancelled)
        val vm = createViewModel()

        vm.purchasePackage(testPackage)

        assertEquals(false, vm.currentState.isPurchasing)
        assertNull(vm.currentState.errorMessage)
    }

    @Test
    fun `subscribed user still sees active plan when offerings fail to load`() = runTest {
        customerInfoFlow.value = activeInfo
        offeringsResult = Try.failure(RuntimeException("Network error"))

        val vm = createViewModel()

        val state = vm.currentState.content
        assertIs<UiState.Loaded<SubscriptionData>>(state)
        assertTrue(state.value.isSubscribed)
    }

    @Test
    fun `screen switches to active plan when store reports new entitlement`() = runTest {
        val vm = createViewModel()
        assertEquals(false, (vm.currentState.content as UiState.Loaded).value.isSubscribed)

        customerInfoFlow.value = activeInfo

        val data = (vm.currentState.content as UiState.Loaded).value
        assertTrue(data.isSubscribed)
        assertEquals(false, data.willRenew)
        assertNotNull(data.formattedExpirationDate)
    }

    @Test
    fun `entitlement arriving before offerings finish is not lost`() = runTest {
        customerInfoFlow.value = activeInfo
        val vm = createViewModel()

        assertTrue((vm.currentState.content as UiState.Loaded).value.isSubscribed)
    }

    @Test
    fun `restorePurchases ignores inactive entitlements`() = runTest {
        restoreResult = Try.success(
            SubscriptionCustomerInfo(
                activeEntitlements = mapOf(
                    "premium" to activeInfo.activeEntitlements.getValue("premium").copy(isActive = false)
                )
            )
        )
        val vm = createViewModel()

        vm.restorePurchases()

        assertEquals("NO_PURCHASES_TO_RESTORE", vm.currentState.errorMessage)
    }

    @Test
    fun `backend granted premium without store purchase shows active plan not paywall`() = runTest {
        backendAccess.value = UserFeatureAccess(hasPremiumAccess = true, source = "GRANT")

        val vm = createViewModel()

        val data = (vm.currentState.content as UiState.Loaded).value
        assertTrue(data.isSubscribed)
        assertEquals(false, data.hasStoreSubscription)
    }

    @Test
    fun `backend granted premium is shown even when offerings fail`() = runTest {
        backendAccess.value = UserFeatureAccess(hasPremiumAccess = true, source = "GRANT")
        offeringsResult = Try.failure(RuntimeException("Network error"))

        val vm = createViewModel()

        assertTrue((vm.currentState.content as UiState.Loaded).value.isSubscribed)
    }

    @Test
    fun `store subscription is manageable`() = runTest {
        customerInfoFlow.value = activeInfo

        val vm = createViewModel()

        val data = (vm.currentState.content as UiState.Loaded).value
        assertTrue(data.isSubscribed)
        assertTrue(data.hasStoreSubscription)
    }

    @Test
    fun `successful purchase syncs premium to the server`() = runTest {
        purchaseResult = Try.success(activeInfo)
        val vm = createViewModel()

        vm.purchasePackage(testPackage)

        assertEquals(1, subscriptionAccessRepository.syncCount)
    }

    @Test
    fun `cancelled or failed purchase does not sync`() = runTest {
        purchaseResult = Try.failure(DomainError.Commerce.PurchaseCancelled)
        val vm = createViewModel()

        vm.purchasePackage(testPackage)

        assertEquals(0, subscriptionAccessRepository.syncCount)
    }

    @Test
    fun `restore with entitlement syncs premium to the server`() = runTest {
        restoreResult = Try.success(activeInfo)
        val vm = createViewModel()

        vm.restorePurchases()

        assertEquals(1, subscriptionAccessRepository.syncCount)
    }

    @Test
    fun `server sync failure does not show an error after purchase`() = runTest {
        purchaseResult = Try.success(activeInfo)
        subscriptionAccessRepository.result = Try.failure(RuntimeException("429"))
        val vm = createViewModel()

        vm.purchasePackage(testPackage)

        assertNull(vm.currentState.errorMessage)
    }

    @Test
    fun `grant shows active plan without expiry renewal or trial`() = runTest {
        backendAccess.value = UserFeatureAccess(
            hasPremiumAccess = true,
            source = "GRANT",
            expiresAt = "2126-01-01T00:00:00Z",
        )

        val vm = createViewModel()

        val data = (vm.currentState.content as UiState.Loaded).value
        assertTrue(data.isSubscribed)
        assertEquals(false, data.hasStoreSubscription)
        assertNull(data.formattedExpirationDate)
        assertNull(data.expirationDateMillis)
        assertEquals(false, data.willRenew)
        assertEquals(false, data.isInTrial)
    }

    @Test
    fun `store purchase from another platform shows backend period`() = runTest {
        backendAccess.value = UserFeatureAccess(
            hasPremiumAccess = true,
            source = "STORE",
            expiresAt = "2030-03-17T17:46:40Z",
            willRenew = true,
            isTrial = true,
        )

        val vm = createViewModel()

        val data = (vm.currentState.content as UiState.Loaded).value
        assertTrue(data.isSubscribed)
        assertEquals(false, data.hasStoreSubscription)
        assertEquals(1_900_000_000_000L, data.expirationDateMillis)
        assertNotNull(data.formattedExpirationDate)
        assertTrue(data.willRenew)
        assertTrue(data.isInTrial)
    }

    @Test
    fun `store entitlement on this device wins over backend period`() = runTest {
        backendAccess.value = UserFeatureAccess(
            hasPremiumAccess = true,
            source = "STORE",
            expiresAt = "2027-01-01T00:00:00Z",
            willRenew = true,
        )
        customerInfoFlow.value = activeInfo

        val vm = createViewModel()

        val data = (vm.currentState.content as UiState.Loaded).value
        assertTrue(data.hasStoreSubscription)
        assertEquals(1_900_000_000_000L, data.expirationDateMillis)
        assertEquals(false, data.willRenew)
    }
}
