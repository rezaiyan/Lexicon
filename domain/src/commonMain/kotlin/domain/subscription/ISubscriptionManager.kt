package domain.subscription

import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Offerings
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.StoreProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ISubscriptionManager {
    val customerInfo: StateFlow<CustomerInfo?>
    
    suspend fun getOfferings(): Result<Offerings>
    
    suspend fun purchase(packageToPurchase: Package): Result<CustomerInfo>
    
    suspend fun purchase(product: StoreProduct): Result<CustomerInfo>
    
    suspend fun restore(): Result<CustomerInfo>
    
    fun isSubscribed(): Flow<Boolean>
    
    suspend fun logIn(userId: String): Result<CustomerInfo>
    
    suspend fun logOut(): Result<CustomerInfo>
    
    fun getCurrentCustomerInfo(): CustomerInfo?
    
    suspend fun manageSubscription(): Result<Unit>
    
    suspend fun cancelSubscription(): Result<Unit>
}


