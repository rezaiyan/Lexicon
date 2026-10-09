package data.word.repository

import core.common.Try
import core.common.getOrThrow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
class WordRepositoryImplTest {

    // -------------------------------------------------------------------------
    // updateWord — syncs to remote then updates local
    // -------------------------------------------------------------------------

    @Test
    fun `updateWord syncs to remote then updates locally`() = runTest {
        val local = FakeWordLocalDataSource()
        val remote = FakeWordRemoteSyncHandler()
        val repo = makeRepository(local = local, remote = remote)

        val word = makeWord(id = 5)
        val result = repo.updateWord(word)

        assertTrue(result.isSuccess)
        assertEquals(1, remote.syncWordUpdateCallCount)
        assertEquals(5L, remote.lastSyncedUpdateId)
        assertEquals(1, local.updatedWords.size)
        assertEquals(word, local.updatedWords.first())
    }

    @Test
    fun `updateWord passes correct word id to remote sync`() = runTest {
        val local = FakeWordLocalDataSource()
        val remote = FakeWordRemoteSyncHandler()
        val repo = makeRepository(local = local, remote = remote)

        val word = makeWord(id = 42)
        repo.updateWord(word)

        assertEquals(42L, remote.lastSyncedUpdateId)
    }

    @Test
    fun `updateWord with remote failure returns failure but local update still applied`() = runTest {
        val local = FakeWordLocalDataSource()
        val remote = FakeWordRemoteSyncHandler().apply { shouldFailSyncWordUpdate = true }
        val repo = makeRepository(local = local, remote = remote)

        val word = makeWord(id = 7)
        val result = repo.updateWord(word)

        assertTrue(result.isFailure)
        assertTrue(local.updatedWords.isNotEmpty())
    }

    // -------------------------------------------------------------------------
    // deleteWord — remote first, local on success
    // -------------------------------------------------------------------------

    @Test
    fun `deleteWord syncs to remote then deletes locally`() = runTest {
        val local = FakeWordLocalDataSource()
        val remote = FakeWordRemoteSyncHandler()
        val repo = makeRepository(local = local, remote = remote)

        val result = repo.deleteWord(10)

        assertTrue(result.isSuccess)
        assertEquals(1, remote.syncWordDeletionCallCount)
        assertEquals(10L, remote.lastSyncedDeletionId)
        assertTrue(local.deletedIds.contains(10))
    }

    @Test
    fun `deleteWord passes correct id to remote sync`() = runTest {
        val local = FakeWordLocalDataSource()
        val remote = FakeWordRemoteSyncHandler()
        val repo = makeRepository(local = local, remote = remote)

        repo.deleteWord(99)

        assertEquals(99L, remote.lastSyncedDeletionId)
    }

    @Test
    fun `deleteWord with remote failure returns failure and does not delete locally`() = runTest {
        val local = FakeWordLocalDataSource()
        val remote = FakeWordRemoteSyncHandler().apply { shouldFailSyncWordDeletion = true }
        val repo = makeRepository(local = local, remote = remote)

        val result = repo.deleteWord(15)

        assertTrue(result.isFailure)
        assertTrue(local.deletedIds.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Remote sync failure — graceful error encapsulation
    // -------------------------------------------------------------------------

    @Test
    fun `updateWord remote failure result is Try Failure with no unhandled exception`() = runTest {
        val local = FakeWordLocalDataSource()
        val remote = FakeWordRemoteSyncHandler().apply { shouldFailSyncWordUpdate = true }
        val repo = makeRepository(local = local, remote = remote)

        val result = repo.updateWord(makeWord(id = 3))

        assertTrue(result.isFailure)
        val exception = (result as Try.Failure).throwable
        assertEquals("Remote update sync failed", exception.message)
    }

    @Test
    fun `deleteWord remote failure result is Try Failure with no unhandled exception`() = runTest {
        val local = FakeWordLocalDataSource()
        val remote = FakeWordRemoteSyncHandler().apply { shouldFailSyncWordDeletion = true }
        val repo = makeRepository(local = local, remote = remote)

        val result = repo.deleteWord(8)

        assertTrue(result.isFailure)
        val exception = (result as Try.Failure).throwable
        assertEquals("Remote deletion sync failed", exception.message)
    }

    // -------------------------------------------------------------------------
    // syncWithRemote — resolves conflicts and inserts
    // -------------------------------------------------------------------------

    @Test
    fun `syncWithRemote with empty remote words resolves to nothing and inserts nothing`() = runTest {
        val local = FakeWordLocalDataSource()
        val remote = FakeWordRemoteSyncHandler().apply { remoteWordsToReturn = emptyList() }
        val resolver = FakeWordConflictResolver().apply { resolvedEntities = emptyList() }
        val repo = makeRepository(local = local, remote = remote, resolver = resolver)

        val result = repo.syncWithRemote()

        assertTrue(result.isSuccess)
        assertTrue(local.insertedWords.isEmpty())
    }

    @Test
    fun `syncWithRemote passes local and remote words to conflict resolver`() = runTest {
        val local = FakeWordLocalDataSource()
        val remoteWords = listOf(makeRemoteWord(id = 1L, originalWord = "cat", translation = "gato"))
        val remote = FakeWordRemoteSyncHandler().apply { remoteWordsToReturn = remoteWords }
        val resolver = FakeWordConflictResolver().apply { resolvedEntities = emptyList() }
        val repo = makeRepository(local = local, remote = remote, resolver = resolver)

        repo.syncWithRemote()

        assertEquals(remoteWords, resolver.lastRemoteWords)
        assertEquals(local.storedEntities, resolver.lastLocalWords)
    }

    @Test
    fun `syncWithRemote inserts resolved entities from conflict resolver`() = runTest {
        val local = FakeWordLocalDataSource()
        val remoteWords = listOf(makeRemoteWord(id = 1L, originalWord = "cat", translation = "gato"))
        val remote = FakeWordRemoteSyncHandler().apply { remoteWordsToReturn = remoteWords }
        val resolvedEntity = makeWordEntityData(id = 1, originalWord = "cat", translation = "gato")
        val resolver = FakeWordConflictResolver().apply { resolvedEntities = listOf(resolvedEntity) }
        val repo = makeRepository(local = local, remote = remote, resolver = resolver)

        val result = repo.syncWithRemote()

        assertTrue(result.isSuccess)
        assertEquals(1, local.insertedWords.size)
        assertEquals("cat", local.insertedWords.first().originalWord)
        assertEquals("gato", local.insertedWords.first().translation)
    }

    @Test
    fun `syncWithRemote moves local words to their server ids before storing the pulled words`() = runTest {
        val local = FakeWordLocalDataSource().apply { storedWords = mutableListOf(makeWord(id = 3, originalWord = "cat")) }
        val remote = FakeWordRemoteSyncHandler().apply { remoteWordsToReturn = listOf(makeRemoteWord(id = 40L)) }
        val resolver = FakeWordConflictResolver().apply { localIdMoves = mapOf(3 to 40) }
        val repo = makeRepository(local = local, remote = remote, resolver = resolver)

        repo.syncWithRemote()

        assertEquals(mapOf(3 to 40), local.idMoves)
        assertEquals(listOf(40), local.storedWords.map { it.id })
    }

    @Test
    fun `syncWithRemote with multiple resolved entities inserts all of them`() = runTest {
        val local = FakeWordLocalDataSource()
        val remoteWords = listOf(
            makeRemoteWord(id = 1L, originalWord = "cat", translation = "gato"),
            makeRemoteWord(id = 2L, originalWord = "dog", translation = "perro")
        )
        val remote = FakeWordRemoteSyncHandler().apply { remoteWordsToReturn = remoteWords }
        val resolvedEntities = listOf(
            makeWordEntityData(id = 1, originalWord = "cat", translation = "gato"),
            makeWordEntityData(id = 2, originalWord = "dog", translation = "perro")
        )
        val resolver = FakeWordConflictResolver().apply { this.resolvedEntities = resolvedEntities }
        val repo = makeRepository(local = local, remote = remote, resolver = resolver)

        val result = repo.syncWithRemote()

        assertTrue(result.isSuccess)
        assertEquals(2, local.insertedWords.size)
    }

    @Test
    fun `syncWithRemote returns failure when remote fetch fails`() = runTest {
        val local = FakeWordLocalDataSource()
        val remote = FakeWordRemoteSyncHandler().apply { shouldFailSyncFromRemote = true }
        val resolver = FakeWordConflictResolver()
        val repo = makeRepository(local = local, remote = remote, resolver = resolver)

        val result = repo.syncWithRemote()

        assertTrue(result.isFailure)
        assertTrue(local.insertedWords.isEmpty())
    }

    @Test
    fun `syncWithRemote remote failure does not propagate as unhandled exception`() = runTest {
        val local = FakeWordLocalDataSource()
        val remote = FakeWordRemoteSyncHandler().apply { shouldFailSyncFromRemote = true }
        val resolver = FakeWordConflictResolver()
        val repo = makeRepository(local = local, remote = remote, resolver = resolver)

        val result = repo.syncWithRemote()

        assertTrue(result.isFailure)
        val exception = (result as Try.Failure).throwable
        assertEquals("Remote fetch failed", exception.message)
    }

    // -------------------------------------------------------------------------
    // syncWithRemote — auth guard (BUG-7) and timestamp dedup (BUG-2 / BUG-8)
    // -------------------------------------------------------------------------

    @Test
    fun `syncWithRemote skips remote fetch when not authenticated`() = runTest {
        val remote = FakeWordRemoteSyncHandler()
        val session = FakeSessionManager(authenticated = false)
        val repo = makeRepository(remote = remote, session = session)

        val result = repo.syncWithRemote()

        assertTrue(result.isSuccess)
        assertEquals(0, remote.syncFromRemoteCallCount)
    }

    @Test
    fun `syncWithRemote fires remote fetch when authenticated`() = runTest {
        val remote = FakeWordRemoteSyncHandler()
        val session = FakeSessionManager(authenticated = true)
        val repo = makeRepository(remote = remote, session = session)

        repo.syncWithRemote()

        assertEquals(1, remote.syncFromRemoteCallCount)
    }

    @Test
    fun `syncWithRemote called twice in rapid succession only fires remote once`() = runTest {
        val remote = FakeWordRemoteSyncHandler()
        val repo = makeRepository(remote = remote)

        repo.syncWithRemote()  // first call — fires remote, stamps lastSyncedAt
        repo.syncWithRemote()  // second call — should be suppressed by timestamp guard

        assertEquals(1, remote.syncFromRemoteCallCount)
    }

    // -------------------------------------------------------------------------
    // getAllWordsAsync / getTotalCount / deleteAllWords
    // -------------------------------------------------------------------------

    @Test
    fun `getAllWordsAsync returns words from local data source`() = runTest {
        val local = FakeWordLocalDataSource()
        val word = makeWord(id = 1)
        local.storedWords.add(word)
        val repo = makeRepository(local = local)

        val result = repo.getAllWordsAsync()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
        assertEquals(word, result.getOrThrow().first())
    }

    @Test
    fun `getTotalCount returns word count from local data source`() = runTest {
        val local = FakeWordLocalDataSource()
        local.storedWords.addAll(listOf(makeWord(id = 1), makeWord(id = 2)))
        val repo = makeRepository(local = local)

        val result = repo.getTotalCount()

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow())
    }

    @Test
    fun `deleteAllWords delegates to local data source and returns success`() = runTest {
        val local = FakeWordLocalDataSource()
        local.storedWords.add(makeWord(id = 1))
        val repo = makeRepository(local = local)

        val result = repo.deleteAllWords()

        assertTrue(result.isSuccess)
        assertTrue(local.storedWords.isEmpty())
    }
}
