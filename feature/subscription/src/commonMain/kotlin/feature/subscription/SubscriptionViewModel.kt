package feature.subscription

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.UiState
import core.common.onFailure
import core.common.onSuccess
import core.error.DomainError
import core.error.toUserMessage
import domain.auth.model.UserFeatureAccess
import domain.auth.usecase.GetFeatureAccessUseCase
import domain.subscription.ISubscriptionManager
import domain.subscription.model.SubscriptionPackage
import domain.subscription.usecase.SyncSubscriptionWithServerUseCase
import feature.subscription.model.SubscriptionContent
import feature.subscription.model.SubscriptionEffect
import feature.subscription.model.SubscriptionScreenState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Paywall / membership screen.
 *
 * Content is derived from three inputs: the store offerings (loaded once, retryable), the live
 * store [ISubscriptionManager.customerInfo], and the app-wide premium gate
 * ([GetFeatureAccessUseCase] — also true for backend-granted premium). Whenever any changes,
 * [render] rebuilds the content, so a purchase, restore, renewal or cancel flips the screen
 * without extra bookkeeping.
 */
class SubscriptionViewModel(
    private val subscriptionManager: ISubscriptionManager,
    private val getFeatureAccessUseCase: GetFeatureAccessUseCase,
    private val syncSubscriptionWithServerUseCase: SyncSubscriptionWithServerUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val contentFactory: SubscriptionContentFactory = SubscriptionContentFactory(),
) : BaseViewModel<SubscriptionScreenState, SubscriptionEffect>() {

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
            render()
            subscriptionManager.getOfferings()
                .onSuccess { offerings = UiState.Loaded(it.availablePackages) }
                .onFailure { offerings = UiState.Error(it.toUserMessage(), it) }
            render()
        }
    }

    fun retry() = loadOfferings()

    /** A member never needs offerings, so their membership shows even if the store catalog fails. */
    private fun render() {
        val customerInfo = subscriptionManager.customerInfo.value
        val isMember = access.hasPremiumAccess || customerInfo?.isSubscribed == true
        val current = offerings
        val content: UiState<SubscriptionContent> = when {
            current is UiState.Loaded -> UiState.Loaded(contentFactory.content(current.value, customerInfo, access))
            isMember -> UiState.Loaded(contentFactory.content(emptyList(), customerInfo, access))
            current is UiState.Error -> current
            else -> UiState.Loading
        }
        updateState {
            val plans = content.paywall()?.plans.orEmpty()
            val selection = selectedPlanId?.takeIf { id -> plans.any { it.identifier == id } }
                ?: PlanPricing.defaultSelection(plans)
            copy(content = content, selectedPlanId = selection)
        }
    }

    fun selectPlan(identifier: String) {
        if (currentState.isPurchasing || identifier == currentState.selectedPlanId) return
        analyticsTracker.logEvent("subscription_plan_toggled", mapOf("package_id" to identifier))
        updateState { copy(selectedPlanId = identifier) }
    }

    /** Buys the plan picked in the selector; the paywall has a single CTA. */
    fun purchaseSelectedPlan() {
        val selected = currentState.content.paywall()?.plans
            ?.firstOrNull { it.identifier == currentState.selectedPlanId }
            ?: return
        purchase(selected.pkg)
    }

    private fun purchase(packageToPurchase: SubscriptionPackage) {
        if (currentState.isPurchasing) return
        val packageParams = mapOf("package_id" to packageToPurchase.identifier)
        analyticsTracker.logEvent("subscription_plan_selected", packageParams)
        viewModelScope.launch {
            updateState { copy(isPurchasing = true) }
            analyticsTracker.logEvent("subscription_purchase_started", packageParams)
            subscriptionManager.purchase(packageToPurchase)
                .onSuccess { customerInfo ->
                    syncWithServer()
                    val isTrialStart = customerInfo.activeEntitlements.values.any { it.isInTrial }
                    analyticsTracker.logEvent(
                        if (isTrialStart) "trial_started" else "subscription_purchase_success",
                        packageParams,
                    )
                }
                .onFailure { error ->
                    if (error is DomainError.Commerce.PurchaseCancelled) {
                        analyticsTracker.logEvent("subscription_purchase_cancelled", packageParams)
                    } else {
                        emitEffect(SubscriptionEffect.Failure(error.toUserMessage()))
                        analyticsTracker.logEvent(
                            "subscription_purchase_failed",
                            packageParams + ("reason" to (error::class.simpleName ?: "unknown")),
                        )
                    }
                }
            updateState { copy(isPurchasing = false) }
        }
    }

    fun restorePurchases() {
        analyticsTracker.logEvent("subscription_restore_tapped")
        viewModelScope.launch {
            subscriptionManager.restore()
                .onSuccess { customerInfo ->
                    if (customerInfo.isSubscribed) {
                        emitEffect(SubscriptionEffect.PurchasesRestored)
                        syncWithServer()
                        analyticsTracker.logEvent("subscription_restore_result", mapOf("success" to "true"))
                    } else {
                        emitEffect(SubscriptionEffect.NothingToRestore)
                        analyticsTracker.logEvent(
                            "subscription_restore_result",
                            mapOf("success" to "false", "reason" to "no_purchases"),
                        )
                    }
                }
                .onFailure { error ->
                    emitEffect(SubscriptionEffect.Failure(error.toUserMessage()))
                    analyticsTracker.logEvent(
                        "subscription_restore_result",
                        mapOf("success" to "false", "reason" to (error::class.simpleName ?: "unknown")),
                    )
                }
        }
    }

    /** Stores don't allow in-app cancel or resubscribe; both happen on the store's page. */
    fun manageSubscription() {
        viewModelScope.launch {
            subscriptionManager.manageSubscription()
                .onFailure { error -> emitEffect(SubscriptionEffect.Failure(error.toUserMessage())) }
        }
    }

    /**
     * Called each time the screen comes to the foreground. Store-side changes (a cancel or
     * resubscribe in Google Play / App Store) aren't pushed to the device and the SDK caches
     * status for minutes, so re-fetch it; if it changed, sync the server so app-wide premium
     * follows.
     */
    fun onResume() {
        viewModelScope.launch {
            val before = subscriptionManager.customerInfo.value
            subscriptionManager.refreshCustomerInfo()
                .onSuccess { if (it != before) syncWithServer() }
        }
    }

    /** Unlocks server-enforced premium now instead of waiting for the store webhook. Best effort. */
    private fun syncWithServer() {
        viewModelScope.launch { syncSubscriptionWithServerUseCase() }
    }

    private fun UiState<SubscriptionContent>.paywall(): SubscriptionContent.Paywall? =
        (this as? UiState.Loaded)?.value as? SubscriptionContent.Paywall
}
