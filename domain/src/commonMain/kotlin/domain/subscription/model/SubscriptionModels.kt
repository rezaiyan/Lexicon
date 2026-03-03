package domain.subscription.model

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

data class SubscriptionEntitlement(
    val identifier: String,
    val isActive: Boolean,
    val expirationDateMillis: Long?,
    val productIdentifier: String,
    val willRenew: Boolean = true
)

data class SubscriptionCustomerInfo(
    val activeEntitlements: Map<String, SubscriptionEntitlement>,
    val managementUrlString: String? = null
)
