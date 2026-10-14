package domain.word.usecase

import core.common.Try
import domain.word.model.Word
import fakes.FakeReviewSyncRepository
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
    private val useCase = RefreshWordsUseCase(FlushReviewSyncQueueUseCase(reviewSync, words), words)

    @Test
    fun `invoke with no pending reviews pulls words`() = runTest {
        val result = useCase(Unit)

        assertTrue(result.isSuccess)
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
