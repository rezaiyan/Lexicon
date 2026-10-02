package feature.subscription

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.UiState
import core.common.onFailure
import core.common.onSuccess
import core.error.DomainError
import core.error.toUserMessage
import domain.common.util.EpochDateFormatter
import domain.subscription.ISubscriptionManager
import domain.subscription.model.SubscriptionCustomerInfo
import domain.subscription.model.SubscriptionPackage
import feature.subscription.ui.SubscriptionData
import domain.auth.model.PremiumSource
import domain.auth.model.UserFeatureAccess
import domain.auth.usecase.GetFeatureAccessUseCase
import domain.subscription.usecase.SyncSubscriptionWithServerUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class SubscriptionScreenState(
    val content: UiState<SubscriptionData> = UiState.Loading,
    val isPurchasing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

/**
 * Paywall / subscription status screen.
 *
 * Screen content is derived from three inputs: the store offerings (loaded once, retryable), the
 * live store [ISubscriptionManager.customerInfo], and the app-wide premium gate
 * ([GetFeatureAccessUseCase] — also true for backend-granted premium with no store purchase).
 * Whenever any changes, [render] rebuilds the content, so a purchase, restore or renewal flips
 * the screen without extra bookkeeping.
 */
class SubscriptionViewModel(
    private val subscriptionManager: ISubscriptionManager,
    private val getFeatureAccessUseCase: GetFeatureAccessUseCase,
    private val syncSubscriptionWithServerUseCase: SyncSubscriptionWithServerUseCase,
    private val analyticsTracker: IAnalyticsTracker,
) : BaseViewModel<SubscriptionScreenState, Nothing>() {

    private var offerings: UiState<List<SubscriptionPackage>> = UiState.Loading
    private var access = UserFeatureAccess()

    override fun initialState() = SubscriptionScreenState()

    init {
        analyticsTracker.logEvent("subscription_screen_viewed")
        observeSubscriptionStatus()
        loadOfferings()
    }

    private fun observeSubscriptionStatus() {
        viewModelScope.launch {
            subscriptionManager.customerInfo.collect { render() }
        }
        viewModelScope.launch {
            getFeatureAccessUseCase()
                .map { it.userAccess }
                .distinctUntilChanged()
                .catch { emit(UserFeatureAccess()) }
                .collect {
                    access = it
                    render()
                }
        }
    }

    fun loadOfferings() {
        viewModelScope.launch {
            offerings = UiState.Loading
            updateState { copy(errorMessage = null) }
            render()
            subscriptionManager.getOfferings()
                .onSuccess { offerings = UiState.Loaded(it.availablePackages) }
                .onFailure { offerings = UiState.Error(it.toUserMessage(), it) }
            render()
        }
    }

    /** A subscriber never needs offerings, so their plan shows even if the store catalog fails. */
    private fun render() {
        val customerInfo = subscriptionManager.customerInfo.value
        val isSubscribed = access.hasPremiumAccess || customerInfo?.isSubscribed == true
        val current = offerings
        val content: UiState<SubscriptionData> = when {
            current is UiState.Loaded -> UiState.Loaded(subscriptionData(current.value, customerInfo))
            isSubscribed -> UiState.Loaded(subscriptionData(emptyList(), customerInfo))
            current is UiState.Error -> current
            else -> UiState.Loading
        }
        updateState { copy(content = content) }
    }

    private fun subscriptionData(
        packages: List<SubscriptionPackage>,
        customerInfo: SubscriptionCustomerInfo?,
    ): SubscriptionData {
        val entitlement = customerInfo?.primaryEntitlement
        val hasStoreSubscription = customerInfo?.isSubscribed == true
        val period = when {
            // This device's store knows the purchase: it is the freshest source.
            entitlement != null ->
                PlanPeriod(entitlement.expirationDateMillis, entitlement.willRenew, entitlement.isInTrial)
            // Bought on another platform (e.g. web): only the backend knows the period.
            access.premiumSource == PremiumSource.STORE ->
                PlanPeriod(access.expiresAtMillis, access.willRenew, access.isTrial)
            // Grants have no billing period: nothing expires, renews or can be managed.
            else -> PlanPeriod(expiresAtMillis = null, willRenew = false, isTrial = false)
        }
        return SubscriptionData(
            packages = packages,
            isSubscribed = access.hasPremiumAccess || hasStoreSubscription,
            hasStoreSubscription = hasStoreSubscription,
            customerInfo = customerInfo,
            formattedExpirationDate = period.expiresAtMillis?.let(EpochDateFormatter::toMediumDate),
            willRenew = period.willRenew,
            expirationDateMillis = period.expiresAtMillis,
            isInTrial = period.isTrial,
        )
    }

    private data class PlanPeriod(val expiresAtMillis: Long?, val willRenew: Boolean, val isTrial: Boolean)

    fun purchasePackage(packageToPurchase: SubscriptionPackage) {
        if (currentState.isPurchasing) return
        val packageParams = mapOf("package_id" to packageToPurchase.identifier)
        analyticsTracker.logEvent("subscription_plan_selected", packageParams)
        viewModelScope.launch {
            updateState { copy(isPurchasing = true, errorMessage = null) }
            analyticsTracker.logEvent("subscription_purchase_started", packageParams)
            subscriptionManager.purchase(packageToPurchase)
                .onSuccess { customerInfo ->
                    updateState { copy(isPurchasing = false) }
                    syncWithServer()
                    val isTrialStart = customerInfo.activeEntitlements.values.any { it.isInTrial }
                    analyticsTracker.logEvent(
                        if (isTrialStart) "trial_started" else "subscription_purchase_success",
                        packageParams
                    )
                }
                .onFailure { error ->
                    if (error is DomainError.Commerce.PurchaseCancelled) {
                        updateState { copy(isPurchasing = false) }
                        analyticsTracker.logEvent("subscription_purchase_cancelled", packageParams)
                    } else {
                        updateState { copy(isPurchasing = false, errorMessage = error.toUserMessage()) }
                        analyticsTracker.logEvent(
                            "subscription_purchase_failed",
                            packageParams + ("reason" to (error::class.simpleName ?: "unknown"))
                        )
                    }
                }
        }
    }

    fun restorePurchases() {
        analyticsTracker.logEvent("subscription_restore_tapped")
        viewModelScope.launch {
            updateState { copy(errorMessage = null, successMessage = null) }

            subscriptionManager.restore()
                .onSuccess { customerInfo ->
                    if (customerInfo.isSubscribed) {
                        updateState { copy(successMessage = "PURCHASES_RESTORED_SUCCESS") }
                        syncWithServer()
                        analyticsTracker.logEvent("subscription_restore_result", mapOf("success" to "true"))
                    } else {
                        updateState { copy(errorMessage = "NO_PURCHASES_TO_RESTORE") }
                        analyticsTracker.logEvent(
                            "subscription_restore_result",
                            mapOf("success" to "false", "reason" to "no_purchases"),
                        )
                    }
                }
                .onFailure { error ->
                    updateState { copy(errorMessage = error.toUserMessage()) }
                    analyticsTracker.logEvent(
                        "subscription_restore_result",
                        mapOf("success" to "false", "reason" to (error::class.simpleName ?: "unknown")),
                    )
                }
        }
    }

    /** Unlocks server-enforced premium now instead of waiting for the store webhook. Best effort. */
    private fun syncWithServer() {
        viewModelScope.launch { syncSubscriptionWithServerUseCase() }
    }

    fun clearError() {
        updateState { copy(errorMessage = null) }
    }

    fun clearSuccess() {
        updateState { copy(successMessage = null) }
    }

    fun retry() {
        loadOfferings()
    }

    fun manageSubscription() {
        viewModelScope.launch {
            updateState { copy(errorMessage = null, successMessage = null) }
            subscriptionManager.manageSubscription()
                .onFailure { error -> updateState { copy(errorMessage = error.toUserMessage()) } }
        }
    }

    fun cancelSubscription() {
        viewModelScope.launch {
            updateState { copy(errorMessage = null) }
            subscriptionManager.cancelSubscription()
                .onFailure { error -> updateState { copy(errorMessage = error.toUserMessage()) } }
        }
    }
}
