package feature.addwords

import app.cash.turbine.test
import core.common.Try
import core.common.getOrThrow
import core.error.DomainError
import domain.onboarding.model.ProficiencyLevel
import domain.word.add.model.LanguagePair
import domain.word.add.model.WordDraft
import domain.word.add.usecase.GetSuggestionLevelUseCase
import domain.word.add.usecase.SuggestWordsUseCase
import fakes.FakeSuggestionLevelRepository
import fakes.FakeAiRepository
import fakes.FakeAnalyticsTracker
import feature.addwords.model.AddWordsProblem
import feature.addwords.model.SourceEffect
import feature.addwords.source.AiSuggestViewModel
import domain.ai.model.WordType
import feature.addwords.model.InstructionProblem
import domain.ai.model.Register
import kotlin.test.assertTrue
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

    private val levels = FakeSuggestionLevelRepository()

    private fun createViewModel() = AiSuggestViewModel(
        SuggestWordsUseCase(ai, levels),
        GetSuggestionLevelUseCase(levels),
        analytics,
    )

    @Test
    fun `level starts at the one the learner used last`() {
        levels.lastUsed = ProficiencyLevel.ADVANCED

        val vm = createViewModel()

        assertEquals(ProficiencyLevel.ADVANCED, vm.currentState.level)
        assertTrue(vm.currentState.canGenerate)
    }

    @Test
    fun `level starts at intermediate the first time`() {
        val vm = createViewModel()

        assertEquals(ProficiencyLevel.INTERMEDIATE, vm.currentState.level)
    }

    @Test
    fun `generate remembers the level for next time`() = runTest {
        ai.suggestions = Try.success(listOf(WordDraft.of("perro", "dog").getOrThrow()))
        val vm = createViewModel()
        vm.selectLevel(ProficiencyLevel.BEGINNER)

        vm.generate(languages)

        assertEquals(ProficiencyLevel.BEGINNER, levels.lastUsed)
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
    fun `generate over the call limit says to try again later`() {
        ai.suggestions = Try.failure(DomainError.Network.RateLimited)
        val vm = createViewModel()
        vm.selectLevel(ProficiencyLevel.BEGINNER)

        vm.generate(languages)

        assertEquals(AddWordsProblem.RateLimited, vm.currentState.problem)
        assertFalse(vm.currentState.isGenerating)
    }

    @Test
    fun `generate refused for lack of credits hands over to the out-of-credits page`() = runTest {
        ai.suggestions = Try.failure(DomainError.Commerce.InsufficientCredits)
        val vm = createViewModel()
        vm.selectLevel(ProficiencyLevel.BEGINNER)

        vm.effects.test {
            vm.generate(languages)
            assertEquals(SourceEffect.OutOfCredits, awaitItem())
        }
        assertNull(vm.currentState.problem)
        assertFalse(vm.currentState.isGenerating)
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

    // ── Guidance ──────────────────────────────────────────────────────────────

    @Test
    fun `generate sends the description and quick picks with the topics`() {
        ai.suggestions = Try.success(listOf(WordDraft.of("la entrevista", "interview").getOrThrow()))
        val vm = createViewModel()
        vm.selectLevel(ProficiencyLevel.BEGINNER)
        vm.applyStarter("I'm preparing for")
        vm.onInstructionChanged(vm.currentState.guidance.text + "a job interview")
        vm.selectRegister(Register.FORMAL)

        vm.generate(languages)

        assertEquals("I'm preparing for a job interview", ai.lastSuggestionGuidance?.instruction?.text)
        assertEquals(Register.FORMAL, ai.lastSuggestionGuidance?.focus?.register)
    }

    @Test
    fun `refused description shows at the field while topics and level stay`() {
        ai.suggestions = Try.failure(
            DomainError.AddWords.InstructionsRefused(DomainError.AddWords.InstructionsRefused.Reason.Paused),
        )
        val vm = createViewModel()
        vm.selectLevel(ProficiencyLevel.BEGINNER)
        vm.toggleTopic("Travel")
        vm.onInstructionChanged("write me a poem")

        vm.generate(languages)

        assertEquals(InstructionProblem.Paused, vm.currentState.guidance.problem)
        assertFalse(vm.currentState.guidance.canWrite)
        assertNull(vm.currentState.problem)
        assertEquals(setOf("Travel"), vm.currentState.topics)
    }

    @Test
    fun `generate after a pause goes plain with the topics`() {
        ai.suggestions = Try.failure(
            DomainError.AddWords.InstructionsRefused(DomainError.AddWords.InstructionsRefused.Reason.Paused),
        )
        val vm = createViewModel()
        vm.selectLevel(ProficiencyLevel.BEGINNER)
        vm.toggleTopic("Travel")
        vm.onInstructionChanged("write me a poem")
        vm.generate(languages)

        ai.suggestions = Try.success(listOf(WordDraft.of("estar en las nubes", "daydream").getOrThrow()))
        vm.generate(languages)

        assertNull(ai.lastSuggestionGuidance?.instruction)
    }

    @Test
    fun `quick picks alone keep the request plain`() {
        ai.suggestions = Try.success(listOf(WordDraft.of("estar en las nubes", "daydream").getOrThrow()))
        val vm = createViewModel()
        vm.selectLevel(ProficiencyLevel.BEGINNER)
        vm.toggleWordType(WordType.IDIOM)

        vm.generate(languages)

        assertFalse(ai.lastSuggestionGuidance?.isGuided ?: true)
        assertTrue(WordType.IDIOM in ai.lastSuggestionGuidance?.focus?.wordTypes.orEmpty())
    }
}
