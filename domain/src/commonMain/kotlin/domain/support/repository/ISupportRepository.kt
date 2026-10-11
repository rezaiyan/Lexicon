package domain.support.repository

import core.common.Try
import domain.support.model.SupportMessage
import domain.support.model.SupportUnread
import kotlinx.coroutines.flow.Flow

/**
 * The user's support conversation. Reads come from the local store; writes land locally first,
 * then sync. A message that fails to sync stays visible as failed until retried.
 */
interface ISupportRepository {
    /** Conversation in display order (oldest first, unsent messages last). */
    fun observeMessages(): Flow<List<SupportMessage>>

    /** Pulls new messages from the backend and retries unsent ones. */
    suspend fun refresh(): Try<Unit>

    /** Stores [body] locally and sends it. Succeeds once stored, even if sending fails. */
    suspend fun send(body: String): Try<Unit>

    /** Re-sends the unsent message [messageId]. */
    suspend fun retry(messageId: String): Try<Unit>

    /** True once the user has a conversation stored locally (sent or received anything). */
    suspend fun hasConversation(): Try<Boolean>

    /** Unread state of support replies, re-emitted when messages or read markers change. */
    fun observeUnread(): Flow<SupportUnread>

    /** Marks every reply stored so far as read (also clears the Settings dot). */
    suspend fun markRead(): Try<Unit>

    /** Marks every reply stored so far as noticed from Settings; the chat stays unread. */
    suspend fun markSeenInSettings(): Try<Unit>
}
