package presentation.feature.onboarding

import domain.onboarding.model.SuggestedVocabulary
import feature.onboarding.VocabularyPreviewViewModel
import feature.onboarding.model.VocabularyPreviewEffect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class VocabularyPreviewViewModelTest : ViewModelTestBase() {

    private fun testWords(source: String = "English", target: String = "Spanish") = listOf(
        SuggestedVocabulary("hola", "hello", "greeting", source, target),
        SuggestedVocabulary("gato", "cat", "animal", source, target),
        SuggestedVocabulary("casa", "house", "building", source, target),
    )

    @Test
    fun `init holds the words and resolves display-name languages`() {
        val words = testWords()

        val vm = VocabularyPreviewViewModel(words)

        assertEquals(words, vm.currentState.words)
        assertEquals(Language.SPANISH, vm.currentState.learningLanguage)
        assertEquals(Language.ENGLISH, vm.currentState.nativeLanguage)
    }

    @Test
    fun `init resolves language codes`() {
        val vm = VocabularyPreviewViewModel(testWords(source = "en", target = "de"))

        assertEquals(Language.GERMAN, vm.currentState.learningLanguage)
        assertEquals(Language.ENGLISH, vm.currentState.nativeLanguage)
    }

    @Test
    fun `init when a language is unknown leaves it empty`() {
        val vm = VocabularyPreviewViewModel(testWords(target = "Klingon"))

        assertNull(vm.currentState.learningLanguage)
    }

    @Test
    fun `init with no words has no languages`() {
        val vm = VocabularyPreviewViewModel(emptyList())

        assertEquals(emptyList(), vm.currentState.words)
        assertNull(vm.currentState.learningLanguage)
    }

    @Test
    fun `addWords emits AddWords with every word`() = runTest(UnconfinedTestDispatcher()) {
        val words = testWords()
        val vm = VocabularyPreviewViewModel(words)

        vm.addWords()

        val effect = vm.effects.first()
        assertIs<VocabularyPreviewEffect.AddWords>(effect)
        assertEquals(words, effect.words)
    }

    @Test
    fun `startEmpty emits StartEmpty`() = runTest(UnconfinedTestDispatcher()) {
        val vm = VocabularyPreviewViewModel(testWords())

        vm.startEmpty()

        assertEquals(VocabularyPreviewEffect.StartEmpty, vm.effects.first())
    }
}
