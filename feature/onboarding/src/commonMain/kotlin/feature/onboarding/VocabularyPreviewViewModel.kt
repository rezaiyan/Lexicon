package feature.onboarding

import core.base.BaseViewModel
import feature.onboarding.model.VocabularyPreviewEffect
import feature.onboarding.model.VocabularyPreviewUiState

class VocabularyPreviewViewModel : BaseViewModel<VocabularyPreviewUiState, VocabularyPreviewEffect>() {

    override fun initialState() = VocabularyPreviewUiState()

    fun setWords(words: List<domain.onboarding.model.SuggestedVocabulary>) {
        updateState {
            copy(
                words = words,
                selectedIndices = words.indices.toSet()
            )
        }
    }

    fun proceedWithSelected() {
        val state = currentState
        val selectedWords = state.selectedIndices.map { state.words[it] }
        emitEffect(VocabularyPreviewEffect.ProceedWithSelection(selectedWords))
    }

    fun skip() {
        emitEffect(VocabularyPreviewEffect.SkipVocabulary)
    }
}
