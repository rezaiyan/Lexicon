package data.word.repository

import core.common.getOrThrow
import domain.word.add.model.AddWordsOutcome
import domain.word.model.Word
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WordRepositoryAddWordsTest {

    private fun newWord(term: String, translation: String, learning: Language = Language.GERMAN) =
        Word.newCard(term, translation, "", learning, Language.ENGLISH, emptyList(), nowMillis = 1L)

    @Test
    fun `addWords stores new words and skips existing ones without touching them`() = runTest {
        val local = FakeWordLocalDataSource()
        val repository = makeRepository(local = local)
        repository.addWords(listOf(newWord("Hund", "dog")))

        val outcome = repository.addWords(listOf(newWord("hund", "Dog"), newWord("Katze", "cat"))).getOrThrow()

        assertEquals(AddWordsOutcome(1, 1, listOf("Katze")), outcome)
        assertTrue(local.updatedWords.isEmpty())
    }

    @Test
    fun `addWords uploads the new words in the background with server-assigned ids`() = runTest {
        val local = FakeWordLocalDataSource()
        val remote = FakeWordRemoteSyncHandler()
        val repository = makeRepository(local = local, remote = remote)

        repository.addWords(listOf(newWord("Hund", "dog")))

        assertEquals(listOf("Hund"), remote.syncedWords.map { it.originalWord })
        assertTrue(remote.syncedWords.all { it.id == 0 })
        assertTrue(local.pendingUploadIds.isEmpty())
    }

    @Test
    fun `addWords succeeds offline and keeps the words queued`() = runTest {
        val local = FakeWordLocalDataSource()
        val remote = FakeWordRemoteSyncHandler().apply { shouldFailSyncWordsToRemote = true }
        val repository = makeRepository(local = local, remote = remote)

        val outcome = repository.addWords(listOf(newWord("Hund", "dog"))).getOrThrow()

        assertEquals(1, outcome.added)
        assertEquals(1, local.pendingUploadIds.size)
    }

    @Test
    fun `uploadPendingWords sends queued words once the connection is back`() = runTest {
        val local = FakeWordLocalDataSource()
        val remote = FakeWordRemoteSyncHandler().apply { shouldFailSyncWordsToRemote = true }
        val repository = makeRepository(local = local, remote = remote)
        repository.addWords(listOf(newWord("Hund", "dog")))
        remote.shouldFailSyncWordsToRemote = false

        val sent = repository.uploadPendingWords().getOrThrow()

        assertEquals(1, sent)
        assertTrue(local.pendingUploadIds.isEmpty())
    }

    @Test
    fun `uploadPendingWords waits while signed out`() = runTest {
        val local = FakeWordLocalDataSource()
        val remote = FakeWordRemoteSyncHandler()
        val repository = makeRepository(local = local, remote = remote, session = FakeSessionManager(authenticated = false))

        repository.addWords(listOf(newWord("Hund", "dog")))

        assertEquals(0, remote.syncWordsToRemoteCallCount)
        assertEquals(1, local.pendingUploadIds.size)
    }

    @Test
    fun `syncWithRemote pushes queued words even when the pull is skipped as fresh`() = runTest {
        val local = FakeWordLocalDataSource()
        val remote = FakeWordRemoteSyncHandler().apply { shouldFailSyncWordsToRemote = true }
        val settings = FakeSettingsLocalDataSource().apply { wordSyncTimestamp = Long.MAX_VALUE / 2 }
        val repository = makeRepository(local = local, remote = remote, settings = settings)
        repository.addWords(listOf(newWord("Hund", "dog")))
        remote.shouldFailSyncWordsToRemote = false

        repository.syncWithRemote()

        assertTrue(local.pendingUploadIds.isEmpty())
        assertEquals(0, remote.syncFromRemoteCallCount)
    }

    @Test
    fun `addWords does not move the remote sync timestamp`() = runTest {
        val settings = FakeSettingsLocalDataSource()
        val repository = makeRepository(settings = settings)
        val before = settings.getWordSyncTimestamp()

        repository.addWords(listOf(newWord("Hund", "dog")))

        assertEquals(before, settings.getWordSyncTimestamp())
    }

    @Test
    fun `addWords reports local failure`() = runTest {
        val local = FakeWordLocalDataSource().apply { shouldThrowOnInsert = true }

        assertTrue(makeRepository(local = local).addWords(listOf(newWord("Hund", "dog"))).isFailure)
    }
}
