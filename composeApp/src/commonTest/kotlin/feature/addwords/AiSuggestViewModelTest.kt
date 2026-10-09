package feature.addwords

import app.cash.turbine.test
import core.common.Try
import core.common.getOrThrow
import core.error.DomainError
import domain.onboarding.model.ProficiencyLevel
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft
import domain.word.add.usecase.SuggestWordsUseCase
import fakes.FakeAiRepository
import fakes.FakeAnalyticsTracker
import feature.addwords.model.AddWordsProblem
import feature.addwords.model.SourceEffect
import feature.addwords.source.AiSuggestViewModel
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class AiSuggestViewModelTest : ViewModelTestBase() {

    private val languages = LanguagePair(Language.SPANISH, Language.ENGLISH)
    private val ai = FakeAiRepository()

    private val analytics = FakeAnalyticsTracker()

    private fun createViewModel() = AiSuggestViewModel(SuggestWordsUseCase(ai), analytics)

    @Test
    fun `generate without a level does nothing`() {
        val vm = createViewModel()

        vm.generate(languages)

        assertNull(ai.lastSuggestionRequest)
        assertFalse(vm.currentState.canGenerate)
    }

    @Test
    fun `generate asks for the level and topics in the sheet languages`() = runTest {
        ai.suggestions = Try.success(listOf(WordDraft.of("perro", "dog").getOrThrow()))
        val vm = createViewModel()
        vm.selectLevel(ProficiencyLevel.INTERMEDIATE)
        vm.toggleTopic("Travel")
        vm.toggleTopic("Food")
        vm.toggleTopic("Travel")

        vm.effects.test {
            vm.generate(languages)
            val ready = assertIs<SourceEffect.CandidatesReady>(awaitItem())
            assertEquals(listOf("perro"), ready.drafts.map { it.term })
        }
        assertEquals(Triple(languages, ProficiencyLevel.INTERMEDIATE, listOf("Food")), ai.lastSuggestionRequest)
    }

    @Test
    fun `generate with no suggestions says so`() {
        ai.suggestions = Try.success(emptyList())
        val vm = createViewModel()
        vm.selectLevel(ProficiencyLevel.BEGINNER)

        vm.generate(languages)

        assertEquals(AddWordsProblem.NothingSuggested, vm.currentState.problem)
        assertFalse(vm.currentState.isGenerating)
    }

    @Test
    fun `generate refused for premium reports the lapse`() = runTest {
        ai.suggestions = Try.failure(DomainError.Commerce.PremiumRequired)
        val vm = createViewModel()
        vm.selectLevel(ProficiencyLevel.BEGINNER)

        vm.effects.test {
            vm.generate(languages)
            assertEquals(SourceEffect.PremiumLapsed, awaitItem())
        }
    }

    @Test
    fun `generate failure logs the topics picked and the failure`() {
        ai.suggestions = Try.failure(DomainError.Network.NoConnection)
        val vm = createViewModel()
        vm.selectLevel(ProficiencyLevel.BEGINNER)
        vm.toggleTopic("Travel")

        vm.generate(languages)

        assertEquals(
            listOf<Pair<String, Map<String, Any>?>>(
                "import_topic_entered" to mapOf("method" to "ai", "topic" to "Travel"),
                "import_failed" to mapOf("method" to "ai", "step" to "extract", "error_type" to "Offline"),
            ),
            analytics.events,
        )
    }
}
