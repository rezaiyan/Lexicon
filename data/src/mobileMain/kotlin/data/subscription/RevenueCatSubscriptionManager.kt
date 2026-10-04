package data.subscription

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.models.CacheFetchPolicy
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.DiscountPaymentMode
import com.revenuecat.purchases.kmp.models.Offerings
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PackageType
import com.revenuecat.purchases.kmp.models.PeriodType
import com.revenuecat.purchases.kmp.models.PeriodUnit
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.PurchasesErrorCode
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction
import com.revenuecat.purchases.kmp.models.freePhase
import core.common.Try
import core.common.getOrNull
import core.common.map
import core.error.DomainError
import domain.subscription.ISubscriptionManager
import domain.subscription.model.PackagePeriod
import domain.subscription.model.SubscriptionCustomerInfo
import domain.subscription.model.SubscriptionEntitlement
import domain.subscription.model.SubscriptionOffering
import domain.subscription.model.SubscriptionPackage
import domain.subscription.model.SubscriptionProduct
import expects.openUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * RevenueCat-backed store. [customerInfo] is kept current from every SDK callback plus the
 * [PurchasesDelegate] push updates (renewals, refunds, purchases from another device).
 */
class RevenueCatSubscriptionManager : ISubscriptionManager, PurchasesDelegate {

    private val _customerInfo = MutableStateFlow<SubscriptionCustomerInfo?>(null)
    override val customerInfo: StateFlow<SubscriptionCustomerInfo?> = _customerInfo.asStateFlow()

    // Raw RC packages from the last getOfferings(), needed to start a purchase.
    private var cachedPackages: Map<String, Package> = emptyMap()

    init {
        Purchases.sharedInstance.delegate = this
        Purchases.sharedInstance.getCustomerInfo(
            onError = { },
            onSuccess = ::publish
        )
    }

    private fun publish(info: CustomerInfo): SubscriptionCustomerInfo =
        info.toDomain().also { _customerInfo.value = it }

    private suspend fun getRawCustomerInfo(
        fetchPolicy: CacheFetchPolicy = CacheFetchPolicy.default(),
    ): Try<CustomerInfo> =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.getCustomerInfo(
                fetchPolicy = fetchPolicy,
                onError = { error ->
                    continuation.resume(Try.failure(error.toDomainError(DomainError.Commerce.ManagementUnavailable)))
                },
                onSuccess = { info ->
                    publish(info)
                    continuation.resume(Try.success(info))
                }
            )
        }

    override suspend fun getOfferings(): Try<SubscriptionOffering> =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.getOfferings(
                onError = { error ->
                    continuation.resume(Try.failure(error.toDomainError(SubscriptionLoadFailedException())))
                },
                onSuccess = { offerings ->
                    cachedPackages = offerings.current?.availablePackages
                        ?.associateBy { it.identifier }
                        .orEmpty()
                    continuation.resume(Try.success(offerings.toDomain()))
                }
            )
        }

    override suspend fun purchase(packageToPurchase: SubscriptionPackage): Try<SubscriptionCustomerInfo> {
        val rcPackage = cachedPackages[packageToPurchase.identifier]
            ?: return Try.failure(DomainError.Commerce.PurchaseFailed)

        return suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.purchase(
                packageToPurchase = rcPackage,
                onError = { error: PurchasesError, userCancelled: Boolean ->
                    val domainError = if (userCancelled) {
                        DomainError.Commerce.PurchaseCancelled
                    } else {
                        error.toDomainError(DomainError.Commerce.PurchaseFailed)
                    }
                    continuation.resume(Try.failure(domainError))
                },
                onSuccess = { _: StoreTransaction, info: CustomerInfo ->
                    continuation.resume(Try.success(publish(info)))
                }
            )
        }
    }

    override suspend fun restore(): Try<SubscriptionCustomerInfo> =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.restorePurchases(
                onError = { error ->
                    continuation.resume(Try.failure(error.toDomainError(DomainError.Commerce.RestoreFailed)))
                },
                onSuccess = { info ->
                    continuation.resume(Try.success(publish(info)))
                }
            )
        }

    override fun isSubscribed(): Flow<Boolean> =
        customerInfo.map { it?.isSubscribed == true }.distinctUntilChanged()

    override suspend fun logIn(userId: String): Try<SubscriptionCustomerInfo> =
        suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.logIn(
                newAppUserID = userId,
                onError = { error ->
                    continuation.resume(Try.failure(error.toDomainError(Exception(error.message))))
                },
                onSuccess = { info, _ ->
                    continuation.resume(Try.success(publish(info)))
                }
            )
        }

    override suspend fun logOut(): Try<SubscriptionCustomerInfo> {
        // RC rejects logOut for anonymous users; nothing to undo then, but still drop local state.
        if (Purchases.sharedInstance.isAnonymous) {
            _customerInfo.value = null
            return Try.success(SubscriptionCustomerInfo(activeEntitlements = emptyMap()))
        }
        return suspendCancellableCoroutine { continuation ->
            Purchases.sharedInstance.logOut(
                onError = { error ->
                    _customerInfo.value = null
                    continuation.resume(Try.failure(error.toDomainError(Exception(error.message))))
                },
                onSuccess = { info ->
                    continuation.resume(Try.success(publish(info)))
                }
            )
        }
    }

    override fun getCurrentCustomerInfo(): SubscriptionCustomerInfo? = _customerInfo.value

    override suspend fun refreshCustomerInfo(): Try<SubscriptionCustomerInfo> =
        getRawCustomerInfo(CacheFetchPolicy.FETCH_CURRENT).map { it.toDomain() }

    override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
        publish(customerInfo)
    }

    /** App Store promoted purchase (iOS): let it proceed and pick up the resulting entitlement. */
    override fun onPurchasePromoProduct(
        product: StoreProduct,
        startPurchase: (
            onError: (error: PurchasesError, userCancelled: Boolean) -> Unit,
            onSuccess: (storeTransaction: StoreTransaction, customerInfo: CustomerInfo) -> Unit
        ) -> Unit
    ) {
        startPurchase({ _, _ -> }, { _, info -> publish(info) })
    }

    override suspend fun manageSubscription(): Try<Unit> {
        val managementUrl = getRawCustomerInfo().getOrNull()?.managementUrlString
        if (managementUrl.isNullOrBlank()) {
            return Try.failure(DomainError.Commerce.ManagementUnavailable)
        }
        return Try.success(openUrl(managementUrl))
    }
}

/** Message key the subscription screen localizes. */
private class SubscriptionLoadFailedException : Exception("SUBSCRIPTION_LOAD_FAILED")

private fun PurchasesError.toDomainError(fallback: Throwable): Throwable = when (code) {
    PurchasesErrorCode.NetworkError,
    PurchasesErrorCode.OfflineConnectionError -> DomainError.Network.NoConnection
    PurchasesErrorCode.PaymentPendingError -> DomainError.Commerce.PaymentPending
    PurchasesErrorCode.PurchaseCancelledError -> DomainError.Commerce.PurchaseCancelled
    else -> fallback
}

// Mapper extensions: RevenueCat types -> Domain types

private fun CustomerInfo.toDomain(): SubscriptionCustomerInfo {
    val activeEntitlements = entitlements.active.mapValues { (_, entitlement) ->
        SubscriptionEntitlement(
            identifier = entitlement.identifier,
            isActive = entitlement.isActive,
            expirationDateMillis = entitlement.expirationDate?.toEpochMilliseconds(),
            productIdentifier = entitlement.productIdentifier,
            willRenew = entitlement.willRenew,
            isInTrial = entitlement.periodType == PeriodType.TRIAL,
            billingIssueDetectedAtMillis = entitlement.billingIssueDetectedAtMillis,
            unsubscribeDetectedAtMillis = entitlement.unsubscribeDetectedAtMillis,
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

    // iOS: introductoryDiscount with FREE_TRIAL payment mode
    val iosTrialPeriod = storeProduct.introductoryDiscount
        ?.takeIf { it.paymentMode == DiscountPaymentMode.FREE_TRIAL }
        ?.subscriptionPeriod

    // Android: subscriptionOptions.freeTrial.freePhase billing period
    val androidTrialPeriod = storeProduct.subscriptionOptions
        ?.freeTrial
        ?.freePhase
        ?.billingPeriod

    val trialPeriod = iosTrialPeriod ?: androidTrialPeriod
    val trialDays = trialPeriod?.toDays()?.takeIf { it > 0 }

    return SubscriptionPackage(
        identifier = identifier,
        packagePeriod = period,
        product = SubscriptionProduct(
            title = storeProduct.title,
            description = storeProduct.localizedDescription ?: "",
            priceFormatted = storeProduct.price.formatted,
            priceAmountMicros = storeProduct.price.amountMicros,
            productIdentifier = storeProduct.id,
        ),
        trialPeriodDays = trialDays,
        hasFreeTrial = trialDays != null
    )
}

private fun com.revenuecat.purchases.kmp.models.Period.toDays(): Int = when (unit) {
    PeriodUnit.DAY -> value
    PeriodUnit.WEEK -> value * 7
    PeriodUnit.MONTH -> value * 30
    PeriodUnit.YEAR -> value * 365
    PeriodUnit.UNKNOWN -> 0
}
