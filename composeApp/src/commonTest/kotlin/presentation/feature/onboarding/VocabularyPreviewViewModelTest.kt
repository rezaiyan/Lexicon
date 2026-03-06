package presentation.feature.onboarding

import domain.onboarding.model.SuggestedVocabulary
import feature.onboarding.VocabularyPreviewViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class VocabularyPreviewViewModelTest : ViewModelTestBase() {

    private fun createViewModel() = VocabularyPreviewViewModel()

    private fun testWords() = listOf(
        SuggestedVocabulary("hola", "hello", "greeting", "es", "en"),
        SuggestedVocabulary("gato", "cat", "animal", "es", "en"),
        SuggestedVocabulary("casa", "house", "building", "es", "en")
    )

    @Test
    fun `initial state has empty words and no selection`() {
        val vm = createViewModel()
        assertEquals(emptyList(), vm.currentState.words)
        assertEquals(emptySet(), vm.currentState.selectedIndices)
    }

    @Test
    fun `setWords initializes state with all indices selected`() {
        val vm = createViewModel()
        val words = testWords()

        vm.setWords(words)

        assertEquals(words, vm.currentState.words)
        assertEquals(setOf(0, 1, 2), vm.currentState.selectedIndices)
        assertEquals(3, vm.currentState.selectedCount)
    }

    @Test
    fun `proceedWithSelected emits ProceedWithSelection with all words`() =
        runTest(UnconfinedTestDispatcher()) {
            val vm = createViewModel()
            val words = testWords()
            vm.setWords(words)

            vm.proceedWithSelected()

            val event = vm.effects.first()
            assertIs<VocabularyPreviewViewModel.Event.ProceedWithSelection>(event)
            assertEquals(words, event.words)
        }

    @Test
    fun `skip emits SkipVocabulary`() = runTest(UnconfinedTestDispatcher()) {
        val vm = createViewModel()

        vm.skip()

        val event = vm.effects.first()
        assertEquals(VocabularyPreviewViewModel.Event.SkipVocabulary, event)
    }
}
