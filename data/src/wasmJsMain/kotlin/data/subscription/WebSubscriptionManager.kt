package data.subscription

import core.common.Try
import domain.subscription.ISubscriptionManager
import domain.subscription.model.SubscriptionCustomerInfo
import domain.subscription.model.SubscriptionOffering
import domain.subscription.model.SubscriptionPackage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Web implementation of ISubscriptionManager.
 * Currently a stub — subscriptions are not yet supported on web.
 * TODO: Integrate RevenueCat JS SDK (@revenuecat/purchases-js) when web billing is enabled.
 */
class WebSubscriptionManager : ISubscriptionManager {

    private val _customerInfo = MutableStateFlow<SubscriptionCustomerInfo?>(null)
    override val customerInfo: StateFlow<SubscriptionCustomerInfo?> = _customerInfo.asStateFlow()

    override suspend fun getOfferings(): Try<SubscriptionOffering> {
        return Try.success(SubscriptionOffering(availablePackages = emptyList()))
    }

    override suspend fun purchase(packageToPurchase: SubscriptionPackage): Try<SubscriptionCustomerInfo> {
        return Try.failure(UnsupportedOperationException("Subscriptions are not yet supported on web"))
    }

    override suspend fun restore(): Try<SubscriptionCustomerInfo> {
        return Try.failure(UnsupportedOperationException("Subscriptions are not yet supported on web"))
    }

    override fun isSubscribed(): Flow<Boolean> = customerInfo.map { false }

    override suspend fun logIn(userId: String): Try<SubscriptionCustomerInfo> {
        return Try.success(SubscriptionCustomerInfo(activeEntitlements = emptyMap()))
    }

    override suspend fun logOut(): Try<SubscriptionCustomerInfo> {
        _customerInfo.value = null
        return Try.success(SubscriptionCustomerInfo(activeEntitlements = emptyMap()))
    }

    override fun getCurrentCustomerInfo(): SubscriptionCustomerInfo? = _customerInfo.value

    override suspend fun manageSubscription(): Try<Unit> {
        return Try.failure(UnsupportedOperationException("Subscriptions are not yet supported on web"))
    }

    override suspend fun cancelSubscription(): Try<Unit> {
        return Try.failure(UnsupportedOperationException("Subscriptions are not yet supported on web"))
    }
}
