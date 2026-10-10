package data.listening

import core.common.Try
import data.listening.local.IListeningPendingQueue
import data.listening.local.PendingListeningSession
import data.listening.remote.IListeningDataSource
import data.listening.remote.SyncListeningSessionRequest
import data.listening.remote.SyncListeningWordRequest
import domain.listening.model.ListeningSessionRecord
import domain.listening.model.ListeningSource
import domain.listening.repository.IListeningRecorder
import expects.logNetwork
import kotlinx.serialization.json.Json

/**
 * Queues listening sessions in SQLDelight, then syncs every queued one to the backend.
 * A failed sync keeps the queue for the next session, so nothing is lost offline.
 */
class ListeningRecorderImpl(
    private val dataSource: IListeningDataSource,
    private val queue: IListeningPendingQueue,
) : IListeningRecorder {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun recordSession(session: ListeningSessionRecord): Try<Unit> {
        queue.insert(
            PendingListeningSession(session.clientSessionId, json.encodeToString(session.toSyncRequest())),
            createdAt = session.endedAt,
        )
        syncPending()
        // The session is safely queued either way; a failed sync is retried after the next session.
        return Try.success(Unit)
    }

    private suspend fun syncPending() {
        val pending = queue.getAll().map { it.clientSessionId to decode(it.requestJson) }
        // Unreadable rows can never sync; drop them instead of blocking the queue forever.
        val corrupt = pending.filter { it.second == null }.map { it.first }
        if (corrupt.isNotEmpty()) queue.delete(corrupt)

        val readable = pending.mapNotNull { (_, request) -> request }
        readable.chunked(MAX_SESSIONS_PER_SYNC).forEach { batch ->
            when (val result = dataSource.syncSessions(batch)) {
                is Try.Success -> queue.delete(batch.map { it.clientSessionId })
                is Try.Failure -> {
                    logNetwork("ListeningRecorder", "Sync failed: ${result.throwable.message}. Will retry later.")
                    return
                }
            }
        }
    }

    private fun decode(requestJson: String): SyncListeningSessionRequest? =
        runCatching { json.decodeFromString<SyncListeningSessionRequest>(requestJson) }.getOrNull()

    private companion object {
        /** Backend limit per sync request. */
        const val MAX_SESSIONS_PER_SYNC = 100
    }
}

fun ListeningSessionRecord.toSyncRequest() = SyncListeningSessionRequest(
    clientSessionId = clientSessionId,
    source = when (selection.source) {
        ListeningSource.Due -> "due"
        ListeningSource.All -> "all"
        is ListeningSource.Level -> "level"
        is ListeningSource.Tag -> "tag"
    },
    sourceDetail = when (val source = selection.source) {
        is ListeningSource.Level -> source.stage.name
        is ListeningSource.Tag -> source.tagId.toString()
        ListeningSource.Due, ListeningSource.All -> null
    },
    order = order.name.lowercase(),
    repeatCount = repeatCount,
    pauseMs = pauseMs,
    speechRate = speechRate,
    plannedWords = plannedWords,
    wordsHeard = heardWords.size,
    wordsSkipped = wordsSkipped,
    pauseCount = pauseCount,
    listeningMs = listeningMs,
    durationMs = durationMs,
    completedNormally = completedNormally,
    startedAt = startedAt,
    endedAt = endedAt,
    words = heardWords.map {
        SyncListeningWordRequest(
            wordId = it.wordId.toLong(),
            sourceLanguage = it.sourceLanguage,
            targetLanguage = it.targetLanguage,
            heardAt = it.heardAt,
        )
    },
)
