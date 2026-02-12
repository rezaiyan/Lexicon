package data.subscription

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Offerings
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction
import domain.subscription.ISubscriptionManager
import expects.openUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class RevenueCatSubscriptionManager(
) : ISubscriptionManager, PurchasesDelegate {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _customerInfo = MutableStateFlow<CustomerInfo?>(null)
    override val customerInfo: StateFlow<CustomerInfo?> = _customerInfo.asStateFlow()

    private var lastSyncedRequestDate: Long? = null

    init {
        Purchases.sharedInstance.delegate = this
        fetchCustomerInfo()
    }

    private fun fetchCustomerInfo() {
        Purchases.sharedInstance.getCustomerInfo(
            onError = { },
            onSuccess = { info ->
                _customerInfo.value = info
            }
        )
    }

    private suspend fun getCustomerInfo(): Result<CustomerInfo> =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.getCustomerInfo(
                onError = { error ->
                    continuation.resume(Result.failure(Exception(error.message)))
                },
                onSuccess = { customerInfo ->
                    _customerInfo.value = customerInfo
                    continuation.resume(Result.success(customerInfo))
                }
            )
        }

    override suspend fun getOfferings(): Result<Offerings> =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.getOfferings(
                onError = { error ->
                    continuation.resume(Result.failure(Exception(error.message)))
                },
                onSuccess = { offerings ->
                    continuation.resume(Result.success(offerings))
                }
            )
        }

    override suspend fun purchase(packageToPurchase: Package): Result<CustomerInfo> =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.purchase(
                packageToPurchase = packageToPurchase,
                onError = { error: PurchasesError, userCancelled: Boolean ->
                    if (userCancelled) {
                        continuation.resume(Result.failure(CancelledPurchaseException()))
                    } else {
                        continuation.resume(Result.failure(Exception(error.message)))
                    }
                },
                onSuccess = { _: StoreTransaction, customerInfo: CustomerInfo ->
                    _customerInfo.value = customerInfo
                    continuation.resume(Result.success(customerInfo))
                }
            )
        }

    override suspend fun purchase(product: StoreProduct): Result<CustomerInfo> =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.purchase(
                storeProduct = product,
                onError = { error: PurchasesError, userCancelled: Boolean ->
                    if (userCancelled) {
                        continuation.resume(Result.failure(CancelledPurchaseException()))
                    } else {
                        continuation.resume(Result.failure(Exception(error.message)))
                    }
                },
                onSuccess = { _: StoreTransaction, customerInfo: CustomerInfo ->
                    _customerInfo.value = customerInfo
                    continuation.resume(Result.success(customerInfo))
                }
            )
        }

    override suspend fun restore(): Result<CustomerInfo> =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.restorePurchases(
                onError = { error ->
                    continuation.resume(Result.failure(Exception(error.message)))
                },
                onSuccess = { customerInfo ->
                    _customerInfo.value = customerInfo
                    continuation.resume(Result.success(customerInfo))
                }
            )
        }

    override fun isSubscribed(): Flow<Boolean> = customerInfo.map { info ->
        info?.entitlements?.active?.isNotEmpty() ?: false
    }

    override suspend fun logIn(userId: String): Result<CustomerInfo> =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.logIn(
                newAppUserID = userId,
                onError = { error ->
                    continuation.resume(Result.failure(Exception(error.message)))
                },
                onSuccess = { customerInfo, _ ->
                    _customerInfo.value = customerInfo
                    continuation.resume(Result.success(customerInfo))
                }
            )
        }

    override suspend fun logOut(): Result<CustomerInfo> =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.logOut(
                onError = { error ->
                    continuation.resume(Result.failure(Exception(error.message)))
                },
                onSuccess = { customerInfo ->
                    _customerInfo.value = customerInfo
                    continuation.resume(Result.success(customerInfo))
                }
            )
        }

    override fun getCurrentCustomerInfo(): CustomerInfo? = _customerInfo.value

    override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
        val requestDate = customerInfo.requestDate.toEpochMilliseconds()
        val isNewUpdate = requestDate != lastSyncedRequestDate

        _customerInfo.value = customerInfo

        if (isNewUpdate) {
            coroutineScope.launch {
                lastSyncedRequestDate = requestDate
            }
        }
    }

    override fun onPurchasePromoProduct(
        product: StoreProduct,
        startPurchase: (
            onError: (error: PurchasesError, userCancelled: Boolean) -> Unit,
            onSuccess: (storeTransaction: StoreTransaction, customerInfo: CustomerInfo) -> Unit
        ) -> Unit
    ) {
    }

    override suspend fun manageSubscription(): Result<Unit> {
        val customerInfoResult = getCustomerInfo()

        val customerInfo = customerInfoResult.getOrNull()
            ?: return Result.failure(Exception("Failed to retrieve subscription information"))

        val managementURL = customerInfo.managementUrlString

        if (managementURL.isNullOrBlank()) {
            return Result.failure(Exception("Unable to open subscription management. Please manage your subscription through your device settings."))
        }

        return Result.success(openUrl(managementURL))
    }

    override suspend fun cancelSubscription(): Result<Unit> {
        return manageSubscription()
    }

}

class CancelledPurchaseException : Exception("Purchase was cancelled by user")


