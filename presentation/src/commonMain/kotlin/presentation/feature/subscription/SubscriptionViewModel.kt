package presentation.feature.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.subscription.ISubscriptionManager
import domain.subscription.model.SubscriptionCustomerInfo
import domain.subscription.model.SubscriptionOffering
import domain.subscription.model.SubscriptionPackage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import presentation.model.UiState
import presentation.ui.screens.SubscriptionData
import presentation.util.stateInWhileSubscribed
import kotlinx.datetime.Instant as DateTimeInstant

data class SubscriptionUiState(
    val isPurchasing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

class SubscriptionViewModel(
    private val subscriptionManager: ISubscriptionManager
) : ViewModel() {

    val customerInfo = subscriptionManager.customerInfo
    val isSubscribed = subscriptionManager.isSubscribed()

    private val _offerings = MutableStateFlow<SubscriptionOffering?>(null)
    val offerings: StateFlow<SubscriptionOffering?> = _offerings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    val state: StateFlow<UiState<SubscriptionData>> = combine(
        _offerings,
        _isLoading,
        _loadError,
        customerInfo,
        isSubscribed
    ) { offerings, isLoading, loadError, customerInfo, isSubscribed ->
        when {
            isLoading && offerings == null -> UiState.Loading
            loadError != null -> UiState.Error(loadError)
            else -> {
                val activeEntitlement = customerInfo?.activeEntitlements?.values?.firstOrNull()
                val expirationDateMillis = activeEntitlement?.expirationDateMillis
                val formattedExpirationDate = expirationDateMillis?.let { formatDate(it) }

                UiState.Loaded(
                    SubscriptionData(
                        packages = offerings?.availablePackages ?: emptyList(),
                        isSubscribed = isSubscribed,
                        customerInfo = customerInfo,
                        formattedExpirationDate = formattedExpirationDate
                    )
                )
            }
        }
    }.stateInWhileSubscribed(
        scope = viewModelScope,
        initialValue = UiState.Loading
    )

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

    init {
        loadOfferings()
    }

    fun loadOfferings() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            _uiState.value = _uiState.value.copy(errorMessage = null)
            subscriptionManager.getOfferings()
                .onSuccess { offerings ->
                    _offerings.value = offerings
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _isLoading.value = false
                    _loadError.value = error.message ?: "SUBSCRIPTION_LOAD_FAILED"
                }
        }
    }

    fun purchasePackage(packageToPurchase: SubscriptionPackage) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPurchasing = true, errorMessage = null)
            subscriptionManager.purchase(packageToPurchase)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isPurchasing = false)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isPurchasing = false,
                        errorMessage = error.message ?: "PURCHASE_FAILED"
                    )
                }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)

            subscriptionManager.restore()
                .onSuccess { customerInfo ->
                    _isLoading.value = false
                    val hasActiveEntitlements = customerInfo.activeEntitlements.isNotEmpty()
                    if (hasActiveEntitlements) {
                        _uiState.value = _uiState.value.copy(successMessage = "PURCHASES_RESTORED_SUCCESS")
                    } else {
                        _uiState.value = _uiState.value.copy(errorMessage = "NO_PURCHASES_TO_RESTORE")
                    }
                }
                .onFailure { error ->
                    _isLoading.value = false
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "RESTORE_PURCHASES_FAILED"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun retry() {
        _loadError.value = null
        loadOfferings()
    }

    fun manageSubscription() {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)

            subscriptionManager.manageSubscription()
                .onSuccess {
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _isLoading.value = false
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "SUBSCRIPTION_INFO_UNAVAILABLE"
                    )
                }
        }
    }

    fun cancelSubscription() {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = _uiState.value.copy(errorMessage = null)

            subscriptionManager.cancelSubscription()
                .onSuccess {
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _isLoading.value = false
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "CANCEL_SUBSCRIPTION_FAILED"
                    )
                }
        }
    }
}
