package domain.subscription

import domain.subscription.model.SubscriptionCustomerInfo
import domain.subscription.model.SubscriptionOffering
import domain.subscription.model.SubscriptionPackage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ISubscriptionManager {
    val customerInfo: StateFlow<SubscriptionCustomerInfo?>

    suspend fun getOfferings(): Result<SubscriptionOffering>

    suspend fun purchase(packageToPurchase: SubscriptionPackage): Result<SubscriptionCustomerInfo>

    suspend fun restore(): Result<SubscriptionCustomerInfo>

    fun isSubscribed(): Flow<Boolean>

    suspend fun logIn(userId: String): Result<SubscriptionCustomerInfo>

    suspend fun logOut(): Result<SubscriptionCustomerInfo>

    fun getCurrentCustomerInfo(): SubscriptionCustomerInfo?

    suspend fun manageSubscription(): Result<Unit>

    suspend fun cancelSubscription(): Result<Unit>
}
