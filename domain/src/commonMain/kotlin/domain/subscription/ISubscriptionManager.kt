package domain.subscription

import core.common.Try
import domain.subscription.model.SubscriptionCustomerInfo
import domain.subscription.model.SubscriptionOffering
import domain.subscription.model.SubscriptionPackage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ISubscriptionManager {
    val customerInfo: StateFlow<SubscriptionCustomerInfo?>

    suspend fun getOfferings(): Try<SubscriptionOffering>

    suspend fun purchase(packageToPurchase: SubscriptionPackage): Try<SubscriptionCustomerInfo>

    suspend fun restore(): Try<SubscriptionCustomerInfo>

    fun isSubscribed(): Flow<Boolean>

    suspend fun logIn(userId: String): Try<SubscriptionCustomerInfo>

    suspend fun logOut(): Try<SubscriptionCustomerInfo>

    fun getCurrentCustomerInfo(): SubscriptionCustomerInfo?

    suspend fun manageSubscription(): Try<Unit>

    suspend fun cancelSubscription(): Try<Unit>
}
