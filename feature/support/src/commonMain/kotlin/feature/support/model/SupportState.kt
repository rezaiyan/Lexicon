package feature.support.model

import domain.support.model.SupportMessage

data class SupportState(
    val messages: List<SupportMessage> = emptyList(),
    val draft: String = "",
    /** True until the local conversation has loaded once. */
    val isLoading: Boolean = true,
    /** The last refresh could not reach the backend; saved messages are still shown. */
    val isOffline: Boolean = false,
) {
    val canSend: Boolean get() = draft.isNotBlank()
}
