package data.listening

import core.common.Try
import data.listening.local.IListeningPendingQueue
import data.listening.local.PendingListeningSession
import data.listening.remote.IListeningDataSource
import data.listening.remote.SyncListeningSessionRequest
import domain.listening.model.HeardWord
import domain.listening.model.ListeningOrder
import domain.listening.model.ListeningSelection
import domain.listening.model.ListeningSessionRecord
import domain.listening.model.ListeningSource
import domain.word.model.LearningStage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListeningRecorderImplTest {

    private class FakeQueue : IListeningPendingQueue {
        val rows = linkedMapOf<String, String>()
        override suspend fun insert(session: PendingListeningSession, createdAt: Long) {
            rows.getOrPut(session.clientSessionId) { session.requestJson }
        }
        override suspend fun getAll() = rows.map { PendingListeningSession(it.key, it.value) }
        override suspend fun delete(clientSessionIds: Collection<String>) {
            clientSessionIds.forEach(rows::remove)
        }
    }

    private class FakeDataSource : IListeningDataSource {
        var fail = false
        val batches = mutableListOf<List<SyncListeningSessionRequest>>()
        override suspend fun syncSessions(sessions: List<SyncListeningSessionRequest>): Try<Unit> {
            batches += sessions
            return if (fail) Try.failure(RuntimeException("offline")) else Try.success(Unit)
        }
    }

    private val queue = FakeQueue()
    private val dataSource = FakeDataSource()
    private val recorder = ListeningRecorderImpl(dataSource, queue)

    private fun record(id: String, source: ListeningSource = ListeningSource.Due) = ListeningSessionRecord(
        clientSessionId = id,
        selection = ListeningSelection(source = source),
        order = ListeningOrder.TRANSLATION_FIRST,
        repeatCount = 2,
        pauseMs = 3_000,
        speechRate = 1.2f,
        plannedWords = 5,
        heardWords = listOf(HeardWord(4, "en", "de", 1_500)),
        wordsSkipped = 1,
        pauseCount = 2,
        listeningMs = 9_000,
        durationMs = 10_000,
        completedNormally = true,
        startedAt = 1_000,
        endedAt = 11_000,
    )

    @Test
    fun `recordSession syncs the session and empties the queue`() = runTest {
        val result = recorder.recordSession(record("s-1"))

        assertTrue(result is Try.Success)
        assertEquals(listOf("s-1"), dataSource.batches.single().map { it.clientSessionId })
        assertTrue(queue.rows.isEmpty())
    }

    @Test
    fun `failed sync keeps the session queued and still succeeds`() = runTest {
        dataSource.fail = true

        val result = recorder.recordSession(record("s-1"))

        assertTrue(result is Try.Success)
        assertEquals(setOf("s-1"), queue.rows.keys)
    }

    @Test
    fun `next session also sends sessions left from a failed sync`() = runTest {
        dataSource.fail = true
        recorder.recordSession(record("s-1"))
        dataSource.fail = false

        recorder.recordSession(record("s-2"))

        assertEquals(listOf("s-1", "s-2"), dataSource.batches.last().map { it.clientSessionId })
        assertTrue(queue.rows.isEmpty())
    }

    @Test
    fun `unreadable queued rows are dropped`() = runTest {
        queue.rows["broken"] = "{not json"

        recorder.recordSession(record("s-1"))

        assertEquals(listOf("s-1"), dataSource.batches.single().map { it.clientSessionId })
        assertTrue(queue.rows.isEmpty())
    }

    @Test
    fun `more than 100 queued sessions sync in batches of 100`() = runTest {
        dataSource.fail = true
        repeat(150) { recorder.recordSession(record("s-$it")) }
        dataSource.fail = false
        dataSource.batches.clear()

        recorder.recordSession(record("last"))

        assertEquals(listOf(100, 51), dataSource.batches.map { it.size })
        assertTrue(queue.rows.isEmpty())
    }

    @Test
    fun `sync request maps every field of the record`() {
        val request = record("s-1", ListeningSource.Level(LearningStage.LEVEL_2_FAMILIAR)).toSyncRequest()

        assertEquals("level", request.source)
        assertEquals("LEVEL_2_FAMILIAR", request.sourceDetail)
        assertEquals("translation_first", request.order)
        assertEquals(2, request.repeatCount)
        assertEquals(3_000, request.pauseMs)
        assertEquals(1.2f, request.speechRate)
        assertEquals(5, request.plannedWords)
        assertEquals(1, request.wordsHeard)
        assertEquals(1, request.wordsSkipped)
        assertEquals(2, request.pauseCount)
        assertEquals(9_000, request.listeningMs)
        assertEquals(10_000, request.durationMs)
        assertEquals(4L, request.words.single().wordId)
        assertEquals("de", request.words.single().targetLanguage)
    }

    @Test
    fun `tag and plain sources map their detail`() {
        assertEquals("42", record("a", ListeningSource.Tag(42)).toSyncRequest().sourceDetail)
        assertNull(record("b", ListeningSource.All).toSyncRequest().sourceDetail)
        assertEquals("all", record("b", ListeningSource.All).toSyncRequest().source)
    }
}
