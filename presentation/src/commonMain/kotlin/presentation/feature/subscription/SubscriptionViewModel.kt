package presentation.feature.subscription

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
import presentation.ui.screens.SubscriptionData
import kotlinx.datetime.Instant as DateTimeInstant

data class SubscriptionScreenState(
    val content: UiState<SubscriptionData> = UiState.Loading,
    val isPurchasing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

class SubscriptionViewModel(
    private val subscriptionManager: ISubscriptionManager
) : BaseViewModel<SubscriptionScreenState, Nothing>() {

    override fun initialState() = SubscriptionScreenState()

    init {
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
        val dateTimeInstant = DateTimeInstant.fromEpochMilliseconds(epochMillis)
        val localDateTime = dateTimeInstant.toLocalDateTime(TimeZone.currentSystemDefault())
        val monthNames = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        val monthName = monthNames[localDateTime.monthNumber - 1]
        return "$monthName ${localDateTime.dayOfMonth}, ${localDateTime.year}"
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
                                    packages = offerings?.availablePackages ?: emptyList(),
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
        viewModelScope.launch {
            updateState { copy(isPurchasing = true, errorMessage = null) }
            subscriptionManager.purchase(packageToPurchase)
                .onSuccess {
                    updateState { copy(isPurchasing = false) }
                }
                .onFailure { error ->
                    updateState {
                        copy(
                            isPurchasing = false,
                            errorMessage = error.message ?: "PURCHASE_FAILED"
                        )
                    }
                }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            updateState { copy(errorMessage = null, successMessage = null) }

            subscriptionManager.restore()
                .onSuccess { customerInfo ->
                    val hasActiveEntitlements = customerInfo.activeEntitlements.isNotEmpty()
                    if (hasActiveEntitlements) {
                        updateState { copy(successMessage = "PURCHASES_RESTORED_SUCCESS") }
                    } else {
                        updateState { copy(errorMessage = "NO_PURCHASES_TO_RESTORE") }
                    }
                }
                .onFailure { error ->
                    updateState {
                        copy(errorMessage = error.message ?: "RESTORE_PURCHASES_FAILED")
                    }
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
