package domain.listening.repository

import core.common.Try
import domain.listening.model.ListeningSessionRecord

interface IListeningRecorder {
    /** Queues [session] locally, then syncs every queued session. Sync failures keep the queue. */
    suspend fun recordSession(session: ListeningSessionRecord): Try<Unit>
}
