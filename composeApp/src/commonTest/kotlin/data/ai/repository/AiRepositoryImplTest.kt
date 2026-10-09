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
import fakes.FakeCreditsRepository
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AiRepositoryImplTest {

    private val remoteDataSource = FakeAiRemoteDataSource()
    private val credits = FakeCreditsRepository()

    private fun createRepo() = AiRepositoryImpl(remoteDataSource, credits)

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
    fun `extractWords reports a missing endpoint as a failure instead of falling back to v1`() = runTest {
        remoteDataSource.extractResult = Try.failure(DomainError.Network.ServerError(404))

        val error = createRepo().extractWords(byteArrayOf(1, 2), germanFromEnglish).exceptionOrNull()

        assertIs<DomainError.Network.ServerError>(error)
    }

    @Test
    fun `extractWords keeps other failures`() = runTest {
        remoteDataSource.extractResult = Try.failure(DomainError.Commerce.PremiumRequired)

        val error = createRepo().extractWords(byteArrayOf(1), germanFromEnglish).exceptionOrNull()

        assertIs<DomainError.Commerce.PremiumRequired>(error)
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

    // --- Credits ---

    @Test
    fun `every paid call re-reads the credit balance whatever the outcome`() = runTest {
        val repo = createRepo()

        repo.extractWords(byteArrayOf(1), germanFromEnglish)
        remoteDataSource.suggestResult = Try.failure(DomainError.Commerce.InsufficientCredits)
        repo.suggestWords(germanFromEnglish, ProficiencyLevel.BEGINNER, emptyList())

        assertEquals(2, credits.invalidateCount)
    }

    // --- Fakes ---

    private class FakeAiRemoteDataSource : IAiRemoteDataSource {
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
    }
}
