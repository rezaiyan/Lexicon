package presentation.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.onboarding.model.SuggestedVocabulary
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.model.VocabularyPreviewUiState

class VocabularyPreviewViewModel : ViewModel() {

    private val _state = MutableStateFlow(VocabularyPreviewUiState())
    val state: StateFlow<VocabularyPreviewUiState> = _state.asStateFlow()

    sealed interface Event {
        data class ProceedWithSelection(val words: List<SuggestedVocabulary>) : Event
        data object SkipVocabulary : Event
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    fun setWords(words: List<SuggestedVocabulary>) {
        _state.update {
            it.copy(
                words = words,
                selectedIndices = words.indices.toSet()
            )
        }
    }

    fun proceedWithSelected() {
        val currentState = _state.value
        val selectedWords = currentState.selectedIndices.map { currentState.words[it] }
        viewModelScope.launch {
            _events.emit(Event.ProceedWithSelection(selectedWords))
        }
    }

    fun skip() {
        viewModelScope.launch {
            _events.emit(Event.SkipVocabulary)
        }
    }
}
