package presentation.feature.subscription

import core.common.Try
import domain.subscription.ISubscriptionManager
import domain.subscription.model.SubscriptionCustomerInfo
import domain.subscription.model.SubscriptionEntitlement
import domain.subscription.model.SubscriptionOffering
import domain.subscription.model.SubscriptionPackage
import domain.subscription.model.SubscriptionProduct
import domain.subscription.model.PackagePeriod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import core.common.UiState
import presentation.ui.screens.SubscriptionData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
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
        override fun isSubscribed(): Flow<Boolean> = flowOf(false)
        override suspend fun logIn(userId: String): Try<SubscriptionCustomerInfo> = Try.success(testCustomerInfo)
        override suspend fun logOut(): Try<SubscriptionCustomerInfo> = Try.success(testCustomerInfo)
        override fun getCurrentCustomerInfo(): SubscriptionCustomerInfo? = customerInfoFlow.value
        override suspend fun manageSubscription(): Try<Unit> = manageResult
        override suspend fun cancelSubscription(): Try<Unit> = cancelResult
    }

    private fun createViewModel() = SubscriptionViewModel(fakeSubscriptionManager())

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
}
