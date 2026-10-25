package core.error

/**
 * Maps a [Throwable] to a user-facing message string.
 *
 * Typed [DomainError] subtypes produce specific messages.
 * Unknown errors fall back to the raw message or a generic fallback.
 *
 * Use this in ViewModels instead of `error.message ?: "fallback"`.
 * Keep using `error.message` for analytics/logging calls where raw info is needed.
 */
@Suppress("CyclomaticComplexMethod") // exhaustive mapping of all DomainError subtypes — complexity is inherent
fun Throwable.toUserMessage(): String = when (this) {
    is DomainError.Network.NoConnection -> "No internet connection"
    is DomainError.Network.Timeout -> "Request timed out. Try again."
    is DomainError.Network.ServerError -> "Server error. Please try again later."
    is DomainError.Network.RateLimited -> "Too many requests. Please try again in a few minutes."
    is DomainError.Auth.NotAuthenticated -> "Please sign in to continue."
    is DomainError.Auth.SessionExpired -> "Your session expired. Please sign in again."
    is DomainError.Auth.Unauthorized -> "You don't have permission to do that."
    is DomainError.Passkey.Cancelled -> "Passkey cancelled."
    is DomainError.Passkey.NoPasskeys ->
        "No Lexicon passkey on this device. Sign in with Google or Apple, then create one in Settings."
    is DomainError.Passkey.AlreadyRegistered -> "This device already has a passkey for your account."
    is DomainError.Passkey.Unavailable -> "Passkeys aren't available on this device."
    is DomainError.Passkey.Rejected ->
        "This passkey didn't work. If you deleted it, sign in with Google or Apple and create a new one."
    is DomainError.Passkey.Removed ->
        "This passkey was removed from your account. Sign in with Google or Apple."
    is DomainError.Passkey.Failed -> "Passkey didn't work. Please try again."
    is DomainError.Commerce.PremiumRequired -> "This feature requires a subscription."
    is DomainError.Commerce.InsufficientCredits -> "You don't have enough credits for this."
    is DomainError.Commerce.PurchaseFailed -> "Purchase failed. Please try again."
    is DomainError.Commerce.PurchaseCancelled -> "Purchase cancelled."
    is DomainError.Commerce.PaymentPending -> "Payment is pending. Premium unlocks as soon as the store confirms it."
    is DomainError.Commerce.RestoreFailed -> "Failed to restore purchases. Please try again."
    is DomainError.Commerce.ManagementUnavailable -> "Subscription management is unavailable."
    is DomainError.Learning.NoDueCards -> "No cards due for review."
    is DomainError.Learning.SessionNotActive -> "No active study session."
    else -> message ?: "Something went wrong. Please try again."
}
