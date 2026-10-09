package domain.word.add

import core.common.Try
import core.common.exceptionOrNull
import core.common.getOrThrow
import core.error.DomainError
import domain.onboarding.model.ProficiencyLevel
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft
import domain.word.add.usecase.SuggestWordsUseCase
import fakes.FakeAiRepository
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SuggestWordsUseCaseTest {

    private val ai = FakeAiRepository()
    private val useCase = SuggestWordsUseCase(ai)
    private val languages = LanguagePair(Language.GERMAN, Language.ENGLISH)
    private val request = SuggestWordsUseCase.Params(languages, ProficiencyLevel.BEGINNER, listOf("Food"))

    @Test
    fun `invoke asks for the requested pair level and topics`() = runTest {
        ai.suggestions = Try.success(listOf(WordDraft.of("Brot", "bread").getOrThrow()))

        val drafts = useCase(request).getOrThrow()

        assertEquals(listOf("Brot"), drafts.map { it.term })
        assertEquals(Triple(languages, ProficiencyLevel.BEGINNER, listOf("Food")), ai.lastSuggestionRequest)
    }

    @Test
    fun `invoke with no suggestion fails with NothingRecognized`() = runTest {
        assertIs<DomainError.AddWords.NothingRecognized>(useCase(request).exceptionOrNull())
    }
}
