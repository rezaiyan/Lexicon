package core.error

/**
 * Typed domain error hierarchy. Thrown by repository implementations in :data,
 * propagated through Try<T> to ViewModels.
 *
 * Usage in repositories:
 *   if (words.isEmpty()) throw DomainError.Learning.NoDueCards
 *
 * Usage in ViewModels:
 *   onFailure = { error ->
 *       when (error) {
 *           is DomainError.Commerce.PremiumRequired -> copy(showPaywall = true)
 *           else -> copy(errorMessage = error.toUserMessage())
 *       }
 *   }
 */
sealed class DomainError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {

    sealed class Network(message: String? = null) : DomainError(message) {
        data object NoConnection : Network("No internet connection")
        data object Timeout : Network("Connection timed out")
        data class ServerError(val code: Int, val body: String? = null) : Network("Server error: $code")
    }

    sealed class Auth(message: String? = null) : DomainError(message) {
        data object NotAuthenticated : Auth()
        data object SessionExpired : Auth()
        data object Unauthorized : Auth()
    }

    sealed class Commerce : DomainError() {
        data object PremiumRequired : Commerce()
        data object PurchaseFailed : Commerce()
        data object RestoreFailed : Commerce()
        data object ManagementUnavailable : Commerce()
    }

    sealed class Learning : DomainError() {
        data object NoDueCards : Learning()
        data object SessionNotActive : Learning()
    }

    sealed class Data : DomainError() {
        data class NotFound(val type: String, val id: String) : Data()
        data class DuplicateEntry(val identifier: String) : Data()
    }
}
