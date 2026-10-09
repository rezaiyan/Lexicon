package data.ai.repository

import core.common.Try
import core.common.getOrThrow
import core.common.exceptionOrNull
import core.error.DomainError
import data.ai.remote.IAiRemoteDataSource
import data.ai.remote.model.ExtractWordsRequest
import data.ai.remote.model.ExtractWordsResponse
import data.ai.remote.model.ExtractedWordDto
import data.ai.remote.model.SuggestWordsRequest
import data.ai.remote.model.SuggestWordsResponse
import data.ai.remote.model.SuggestedWordDto
import domain.onboarding.model.ProficiencyLevel
import domain.word.add.model.LanguagePair
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AiRepositoryImplTest {

    private val remoteDataSource = FakeAiRemoteDataSource()

    private fun createRepo() = AiRepositoryImpl(remoteDataSource)

    // --- v2 contract ---

    private val germanFromEnglish = LanguagePair(Language.GERMAN, Language.ENGLISH)

    @Test
    fun `extractWords sends both languages and maps items to drafts`() = runTest {
        remoteDataSource.extractResult = Try.success(
            ExtractWordsResponse(listOf(ExtractedWordDto("gehen", "to go, to walk", "verb"), ExtractedWordDto(" ", "x"))),
        )

        val drafts = createRepo().extractWords(byteArrayOf(1, 2), germanFromEnglish).getOrThrow()

        assertEquals(listOf(Triple("gehen", "to go, to walk", "verb")), drafts.map { Triple(it.term, it.translation, it.note) })
        assertEquals("German", remoteDataSource.lastExtractRequest?.learningLanguage)
        assertEquals("English", remoteDataSource.lastExtractRequest?.nativeLanguage)
    }

    @Test
    fun `extractWords falls back to v1 on servers without the endpoint and asks for native translations`() = runTest {
        remoteDataSource.extractResult = Try.failure(DomainError.Network.ServerError(404))
        remoteDataSource.result = Try.success("Hund,dog;Katze,cat")

        val drafts = createRepo().extractWords(byteArrayOf(1, 2), germanFromEnglish).getOrThrow()

        assertEquals(listOf("Hund", "Katze"), drafts.map { it.term })
        assertEquals(Language.ENGLISH, remoteDataSource.lastTargetLanguage)
    }

    @Test
    fun `extractWords keeps other failures`() = runTest {
        remoteDataSource.extractResult = Try.failure(DomainError.Commerce.PremiumRequired)

        val error = createRepo().extractWords(byteArrayOf(1), germanFromEnglish).exceptionOrNull()

        assertIs<DomainError.Commerce.PremiumRequired>(error)
        assertEquals(null, remoteDataSource.lastTargetLanguage)
    }

    @Test
    fun `suggestWords sends display names plus the language code used for skipping known words`() = runTest {
        remoteDataSource.suggestResult = Try.success(SuggestWordsResponse(listOf(SuggestedWordDto("Brot", "bread"))))

        val drafts = createRepo().suggestWords(germanFromEnglish, ProficiencyLevel.INTERMEDIATE, listOf("Food")).getOrThrow()

        assertEquals(listOf("Brot"), drafts.map { it.term })
        assertEquals(
            SuggestWordsRequest("German", "English", "intermediate", listOf("Food"), targetLanguageCode = "de"),
            remoteDataSource.lastSuggestRequest,
        )
    }

    // --- Fakes ---

    private class FakeAiRemoteDataSource : IAiRemoteDataSource {
        var result: Try<String> = Try.success("")
        var extractResult: Try<ExtractWordsResponse> = Try.success(ExtractWordsResponse())
        var suggestResult: Try<SuggestWordsResponse> = Try.success(SuggestWordsResponse())
        var lastExtractRequest: ExtractWordsRequest? = null
        var lastSuggestRequest: SuggestWordsRequest? = null

        override suspend fun extractWords(request: ExtractWordsRequest): Try<ExtractWordsResponse> {
            lastExtractRequest = request
            return extractResult
        }

        override suspend fun suggestWords(request: SuggestWordsRequest): Try<SuggestWordsResponse> {
            lastSuggestRequest = request
            return suggestResult
        }
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
