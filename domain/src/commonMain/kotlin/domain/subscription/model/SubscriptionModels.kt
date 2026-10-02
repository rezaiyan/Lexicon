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
    val product: SubscriptionProduct,
    val trialPeriodDays: Int? = null,
    val hasFreeTrial: Boolean = false
)

data class SubscriptionOffering(
    val availablePackages: List<SubscriptionPackage>
)

data class SubscriptionEntitlement(
    val identifier: String,
    val isActive: Boolean,
    val expirationDateMillis: Long?,
    val productIdentifier: String,
    val willRenew: Boolean = true,
    val isInTrial: Boolean = false
)

data class SubscriptionCustomerInfo(
    val activeEntitlements: Map<String, SubscriptionEntitlement>,
    val managementUrlString: String? = null
) {
    /** Single rule for "this store customer is subscribed" — any active entitlement. */
    val isSubscribed: Boolean get() = activeEntitlements.values.any { it.isActive }

    /** The entitlement the subscription screen describes: the one lasting longest (no expiry = lifetime wins). */
    val primaryEntitlement: SubscriptionEntitlement?
        get() = activeEntitlements.values
            .filter { it.isActive }
            .maxByOrNull { it.expirationDateMillis ?: Long.MAX_VALUE }
}
