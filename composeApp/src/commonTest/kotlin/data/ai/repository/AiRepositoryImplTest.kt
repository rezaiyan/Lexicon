package data.ai.repository

import core.common.Try
import core.common.getOrThrow
import data.ai.remote.IAiRemoteDataSource
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiRepositoryImplTest {

    private val remoteDataSource = FakeAiRemoteDataSource()

    private fun createRepo() = AiRepositoryImpl(remoteDataSource)

    @Test
    fun `extractVocabularyFromImage delegates to data source`() = runTest {
        remoteDataSource.result = Try.success("hello,hola")
        val repo = createRepo()

        val result = repo.extractVocabularyFromImage(
            imageBytes = byteArrayOf(1, 2, 3),
            targetLanguage = Language.SPANISH,
            extractWords = true,
            extractSentences = false
        )

        assertTrue(result.isSuccess)
        assertEquals("hello,hola", result.getOrThrow())
    }

    @Test
    fun `extractVocabularyFromImage passes parameters correctly`() = runTest {
        remoteDataSource.result = Try.success("")
        val repo = createRepo()

        repo.extractVocabularyFromImage(
            imageBytes = byteArrayOf(4, 5),
            targetLanguage = Language.GERMAN,
            extractWords = false,
            extractSentences = true
        )

        assertEquals(Language.GERMAN, remoteDataSource.lastTargetLanguage)
        assertEquals(false, remoteDataSource.lastExtractWords)
        assertEquals(true, remoteDataSource.lastExtractSentences)
    }

    @Test
    fun `extractVocabularyFromImage returns failure on error`() = runTest {
        remoteDataSource.result = Try.failure(RuntimeException("AI unavailable"))
        val repo = createRepo()

        val result = repo.extractVocabularyFromImage(
            byteArrayOf(1), Language.ENGLISH, true, false
        )

        assertTrue(result.isFailure)
    }

    // --- Fakes ---

    private class FakeAiRemoteDataSource : IAiRemoteDataSource {
        var result: Try<String> = Try.success("")
        var lastTargetLanguage: Language? = null
        var lastExtractWords: Boolean? = null
        var lastExtractSentences: Boolean? = null

        override suspend fun extractVocabularyFromImage(
            imageBytes: ByteArray,
            targetLanguage: Language,
            extractWords: Boolean,
            extractSentences: Boolean
        ): Try<String> {
            lastTargetLanguage = targetLanguage
            lastExtractWords = extractWords
            lastExtractSentences = extractSentences
            return result
        }
    }
}
