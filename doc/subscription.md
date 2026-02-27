# Subscription System

## Architecture

```
SubscriptionViewModel
    └── ISubscriptionManager (RevenueCatSubscriptionManager)
         └── RevenueCat KMP SDK (2.2.10+17.19.1)
              ├── Android: Google Play Billing
              └── iOS: StoreKit
```

## ISubscriptionManager Interface

```kotlin
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
```

## Models

```kotlin
enum class PackagePeriod { MONTHLY, ANNUAL, LIFETIME, UNKNOWN }

data class SubscriptionProduct(
    val title: String,
    val description: String,
    val priceFormatted: String
)

data class SubscriptionPackage(
    val identifier: String,
    val packagePeriod: PackagePeriod,
    val product: SubscriptionProduct
)

data class SubscriptionOffering(
    val availablePackages: List<SubscriptionPackage>
)

data class SubscriptionCustomerInfo(
    val activeEntitlements: Map<String, SubscriptionEntitlement>,
    val managementUrlString: String? = null
)
```

## SubscriptionViewModel States

```kotlin
// Main state
state: StateFlow<UiState<SubscriptionData>>  // Loading, Error, Loaded

// UI-specific state
uiState: StateFlow<SubscriptionUiState> {
    val isPurchasing: Boolean
    val error: String?
    val successMessage: String?
}
```

## Screen States
- **Loading**: SubscriptionLoadingContent
- **Error**: SubscriptionErrorContent (with retry)
- **Subscribed**: SubscriptionActiveContent (manage/cancel, expiry date)
- **Not Subscribed**: SubscriptionNotSubscribedContent (purchase/restore, plan cards)

## Feature Access
Premium features gated via:
```kotlin
GetFeatureAccessUseCase() -> Flow<FeatureAccessResponse>
// FeatureAccessResponse.userAccess.hasPremiumAccess: Boolean
```
Backend endpoint: `GET /users/feature-access`

Currently gates:
- Image import (OCR) tab visibility
- AI features availability

## RevenueCat Initialization

**Android** (`LexiconApplication.kt`):
```kotlin
Purchases.configure(PurchasesConfiguration.Builder(context, revenueCatAndroidKey))
```

**iOS** (`MainViewController.kt`):
```kotlin
Purchases.configure(PurchasesConfiguration.Builder(revenueCatIosKey))
```

API keys from `AppConfig.REVENUECAT_ANDROID_KEY` / `REVENUECAT_IOS_KEY`.

## User Subscription Status
Stored on backend as part of AuthUser:
```kotlin
data class AuthUser(
    val subscriptionStatus: SubscriptionStatus,  // FREE, TRIAL, ACTIVE, EXPIRED, CANCELLED
    val subscriptionExpiresAt: String?            // ISO 8601
)
```

## Error Handling
- `CancelledPurchaseException`: User cancelled purchase dialog (not an error)
- Other errors: displayed in UI with retry option
