package domain.subscription.model

enum class PackagePeriod { MONTHLY, ANNUAL, LIFETIME, UNKNOWN }

data class SubscriptionProduct(
    val title: String,
    val description: String,
    val priceFormatted: String,
    /** Store price in millionths of the currency unit; null when the store didn't report it. */
    val priceAmountMicros: Long? = null,
    /** Store product id, matched against [SubscriptionEntitlement.productIdentifier]. */
    val productIdentifier: String = "",
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

/** Who bills a subscription. Only the two app stores have a management page the user can open. */
enum class SubscriptionStore { APP_STORE, PLAY_STORE, OTHER }

data class SubscriptionEntitlement(
    val identifier: String,
    val isActive: Boolean,
    val expirationDateMillis: Long?,
    val productIdentifier: String,
    val willRenew: Boolean = true,
    val isInTrial: Boolean = false,
    /** Set while a renewal payment is failing (store grace period); the store reports willRenew = false then. */
    val billingIssueDetectedAtMillis: Long? = null,
    /** Set once the user turned auto-renew off. Unlike willRenew = false, a scheduled pause doesn't set it. */
    val unsubscribeDetectedAtMillis: Long? = null,
    val store: SubscriptionStore = SubscriptionStore.OTHER,
)

/**
 * The store customer, shared by every platform the account signs in on: a subscription bought on
 * iPhone is active on Android too, and [managementUrlString] then points at the App Store.
 */
data class SubscriptionCustomerInfo(
    val activeEntitlements: Map<String, SubscriptionEntitlement>,
    val managementUrlString: String? = null,
    /** The store this device buys through; null where there is none (web). */
    val deviceStore: SubscriptionStore? = null,
) {
    /** Single rule for "this store customer is subscribed" — any active entitlement. */
    val isSubscribed: Boolean get() = activeEntitlements.values.any { it.isActive }

    /** The entitlement the subscription screen describes: the one lasting longest (no expiry = lifetime wins). */
    val primaryEntitlement: SubscriptionEntitlement?
        get() = activeEntitlements.values
            .filter { it.isActive }
            .maxByOrNull { it.expirationDateMillis ?: Long.MAX_VALUE }

    /**
     * This device's store bills [primaryEntitlement], so its management page applies here. A
     * purchase from the other platform's store can only be managed on that platform.
     */
    val isManageableHere: Boolean
        get() = deviceStore != null && deviceStore != SubscriptionStore.OTHER &&
            primaryEntitlement?.store == deviceStore
}
