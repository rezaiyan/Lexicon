package presentation.feature.onboarding

import domain.onboarding.model.SuggestedVocabulary
import core.base.BaseViewModel
import presentation.model.VocabularyPreviewUiState

class VocabularyPreviewViewModel : BaseViewModel<VocabularyPreviewUiState, VocabularyPreviewViewModel.Event>() {

    sealed interface Event {
        data class ProceedWithSelection(val words: List<SuggestedVocabulary>) : Event
        data object SkipVocabulary : Event
    }

    override fun initialState() = VocabularyPreviewUiState()

    fun setWords(words: List<SuggestedVocabulary>) {
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
        emitEffect(Event.ProceedWithSelection(selectedWords))
    }

    fun skip() {
        emitEffect(Event.SkipVocabulary)
    }
}
