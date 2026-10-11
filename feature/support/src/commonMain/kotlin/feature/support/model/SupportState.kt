package feature.support.model

import domain.support.model.MAX_SUPPORT_MESSAGE_LENGTH
import domain.support.model.SupportMessage

data class SupportState(
    val messages: List<SupportMessage> = emptyList(),
    /** [messages] as list rows: day separators plus grouped bubbles, oldest first. */
    val timeline: List<SupportTimelineItem> = emptyList(),
    val draft: String = "",
    /** True until the local conversation has loaded once. */
    val isLoading: Boolean = true,
    /** The last refresh could not reach the backend; saved messages are still shown. */
    val isOffline: Boolean = false,
) {
    val canSend: Boolean get() = draft.isNotBlank()

    /** Characters left before the backend limit, or null while the draft is far from it. */
    val remainingChars: Int?
        get() = (MAX_SUPPORT_MESSAGE_LENGTH - draft.length).takeIf { it <= DRAFT_LIMIT_WARNING }

    private companion object {
        const val DRAFT_LIMIT_WARNING = 200
    }
}
