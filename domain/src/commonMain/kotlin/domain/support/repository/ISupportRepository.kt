package domain.support.repository

import core.common.Try
import domain.support.model.SupportMessage
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
}
