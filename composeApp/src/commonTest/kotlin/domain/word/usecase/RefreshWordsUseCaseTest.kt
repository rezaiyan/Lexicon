package domain.word.usecase

import core.common.Try
import domain.tag.usecase.SyncTagsFromRemoteUseCase
import domain.word.model.Word
import fakes.FakeReviewSyncRepository
import fakes.FakeTagRepository
import fakes.FakeWordRepository
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RefreshWordsUseCaseTest {

    private val reviewSync = FakeReviewSyncRepository()
    private val words = FakeWordRepository()
    private val tags = FakeTagRepository()
    private val useCase = RefreshWordsUseCase(
        FlushReviewSyncQueueUseCase(reviewSync, words),
        words,
        SyncTagsFromRemoteUseCase(tags),
    )

    @Test
    fun `invoke with no pending reviews pulls words`() = runTest {
        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        assertTrue(words.syncWithRemoteCalled)
    }

    @Test
    fun `invoke pulls tags too so tags made elsewhere show up`() = runTest {
        val result = useCase(Unit)

        assertTrue(result.isSuccess)
        assertTrue(tags.syncTagsFromRemoteCalled)
    }

    @Test
    fun `invoke when the tag pull fails still pulls words and returns failure`() = runTest {
        tags.syncTagsFromRemoteResult = Try.failure(RuntimeException("server down"))

        val result = useCase(Unit)

        assertTrue(result.isFailure)
        assertTrue(words.syncWithRemoteCalled)
    }

    @Test
    fun `invoke uploads pending reviews before pulling`() = runTest {
        words.storedWords = mutableListOf(word(1), word(2))
        reviewSync.enqueue(1)

        useCase(Unit)

        assertEquals(listOf(1), words.lastBatchSyncedWords.map { it.id })
        assertTrue(reviewSync.pendingIds.isEmpty())
        assertTrue(words.syncWithRemoteCalled)
    }

    @Test
    fun `invoke when pending reviews fail to upload skips the pull and keeps them queued`() = runTest {
        words.storedWords = mutableListOf(word(1))
        words.batchSyncResult = Try.failure(RuntimeException("offline"))
        reviewSync.enqueue(1)

        val result = useCase(Unit)

        assertTrue(result.isFailure)
        assertFalse(words.syncWithRemoteCalled)
        assertFalse(tags.syncTagsFromRemoteCalled)
        assertEquals(listOf(1), reviewSync.pendingIds)
    }

    @Test
    fun `invoke when the pull fails returns failure`() = runTest {
        words.syncResult = Try.failure(RuntimeException("server down"))

        val result = useCase(Unit)

        assertTrue(result.isFailure)
    }

    private fun word(id: Int) = Word(
        id = id,
        originalWord = "word$id",
        translation = "trans$id",
        description = "",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.GERMAN,
        nextReviewDate = 0L,
    )
}
