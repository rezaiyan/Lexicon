package data.word.sync

import core.common.Try
import data.word.remote.IWordRemoteDataSource
import data.word.remote.model.BatchUpdateLanguagesRequest
import data.word.remote.model.RemoteWord
import data.word.repository.makeWord
import domain.word.model.Word
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WordRemoteSyncHandlerTest {

    // region Fake

    private class FakeWordRemoteDataSource : IWordRemoteDataSource {
        var getWordsResult: Try<List<RemoteWord>> = Try.success(emptyList())
        var upsertWordsResult: Try<Unit> = Try.success(Unit)
        var updateWordResult: Try<Unit> = Try.success(Unit)
        var deleteWordResult: Try<Unit> = Try.success(Unit)
        var deleteWordsResult: Try<Unit> = Try.success(Unit)
        var batchUpdateLanguagesResult: Try<Unit> = Try.success(Unit)

        var upsertedWords: List<RemoteWord>? = null
        var updatedId: Long? = null
        var updatedWord: RemoteWord? = null
        var deletedId: Long? = null
        var deletedIds: List<Long>? = null
        var batchUpdateRequest: BatchUpdateLanguagesRequest? = null

        override suspend fun getWords(): Try<List<RemoteWord>> = getWordsResult
        override suspend fun upsertWords(words: List<RemoteWord>): Try<Unit> {
            upsertedWords = words
            return upsertWordsResult
        }
        override suspend fun updateWord(id: Long, word: RemoteWord): Try<Unit> {
            updatedId = id
            updatedWord = word
            return updateWordResult
        }
        override suspend fun deleteWord(id: Long): Try<Unit> {
            deletedId = id
            return deleteWordResult
        }
        override suspend fun deleteWords(ids: List<Long>): Try<Unit> {
            deletedIds = ids
            return deleteWordsResult
        }
        override suspend fun batchUpdateLanguages(request: BatchUpdateLanguagesRequest): Try<Unit> {
            batchUpdateRequest = request
            return batchUpdateLanguagesResult
        }
    }

    // endregion

    private val fakeDataSource = FakeWordRemoteDataSource()
    private val handler = WordRemoteSyncHandler(fakeDataSource)

    // region syncWordsToRemote

    @Test
    fun syncWordsToRemote_emptyList_returnsSuccessWithoutCallingDataSource() = runTest {
        val result = handler.syncWordsToRemote(emptyList())

        assertTrue(result.isSuccess)
        assertEquals(null, fakeDataSource.upsertedWords)
    }

    @Test
    fun syncWordsToRemote_mapsWordsToRemoteAndCallsUpsert() = runTest {
        val words = listOf(
            makeWord(id = 1, originalWord = "hello", translation = "hola"),
            makeWord(id = 2, originalWord = "world", translation = "mundo")
        )

        val result = handler.syncWordsToRemote(words)

        assertTrue(result.isSuccess)
        val upserted = fakeDataSource.upsertedWords!!
        assertEquals(2, upserted.size)
        assertEquals("hello", upserted[0].originalWord)
        assertEquals("hola", upserted[0].translation)
        assertEquals(1L, upserted[0].id)
        assertEquals("world", upserted[1].originalWord)
        assertEquals("mundo", upserted[1].translation)
        assertEquals(2L, upserted[1].id)
    }

    @Test
    fun syncWordsToRemote_wordWithIdZero_mapsIdToNull() = runTest {
        val words = listOf(makeWord(id = 0))

        handler.syncWordsToRemote(words)

        val upserted = fakeDataSource.upsertedWords!!
        assertEquals(null, upserted[0].id)
    }

    @Test
    fun syncWordsToRemote_mapsLanguageCodes() = runTest {
        val word = makeWord(
            sourceLanguage = Language.ENGLISH,
            targetLanguage = Language.SPANISH
        )

        handler.syncWordsToRemote(listOf(word))

        val upserted = fakeDataSource.upsertedWords!!.first()
        assertEquals("en", upserted.sourceLanguage)
        assertEquals("es", upserted.targetLanguage)
    }

    @Test
    fun syncWordsToRemote_dataSourceFails_returnsFailure() = runTest {
        fakeDataSource.upsertWordsResult = Try.failure(RuntimeException("network error"))

        val result = handler.syncWordsToRemote(listOf(makeWord()))

        assertTrue(result.isFailure)
    }

    // endregion

    // region syncWordUpdateToRemote

    @Test
    fun syncWordUpdateToRemote_delegatesToDataSourceWithCorrectIdAndMapping() = runTest {
        val word = makeWord(id = 5, originalWord = "cat", translation = "gato")

        val result = handler.syncWordUpdateToRemote(42L, word)

        assertTrue(result.isSuccess)
        assertEquals(42L, fakeDataSource.updatedId)
        assertEquals("cat", fakeDataSource.updatedWord!!.originalWord)
        assertEquals("gato", fakeDataSource.updatedWord!!.translation)
    }

    @Test
    fun syncWordUpdateToRemote_dataSourceFails_returnsFailure() = runTest {
        fakeDataSource.updateWordResult = Try.failure(RuntimeException("update failed"))

        val result = handler.syncWordUpdateToRemote(1L, makeWord())

        assertTrue(result.isFailure)
    }

    // endregion

    // region syncWordDeletionToRemote

    @Test
    fun syncWordDeletionToRemote_delegatesToDataSource() = runTest {
        val result = handler.syncWordDeletionToRemote(99L)

        assertTrue(result.isSuccess)
        assertEquals(99L, fakeDataSource.deletedId)
    }

    @Test
    fun syncWordDeletionToRemote_dataSourceFails_returnsFailure() = runTest {
        fakeDataSource.deleteWordResult = Try.failure(RuntimeException("delete failed"))

        val result = handler.syncWordDeletionToRemote(1L)

        assertTrue(result.isFailure)
    }

    // endregion

    // region syncWordsDeletionToRemote

    @Test
    fun syncWordsDeletionToRemote_emptyList_returnsSuccessWithoutCallingDataSource() = runTest {
        val result = handler.syncWordsDeletionToRemote(emptyList())

        assertTrue(result.isSuccess)
        assertEquals(null, fakeDataSource.deletedIds)
    }

    @Test
    fun syncWordsDeletionToRemote_delegatesToDataSource() = runTest {
        val ids = listOf(1L, 2L, 3L)

        val result = handler.syncWordsDeletionToRemote(ids)

        assertTrue(result.isSuccess)
        assertEquals(ids, fakeDataSource.deletedIds)
    }

    @Test
    fun syncWordsDeletionToRemote_dataSourceFails_returnsFailure() = runTest {
        fakeDataSource.deleteWordsResult = Try.failure(RuntimeException("bulk delete failed"))

        val result = handler.syncWordsDeletionToRemote(listOf(1L))

        assertTrue(result.isFailure)
    }

    // endregion

    // region syncBatchLanguageUpdateToRemote

    @Test
    fun syncBatchLanguageUpdateToRemote_emptyIds_returnsSuccessWithoutCallingDataSource() = runTest {
        val result = handler.syncBatchLanguageUpdateToRemote(emptyList(), "en", "es")

        assertTrue(result.isSuccess)
        assertEquals(null, fakeDataSource.batchUpdateRequest)
    }

    @Test
    fun syncBatchLanguageUpdateToRemote_buildsCorrectRequest() = runTest {
        val ids = listOf(10L, 20L)

        val result = handler.syncBatchLanguageUpdateToRemote(ids, "fr", "de")

        assertTrue(result.isSuccess)
        val request = fakeDataSource.batchUpdateRequest!!
        assertEquals(ids, request.ids)
        assertEquals("fr", request.sourceLanguage)
        assertEquals("de", request.targetLanguage)
    }

    @Test
    fun syncBatchLanguageUpdateToRemote_nullLanguages_passesNullsInRequest() = runTest {
        val result = handler.syncBatchLanguageUpdateToRemote(listOf(1L), null, null)

        assertTrue(result.isSuccess)
        val request = fakeDataSource.batchUpdateRequest!!
        assertEquals(null, request.sourceLanguage)
        assertEquals(null, request.targetLanguage)
    }

    @Test
    fun syncBatchLanguageUpdateToRemote_dataSourceFails_returnsFailure() = runTest {
        fakeDataSource.batchUpdateLanguagesResult = Try.failure(RuntimeException("batch failed"))

        val result = handler.syncBatchLanguageUpdateToRemote(listOf(1L), "en", "es")

        assertTrue(result.isFailure)
    }

    // endregion

    // region syncFromRemote

    @Test
    fun syncFromRemote_returnsWordsFromDataSource() = runTest {
        val remoteWords = listOf(
            RemoteWord(
                id = 1L, originalWord = "dog", translation = "perro",
                description = "", sourceLanguage = "en", targetLanguage = "es",
                level = 0, easeFactor = 2.5f, interval = 0, repetitions = 0,
                lastReviewDate = 0L, nextReviewDate = 0L, createdAt = null
            )
        )
        fakeDataSource.getWordsResult = Try.success(remoteWords)

        val result = handler.syncFromRemote()

        assertTrue(result.isSuccess)
        assertEquals(remoteWords, (result as Try.Success).value)
    }

    @Test
    fun syncFromRemote_dataSourceFails_returnsFailure() = runTest {
        fakeDataSource.getWordsResult = Try.failure(RuntimeException("fetch failed"))

        val result = handler.syncFromRemote()

        assertTrue(result.isFailure)
    }

    // endregion

    // region toRemote mapping

    @Test
    fun syncWordsToRemote_mapsAllFieldsCorrectly() = runTest {
        val word = Word(
            id = 7,
            originalWord = "book",
            translation = "libro",
            description = "a written work",
            sourceLanguage = Language.ENGLISH,
            targetLanguage = Language.SPANISH,
            level = 3,
            easeFactor = 2.1f,
            interval = 10,
            repetitions = 5,
            lastReviewDate = 1000L,
            nextReviewDate = 2000L,
            dateAdded = 500L
        )

        handler.syncWordsToRemote(listOf(word))

        val remote = fakeDataSource.upsertedWords!!.first()
        assertEquals(7L, remote.id)
        assertEquals("book", remote.originalWord)
        assertEquals("libro", remote.translation)
        assertEquals("a written work", remote.description)
        assertEquals("en", remote.sourceLanguage)
        assertEquals("es", remote.targetLanguage)
        assertEquals(3, remote.level)
        assertEquals(2.1f, remote.easeFactor)
        assertEquals(10, remote.interval)
        assertEquals(5, remote.repetitions)
        assertEquals(1000L, remote.lastReviewDate)
        assertEquals(2000L, remote.nextReviewDate)
        assertEquals(500L, remote.createdAt)
    }

    // endregion
}
