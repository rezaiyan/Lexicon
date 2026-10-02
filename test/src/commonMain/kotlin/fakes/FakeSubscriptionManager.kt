package fakes

import core.common.Try
import core.common.getOrNull
import domain.subscription.ISubscriptionManager
import domain.subscription.model.SubscriptionCustomerInfo
import domain.subscription.model.SubscriptionEntitlement
import domain.subscription.model.SubscriptionOffering
import domain.subscription.model.SubscriptionPackage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class FakeSubscriptionManager(
    initialCustomerInfo: SubscriptionCustomerInfo? = null,
) : ISubscriptionManager {

    val customerInfoFlow = MutableStateFlow(initialCustomerInfo)
    override val customerInfo: StateFlow<SubscriptionCustomerInfo?> = customerInfoFlow

    var offeringsResult: Try<SubscriptionOffering> = Try.success(SubscriptionOffering(emptyList()))
    var purchaseResult: Try<SubscriptionCustomerInfo>? = null
    var restoreResult: Try<SubscriptionCustomerInfo> = Try.success(SubscriptionCustomerInfo(emptyMap()))
    var logInResult: Try<SubscriptionCustomerInfo> = Try.success(SubscriptionCustomerInfo(emptyMap()))
    var manageResult: Try<Unit> = Try.success(Unit)

    var lastLoggedInUserId: String? = null
        private set
    var logOutCount = 0
        private set

    fun setSubscribed(subscribed: Boolean) {
        customerInfoFlow.value = if (subscribed) subscribedCustomerInfo() else SubscriptionCustomerInfo(emptyMap())
    }

    override suspend fun getOfferings(): Try<SubscriptionOffering> = offeringsResult

    override suspend fun purchase(packageToPurchase: SubscriptionPackage): Try<SubscriptionCustomerInfo> {
        val result = purchaseResult ?: Try.success(subscribedCustomerInfo())
        result.getOrNull()?.let { customerInfoFlow.value = it }
        return result
    }

    override suspend fun restore(): Try<SubscriptionCustomerInfo> {
        restoreResult.getOrNull()?.let { customerInfoFlow.value = it }
        return restoreResult
    }

    override fun isSubscribed(): Flow<Boolean> = customerInfoFlow.map { it?.isSubscribed == true }

    override suspend fun logIn(userId: String): Try<SubscriptionCustomerInfo> {
        lastLoggedInUserId = userId
        return logInResult
    }

    override suspend fun logOut(): Try<SubscriptionCustomerInfo> {
        logOutCount++
        customerInfoFlow.value = null
        return Try.success(SubscriptionCustomerInfo(emptyMap()))
    }

    override fun getCurrentCustomerInfo(): SubscriptionCustomerInfo? = customerInfoFlow.value

    override suspend fun manageSubscription(): Try<Unit> = manageResult

    override suspend fun cancelSubscription(): Try<Unit> = manageResult

    companion object {
        fun subscribedCustomerInfo(
            expirationDateMillis: Long? = 1_900_000_000_000L,
            willRenew: Boolean = true,
            isInTrial: Boolean = false,
        ) = SubscriptionCustomerInfo(
            activeEntitlements = mapOf(
                "premium" to SubscriptionEntitlement(
                    identifier = "premium",
                    isActive = true,
                    expirationDateMillis = expirationDateMillis,
                    productIdentifier = "premium_monthly",
                    willRenew = willRenew,
                    isInTrial = isInTrial,
                )
            )
        )
    }
}

