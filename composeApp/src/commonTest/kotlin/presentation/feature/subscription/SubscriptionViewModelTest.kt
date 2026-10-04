package presentation.feature.subscription

import app.cash.turbine.test
import core.common.Try
import core.common.UiState
import core.error.DomainError
import domain.auth.model.FeatureAccessResponse
import domain.auth.model.FeatureFlags
import domain.auth.model.UserFeatureAccess
import domain.auth.usecase.GetFeatureAccessUseCase
import domain.subscription.ISubscriptionManager
import domain.subscription.model.PackagePeriod
import domain.subscription.model.SubscriptionCustomerInfo
import domain.subscription.model.SubscriptionEntitlement
import domain.subscription.model.SubscriptionOffering
import domain.subscription.model.SubscriptionPackage
import domain.subscription.model.SubscriptionProduct
import domain.subscription.usecase.SyncSubscriptionWithServerUseCase
import fakes.FakeAnalyticsTracker
import fakes.FakeAuthRepository
import fakes.FakeSubscriptionAccessRepository
import feature.subscription.SubscriptionViewModel
import feature.subscription.model.MembershipStatus
import feature.subscription.model.SubscriptionContent
import feature.subscription.model.SubscriptionEffect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class SubscriptionViewModelTest : ViewModelTestBase() {

    private val monthly = SubscriptionPackage(
        identifier = "monthly",
        packagePeriod = PackagePeriod.MONTHLY,
        product = SubscriptionProduct("Monthly", "", "$4.99", 4_990_000, "vokab_monthly"),
    )
    private val annual = SubscriptionPackage(
        identifier = "annual",
        packagePeriod = PackagePeriod.ANNUAL,
        product = SubscriptionProduct("Annual", "", "$29.99", 29_990_000, "vokab_annual"),
    )
    private val noEntitlements = SubscriptionCustomerInfo(activeEntitlements = emptyMap())

    private fun storeMember(willRenew: Boolean = true) = SubscriptionCustomerInfo(
        activeEntitlements = mapOf(
            "premium" to SubscriptionEntitlement(
                identifier = "premium",
                isActive = true,
                expirationDateMillis = 4_000_000_000_000L,
                productIdentifier = "vokab_annual",
                willRenew = willRenew,
            )
        )
    )

    private var offeringsResult: Try<SubscriptionOffering> = Try.success(SubscriptionOffering(listOf(monthly, annual)))
    private var purchaseResult: Try<SubscriptionCustomerInfo> = Try.success(storeMember())
    private var restoreResult: Try<SubscriptionCustomerInfo> = Try.success(noEntitlements)
    private var manageResult: Try<Unit> = Try.success(Unit)
    private var refreshResult: SubscriptionCustomerInfo? = null
    private var refreshCount = 0
    private val purchased = mutableListOf<SubscriptionPackage>()
    private val customerInfoFlow = MutableStateFlow<SubscriptionCustomerInfo?>(null)
    private val backendAccess = MutableStateFlow(UserFeatureAccess())
    private val subscriptionAccessRepository = FakeSubscriptionAccessRepository()

    private val subscriptionManager = object : ISubscriptionManager {
        override val customerInfo = customerInfoFlow
        override suspend fun getOfferings() = offeringsResult
        override suspend fun purchase(packageToPurchase: SubscriptionPackage): Try<SubscriptionCustomerInfo> {
            purchased += packageToPurchase
            (purchaseResult as? Try.Success)?.let { customerInfoFlow.value = it.value }
            return purchaseResult
        }
        override suspend fun restore() = restoreResult
        override fun isSubscribed(): Flow<Boolean> = customerInfoFlow.map { it?.isSubscribed == true }
        override suspend fun logIn(userId: String) = Try.success(noEntitlements)
        override suspend fun logOut() = Try.success(noEntitlements)
        override fun getCurrentCustomerInfo() = customerInfoFlow.value
        override suspend fun refreshCustomerInfo(): Try<SubscriptionCustomerInfo> {
            refreshCount++
            refreshResult?.let { customerInfoFlow.value = it }
            return Try.success(customerInfoFlow.value ?: noEntitlements)
        }
        override suspend fun manageSubscription() = manageResult
    }

    private fun createViewModel(): SubscriptionViewModel {
        val authRepository = FakeAuthRepository().apply {
            featureAccessFlow = backendAccess.map { FeatureAccessResponse(FeatureFlags(), it) }
        }
        return SubscriptionViewModel(
            subscriptionManager = subscriptionManager,
            getFeatureAccessUseCase = GetFeatureAccessUseCase(authRepository, subscriptionManager),
            syncSubscriptionWithServerUseCase = SyncSubscriptionWithServerUseCase(subscriptionAccessRepository),
            analyticsTracker = FakeAnalyticsTracker(),
        )
    }

    private fun SubscriptionViewModel.loaded(): SubscriptionContent =
        assertIs<UiState.Loaded<SubscriptionContent>>(currentState.content).value

    private fun SubscriptionViewModel.membershipStatus(): MembershipStatus =
        assertIs<SubscriptionContent.Member>(loaded()).membership.status

    // region content

    @Test
    fun `free user sees paywall with annual preselected`() = runTest {
        val vm = createViewModel()

        val paywall = assertIs<SubscriptionContent.Paywall>(vm.loaded())
        assertEquals(listOf("annual", "monthly"), paywall.plans.map { it.identifier })
        assertEquals("annual", vm.currentState.selectedPlanId)
    }

    @Test
    fun `offerings failure for free user shows error with message key`() = runTest {
        offeringsResult = Try.failure(UnsupportedOperationException("WEB_SUBSCRIPTIONS_NOT_AVAILABLE"))

        val state = assertIs<UiState.Error>(createViewModel().currentState.content)

        assertEquals("WEB_SUBSCRIPTIONS_NOT_AVAILABLE", state.message)
    }

    @Test
    fun `retry after failure loads the paywall`() = runTest {
        offeringsResult = Try.failure(RuntimeException("offline"))
        val vm = createViewModel()

        offeringsResult = Try.success(SubscriptionOffering(listOf(monthly)))
        vm.retry()

        assertIs<SubscriptionContent.Paywall>(vm.loaded())
    }

    @Test
    fun `member sees membership even when offerings fail`() = runTest {
        customerInfoFlow.value = storeMember()
        offeringsResult = Try.failure(RuntimeException("offline"))

        assertIs<MembershipStatus.Renewing>(createViewModel().membershipStatus())
    }

    @Test
    fun `backend grant shows membership not paywall`() = runTest {
        backendAccess.value = UserFeatureAccess(hasPremiumAccess = true, source = "GRANT")

        val member = assertIs<SubscriptionContent.Member>(createViewModel().loaded())

        assertIs<MembershipStatus.Granted>(member.membership.status)
        assertFalse(member.membership.isManageable)
    }

    @Test
    fun `screen flips to membership when the store reports an entitlement`() = runTest {
        val vm = createViewModel()
        assertIs<SubscriptionContent.Paywall>(vm.loaded())

        customerInfoFlow.value = storeMember()

        assertIs<SubscriptionContent.Member>(vm.loaded())
    }

    // endregion

    // region purchase

    @Test
    fun `purchaseSelectedPlan buys the selected plan and syncs the server`() = runTest {
        val vm = createViewModel()
        vm.selectPlan("monthly")

        vm.purchaseSelectedPlan()

        assertEquals(listOf("monthly"), purchased.map { it.identifier })
        assertFalse(vm.currentState.isPurchasing)
        assertEquals(1, subscriptionAccessRepository.syncCount)
        assertIs<SubscriptionContent.Member>(vm.loaded())
    }

    @Test
    fun `purchase failure emits failure effect and does not sync`() = runTest {
        purchaseResult = Try.failure(RuntimeException("Payment declined"))
        val vm = createViewModel()

        vm.effects.test {
            vm.purchaseSelectedPlan()
            assertEquals(SubscriptionEffect.Failure("Payment declined"), awaitItem())
        }
        assertEquals(0, subscriptionAccessRepository.syncCount)
        assertFalse(vm.currentState.isPurchasing)
    }

    @Test
    fun `user closing the store sheet emits nothing`() = runTest {
        purchaseResult = Try.failure(DomainError.Commerce.PurchaseCancelled)
        val vm = createViewModel()

        vm.effects.test {
            vm.purchaseSelectedPlan()
            expectNoEvents()
        }
        assertEquals(0, subscriptionAccessRepository.syncCount)
    }

    @Test
    fun `server sync failure after purchase is silent`() = runTest {
        subscriptionAccessRepository.result = Try.failure(RuntimeException("429"))
        val vm = createViewModel()

        vm.effects.test {
            vm.purchaseSelectedPlan()
            expectNoEvents()
        }
    }

    // endregion

    // region restore & manage

    @Test
    fun `restore with nothing active emits NothingToRestore`() = runTest {
        val vm = createViewModel()

        vm.effects.test {
            vm.restorePurchases()
            assertEquals(SubscriptionEffect.NothingToRestore, awaitItem())
        }
        assertEquals(0, subscriptionAccessRepository.syncCount)
    }

    @Test
    fun `restore ignores inactive entitlements`() = runTest {
        val inactive = storeMember().activeEntitlements.mapValues { it.value.copy(isActive = false) }
        restoreResult = Try.success(SubscriptionCustomerInfo(inactive))
        val vm = createViewModel()

        vm.effects.test {
            vm.restorePurchases()
            assertEquals(SubscriptionEffect.NothingToRestore, awaitItem())
        }
    }

    @Test
    fun `restore with active entitlement emits PurchasesRestored and syncs`() = runTest {
        restoreResult = Try.success(storeMember())
        val vm = createViewModel()

        vm.effects.test {
            vm.restorePurchases()
            assertEquals(SubscriptionEffect.PurchasesRestored, awaitItem())
        }
        assertEquals(1, subscriptionAccessRepository.syncCount)
    }

    @Test
    fun `manage failure emits failure effect`() = runTest {
        manageResult = Try.failure(RuntimeException("No store"))
        val vm = createViewModel()

        vm.effects.test {
            vm.manageSubscription()
            assertEquals(SubscriptionEffect.Failure("No store"), awaitItem())
        }
    }

    // endregion

    // region returning from the store

    @Test
    fun `resume after canceling in the store shows the plan as canceled and syncs the server`() = runTest {
        customerInfoFlow.value = storeMember(willRenew = true)
        val vm = createViewModel()
        vm.onResume()
        assertIs<MembershipStatus.Renewing>(vm.membershipStatus())

        refreshResult = storeMember(willRenew = false)
        vm.manageSubscription()
        vm.onResume()

        assertIs<MembershipStatus.Canceled>(vm.membershipStatus())
        assertEquals(1, subscriptionAccessRepository.syncCount)
    }

    @Test
    fun `resume without a store change refreshes but does not sync`() = runTest {
        customerInfoFlow.value = storeMember()
        val vm = createViewModel()

        vm.onResume()
        vm.onResume()

        assertEquals(2, refreshCount)
        assertEquals(0, subscriptionAccessRepository.syncCount)
    }

    // endregion
}
