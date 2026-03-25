package feature.subscription

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import domain.subscription.ISubscriptionManager
import domain.subscription.model.SubscriptionPackage
import core.common.onFailure
import core.common.onSuccess
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import core.base.BaseViewModel
import core.common.UiState
import feature.subscription.ui.SubscriptionData
import kotlin.time.Instant

data class SubscriptionScreenState(
    val content: UiState<SubscriptionData> = UiState.Loading,
    val isPurchasing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

class SubscriptionViewModel(
    private val subscriptionManager: ISubscriptionManager,
    private val analyticsTracker: IAnalyticsTracker,
) : BaseViewModel<SubscriptionScreenState, Nothing>() {

    override fun initialState() = SubscriptionScreenState()

    init {
        analyticsTracker.logEvent("subscription_screen_viewed")
        loadOfferings()
        observeSubscriptionState()
    }

    private fun observeSubscriptionState() {
        viewModelScope.launch {
            combine(
                subscriptionManager.customerInfo,
                subscriptionManager.isSubscribed()
            ) { customerInfo, isSubscribed ->
                customerInfo to isSubscribed
            }.collect { (customerInfo, isSubscribed) ->
                val existingData = (currentState.content as? UiState.Loaded)?.value ?: return@collect

                val activeEntitlement = customerInfo?.activeEntitlements?.values?.firstOrNull()
                val expirationDateMillis = activeEntitlement?.expirationDateMillis
                val formattedExpirationDate = expirationDateMillis?.let { formatDate(it) }
                val willRenew = activeEntitlement?.willRenew == true

                updateState {
                    copy(
                        content = UiState.Loaded(
                            existingData.copy(
                                customerInfo = customerInfo ?: existingData.customerInfo,
                                isSubscribed = isSubscribed,
                                formattedExpirationDate = formattedExpirationDate,
                                willRenew = willRenew
                            )
                        )
                    )
                }
            }
        }
    }

    private fun formatDate(epochMillis: Long): String {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val monthNames = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        val monthName = monthNames[localDateTime.month.ordinal]
        return "$monthName ${localDateTime.day}, ${localDateTime.year}"
    }

    fun loadOfferings() {
        viewModelScope.launch {
            updateState { copy(content = UiState.Loading, errorMessage = null) }
            subscriptionManager.getOfferings()
                .onSuccess { offerings ->
                    val isSubscribed = subscriptionManager.isSubscribed().first()
                    val customerInfo = subscriptionManager.customerInfo.value
                    val activeEntitlement = customerInfo?.activeEntitlements?.values?.firstOrNull()
                    val formattedExpirationDate = activeEntitlement?.expirationDateMillis?.let { formatDate(it) }
                    val willRenew = activeEntitlement?.willRenew == true

                    updateState {
                        copy(
                            content = UiState.Loaded(
                                SubscriptionData(
                                    packages = offerings.availablePackages,
                                    isSubscribed = isSubscribed,
                                    customerInfo = customerInfo,
                                    formattedExpirationDate = formattedExpirationDate,
                                    willRenew = willRenew
                                )
                            )
                        )
                    }
                }
                .onFailure { error ->
                    updateState {
                        copy(content = UiState.Error(error.message ?: "SUBSCRIPTION_LOAD_FAILED"))
                    }
                }
        }
    }

    fun purchasePackage(packageToPurchase: SubscriptionPackage) {
        analyticsTracker.logEvent(
            "subscription_plan_selected",
            mapOf("package_id" to packageToPurchase.identifier)
        )
        viewModelScope.launch {
            updateState { copy(isPurchasing = true, errorMessage = null) }
            analyticsTracker.logEvent(
                "subscription_purchase_started",
                mapOf("package_id" to packageToPurchase.identifier)
            )
            subscriptionManager.purchase(packageToPurchase)
                .onSuccess { customerInfo ->
                    updateState { copy(isPurchasing = false) }
                    val isTrialStart = customerInfo.activeEntitlements.values.any { it.isInTrial }
                    if (isTrialStart) {
                        analyticsTracker.logEvent(
                            "trial_started",
                            mapOf("package_id" to packageToPurchase.identifier)
                        )
                    } else {
                        analyticsTracker.logEvent(
                            "subscription_purchase_success",
                            mapOf("package_id" to packageToPurchase.identifier)
                        )
                    }
                }
                .onFailure { error ->
                    updateState {
                        copy(
                            isPurchasing = false,
                            errorMessage = error.message ?: "PURCHASE_FAILED"
                        )
                    }
                    analyticsTracker.logEvent(
                        "subscription_purchase_failed",
                        mapOf("package_id" to packageToPurchase.identifier, "reason" to (error.message ?: "unknown"))
                    )
                }
        }
    }

    fun restorePurchases() {
        analyticsTracker.logEvent("subscription_restore_tapped")
        viewModelScope.launch {
            updateState { copy(errorMessage = null, successMessage = null) }

            subscriptionManager.restore()
                .onSuccess { customerInfo ->
                    val hasActiveEntitlements = customerInfo.activeEntitlements.isNotEmpty()
                    if (hasActiveEntitlements) {
                        updateState { copy(successMessage = "PURCHASES_RESTORED_SUCCESS") }
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
                    updateState {
                        copy(errorMessage = error.message ?: "RESTORE_PURCHASES_FAILED")
                    }
                    analyticsTracker.logEvent(
                        "subscription_restore_result",
                        mapOf("success" to "false", "reason" to (error.message ?: "unknown")),
                    )
                }
        }
    }

    fun clearError() {
        updateState { copy(errorMessage = null) }
    }

    fun clearSuccess() {
        updateState { copy(successMessage = null) }
    }

    fun retry() {
        updateState { copy(errorMessage = null) }
        loadOfferings()
    }

    fun manageSubscription() {
        viewModelScope.launch {
            updateState { copy(errorMessage = null, successMessage = null) }

            subscriptionManager.manageSubscription()
                .onFailure { error ->
                    updateState {
                        copy(errorMessage = error.message ?: "SUBSCRIPTION_INFO_UNAVAILABLE")
                    }
                }
        }
    }

    fun cancelSubscription() {
        viewModelScope.launch {
            updateState { copy(errorMessage = null) }

            subscriptionManager.cancelSubscription()
                .onFailure { error ->
                    updateState {
                        copy(errorMessage = error.message ?: "CANCEL_SUBSCRIPTION_FAILED")
                    }
                }
        }
    }
}
