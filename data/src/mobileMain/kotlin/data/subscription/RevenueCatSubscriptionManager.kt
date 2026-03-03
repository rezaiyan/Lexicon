package data.subscription

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Offerings
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PackageType
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction
import domain.common.Try
import domain.common.fold
import domain.common.getOrNull
import domain.subscription.ISubscriptionManager
import domain.subscription.model.PackagePeriod
import domain.subscription.model.SubscriptionCustomerInfo
import domain.subscription.model.SubscriptionEntitlement
import domain.subscription.model.SubscriptionOffering
import domain.subscription.model.SubscriptionPackage
import domain.subscription.model.SubscriptionProduct
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

    private val _customerInfo = MutableStateFlow<SubscriptionCustomerInfo?>(null)
    override val customerInfo: StateFlow<SubscriptionCustomerInfo?> = _customerInfo.asStateFlow()

    private var lastSyncedRequestDate: Long? = null

    // Keep reference to raw RC packages for purchase operations
    private var cachedPackages: Map<String, Package> = emptyMap()

    init {
        Purchases.sharedInstance.delegate = this
        fetchCustomerInfo()
    }

    private fun fetchCustomerInfo() {
        Purchases.sharedInstance.getCustomerInfo(
            onError = { },
            onSuccess = { info ->
                _customerInfo.value = info.toDomain()
            }
        )
    }

    private suspend fun getRawCustomerInfo(): Try<CustomerInfo> =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.getCustomerInfo(
                onError = { error ->
                    continuation.resume(Try.failure(Exception(error.message)))
                },
                onSuccess = { customerInfo ->
                    _customerInfo.value = customerInfo.toDomain()
                    continuation.resume(Try.success(customerInfo))
                }
            )
        }

    override suspend fun getOfferings(): Try<SubscriptionOffering> =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.getOfferings(
                onError = { error ->
                    continuation.resume(Try.failure(Exception(error.message)))
                },
                onSuccess = { offerings ->
                    val current = offerings.current
                    if (current != null) {
                        val packages = current.availablePackages
                        cachedPackages = packages.associateBy { it.identifier }
                        continuation.resume(Try.success(offerings.toDomain()))
                    } else {
                        continuation.resume(Try.success(SubscriptionOffering(emptyList())))
                    }
                }
            )
        }

    override suspend fun purchase(packageToPurchase: SubscriptionPackage): Try<SubscriptionCustomerInfo> {
        val rcPackage = cachedPackages[packageToPurchase.identifier]
            ?: return Try.failure(Exception("Package not found: ${packageToPurchase.identifier}"))

        return suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.purchase(
                packageToPurchase = rcPackage,
                onError = { error: PurchasesError, userCancelled: Boolean ->
                    if (userCancelled) {
                        continuation.resume(Try.failure(CancelledPurchaseException()))
                    } else {
                        continuation.resume(Try.failure(Exception(error.message)))
                    }
                },
                onSuccess = { _: StoreTransaction, customerInfo: CustomerInfo ->
                    val domainInfo = customerInfo.toDomain()
                    _customerInfo.value = domainInfo
                    continuation.resume(Try.success(domainInfo))
                }
            )
        }
    }

    override suspend fun restore(): Try<SubscriptionCustomerInfo> =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.restorePurchases(
                onError = { error ->
                    continuation.resume(Try.failure(Exception(error.message)))
                },
                onSuccess = { customerInfo ->
                    val domainInfo = customerInfo.toDomain()
                    _customerInfo.value = domainInfo
                    continuation.resume(Try.success(domainInfo))
                }
            )
        }

    override fun isSubscribed(): Flow<Boolean> = customerInfo.map { info ->
        info?.activeEntitlements?.isNotEmpty() ?: false
    }

    override suspend fun logIn(userId: String): Try<SubscriptionCustomerInfo> =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.logIn(
                newAppUserID = userId,
                onError = { error ->
                    continuation.resume(Try.failure(Exception(error.message)))
                },
                onSuccess = { customerInfo, _ ->
                    val domainInfo = customerInfo.toDomain()
                    _customerInfo.value = domainInfo
                    continuation.resume(Try.success(domainInfo))
                }
            )
        }

    override suspend fun logOut(): Try<SubscriptionCustomerInfo> =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.logOut(
                onError = { error ->
                    continuation.resume(Try.failure(Exception(error.message)))
                },
                onSuccess = { customerInfo ->
                    val domainInfo = customerInfo.toDomain()
                    _customerInfo.value = domainInfo
                    continuation.resume(Try.success(domainInfo))
                }
            )
        }

    override fun getCurrentCustomerInfo(): SubscriptionCustomerInfo? = _customerInfo.value

    override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
        val requestDate = customerInfo.requestDate.toEpochMilliseconds()
        val isNewUpdate = requestDate != lastSyncedRequestDate

        _customerInfo.value = customerInfo.toDomain()

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

    override suspend fun manageSubscription(): Try<Unit> {
        val customerInfoResult = getRawCustomerInfo()

        val rawCustomerInfo = customerInfoResult.getOrNull()
            ?: return Try.failure(Exception("Failed to retrieve subscription information"))

        val managementURL = rawCustomerInfo.managementUrlString

        if (managementURL.isNullOrBlank()) {
            return Try.failure(Exception("Unable to open subscription management. Please manage your subscription through your device settings."))
        }

        return Try.success(openUrl(managementURL))
    }

    override suspend fun cancelSubscription(): Try<Unit> {
        return manageSubscription()
    }
}

// Mapper extensions: RevenueCat types -> Domain types

private fun CustomerInfo.toDomain(): SubscriptionCustomerInfo {
    val activeEntitlements = entitlements.active.mapValues { (_, entitlement) ->
        SubscriptionEntitlement(
            identifier = entitlement.identifier,
            isActive = entitlement.isActive,
            expirationDateMillis = entitlement.expirationDate?.toEpochMilliseconds(),
            productIdentifier = entitlement.productIdentifier,
            willRenew = entitlement.willRenew
        )
    }
    return SubscriptionCustomerInfo(
        activeEntitlements = activeEntitlements,
        managementUrlString = managementUrlString
    )
}

private fun Offerings.toDomain(): SubscriptionOffering {
    val packages = current?.availablePackages?.map { it.toDomain() } ?: emptyList()
    return SubscriptionOffering(availablePackages = packages)
}

private fun Package.toDomain(): SubscriptionPackage {
    val period = when (packageType) {
        PackageType.MONTHLY -> PackagePeriod.MONTHLY
        PackageType.ANNUAL -> PackagePeriod.ANNUAL
        PackageType.LIFETIME -> PackagePeriod.LIFETIME
        else -> PackagePeriod.UNKNOWN
    }
    return SubscriptionPackage(
        identifier = identifier,
        packagePeriod = period,
        product = SubscriptionProduct(
            title = storeProduct.title,
            description = storeProduct.localizedDescription ?: "",
            priceFormatted = storeProduct.price.formatted
        )
    )
}
