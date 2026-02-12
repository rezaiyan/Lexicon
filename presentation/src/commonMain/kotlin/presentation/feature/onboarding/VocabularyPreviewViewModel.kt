package presentation.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.usecase.ImportSuggestedVocabularyUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.model.VocabularyPreviewUiState

class VocabularyPreviewViewModel(
    private val importSuggestedVocabularyUseCase: ImportSuggestedVocabularyUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(VocabularyPreviewUiState())
    val state: StateFlow<VocabularyPreviewUiState> = _state.asStateFlow()

    sealed interface Event {
        data object NavigateToMain : Event
    }

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    fun setWords(words: List<SuggestedVocabulary>) {
        _state.update {
            it.copy(
                words = words,
                selectedIndices = words.indices.toSet()
            )
        }
    }

    fun toggleWord(index: Int) {
        _state.update { current ->
            val newIndices = current.selectedIndices.toMutableSet()
            if (newIndices.contains(index)) {
                newIndices.remove(index)
            } else {
                newIndices.add(index)
            }
            current.copy(selectedIndices = newIndices)
        }
    }

    fun selectAll() {
        _state.update { current ->
            current.copy(selectedIndices = current.words.indices.toSet())
        }
    }

    fun deselectAll() {
        _state.update { it.copy(selectedIndices = emptySet()) }
    }

    fun importSelected() {
        val currentState = _state.value
        val selectedWords = currentState.selectedIndices.map { currentState.words[it] }
        if (selectedWords.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isImporting = true, error = null) }
            importSuggestedVocabularyUseCase(selectedWords)
                .onSuccess {
                    _state.update { it.copy(isImporting = false) }
                    _events.emit(Event.NavigateToMain)
                }
                .onFailure { error ->
                    _state.update { it.copy(isImporting = false, error = error.message) }
                }
        }
    }

    fun skip() {
        viewModelScope.launch {
            _events.emit(Event.NavigateToMain)
        }
    }
}
