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
    is DomainError.Auth.NotAuthenticated -> "Please sign in to continue."
    is DomainError.Auth.SessionExpired -> "Your session expired. Please sign in again."
    is DomainError.Auth.Unauthorized -> "You don't have permission to do that."
    is DomainError.Commerce.PremiumRequired -> "This feature requires a subscription."
    is DomainError.Commerce.PurchaseFailed -> "Purchase failed. Please try again."
    is DomainError.Commerce.RestoreFailed -> "Failed to restore purchases. Please try again."
    is DomainError.Commerce.ManagementUnavailable -> "Subscription management is unavailable."
    is DomainError.Learning.NoDueCards -> "No cards due for review."
    is DomainError.Learning.SessionNotActive -> "No active study session."
    else -> message ?: "Something went wrong. Please try again."
}
