package domain.support.model

enum class SupportSender { USER, ADMIN }

/** Delivery state of a message the user wrote; admin replies are always [SENT]. */
enum class SupportDeliveryStatus { SENDING, SENT, FAILED }

/**
 * One message in the user's support conversation.
 *
 * @param id Stable local id: the client message id for messages written here, or a server-derived id.
 * @param createdAtMillis Epoch millis.
 */
data class SupportMessage(
    val id: String,
    val sender: SupportSender,
    val body: String,
    val createdAtMillis: Long,
    val status: SupportDeliveryStatus,
)

/** Longest message the backend accepts. */
const val MAX_SUPPORT_MESSAGE_LENGTH = 4000
