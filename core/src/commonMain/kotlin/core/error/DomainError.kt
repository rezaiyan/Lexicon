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
        /** The server refused an AI action: the balance is below its cost. */
        data object InsufficientCredits : Commerce()
        data object PurchaseFailed : Commerce()
        /** User dismissed the store sheet — not an error to surface. */
        data object PurchaseCancelled : Commerce()
        /** Store accepted the purchase but payment is still processing (e.g. slow card / cash). */
        data object PaymentPending : Commerce()
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

    sealed class Validation(message: String) : DomainError(message) {
        data class BlankField(val fieldName: String) : Validation("$fieldName cannot be blank")
    }

    /** Failures of the add-words pipeline (manual, file, photo, AI suggestions). */
    sealed class AddWords(message: String? = null) : DomainError(message) {
        data object EmptyInput : AddWords("Nothing to add")
        data class InvalidDraft(val reason: Reason) : AddWords("Invalid word: $reason") {
            enum class Reason { BlankTerm, BlankTranslation, TooLong }
        }
        data object UnsupportedFile : AddWords("Unsupported file")
        data class FileTooLarge(val maxBytes: Int) : AddWords("File too large")
        data class ImageTooLarge(val maxBytes: Int) : AddWords("Image too large")
        data object ImageUnreadable : AddWords("Image unreadable")
        data object NothingRecognized : AddWords("No vocabulary found")
    }
}
