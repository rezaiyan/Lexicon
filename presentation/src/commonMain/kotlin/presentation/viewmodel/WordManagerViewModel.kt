@file:OptIn(ExperimentalTime::class)

package presentation.viewmodel

import analytics.IAnalyticsTracker
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.subscription.ISubscriptionManager
import domain.word.model.Word
import domain.word.usecase.DeleteWordsUseCase
import domain.word.usecase.ExportWordsUseCase
import domain.word.usecase.GetAllWordsUseCase
import domain.word.usecase.UpdateWordUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import presentation.model.WordManagerEvent
import presentation.model.WordManagerScreenState
import kotlin.time.ExperimentalTime

class WordManagerViewModel(
    private val getAllWordsUseCase: GetAllWordsUseCase,
    private val deleteWordsUseCase: DeleteWordsUseCase,
    private val updateWordUseCase: UpdateWordUseCase,
    private val exportWordsUseCase: ExportWordsUseCase,
    private val subscriptionManager: ISubscriptionManager,
    private val analyticsTracker: IAnalyticsTracker
) : ViewModel() {

    private val _state = MutableStateFlow(WordManagerScreenState())
    val state = _state.asStateFlow()

    private val _events = Channel<WordManagerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val deletionHandler = WordDeletionHandler(
        deleteWordsUseCase = deleteWordsUseCase,
        analyticsTracker = analyticsTracker,
        state = _state,
        events = _events,
        scope = viewModelScope
    )

    private val exportHandler = WordExportHandler(
        exportWordsUseCase = exportWordsUseCase,
        analyticsTracker = analyticsTracker,
        events = _events,
        scope = viewModelScope
    )

    private val editingHandler = WordEditingHandler(
        updateWordUseCase = updateWordUseCase,
        analyticsTracker = analyticsTracker,
        state = _state,
        events = _events,
        scope = viewModelScope
    )

    init {
        startObservingWords()
        viewModelScope.launch {
            subscriptionManager.isSubscribed()
                .catch {
                    it.printStackTrace()
                }
                .collect { isSubscribed ->
                    _state.value = _state.value.copy(isUserSubscribed = isSubscribed)
                }
        }
    }

    /**
     * Reset state when screen is opened
     * Clears selections, search query, editing state, and dialogs
     */
    fun resetState() {
        _state.value = _state.value.copy(
            selectedWordIds = emptySet(),
            searchQuery = "",
            editingWord = null,
            showDeleteConfirmation = false,
            isDeletingWords = false,
            errorMessage = null,
            snackbarMessage = null
        )
        println("🔄 [WordManager] State reset - fresh screen")
    }

    private fun startObservingWords() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            getAllWordsUseCase()
                .catch {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = it.message ?: "Failed to load words"
                    )
                }
                .collect { words ->
                    _state.value = _state.value.copy(
                        words = words.sortedWith(
                            compareByDescending<Word> { it.dateAdded }
                                .thenBy { it.level }
                        ),
                        isLoading = false,
                        errorMessage = null
                    )
                }
        }
    }

    fun toggleWordSelection(wordId: Int) {
        val currentSelection = _state.value.selectedWordIds
        val newSelection = if (currentSelection.contains(wordId)) {
            currentSelection - wordId
        } else {
            currentSelection + wordId
        }
        _state.value = _state.value.copy(selectedWordIds = newSelection)
    }

    fun selectAll() {
        val allWordIds: Set<Int> = _state.value.filteredWords.map { it.id }.toSet()
        _state.value = _state.value.copy(selectedWordIds = allWordIds)
    }

    fun deselectAll() {
        _state.value = _state.value.copy(selectedWordIds = emptySet())
    }

    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun clearSearch() {
        _state.value = _state.value.copy(searchQuery = "")
    }


    fun startEditingWord(word: Word) {
        editingHandler.startEditing(word)
    }

    fun cancelEditing() {
        editingHandler.cancelEditing()
    }

    fun updateWord(word: Word) {
        editingHandler.updateWord(word)
    }


    fun showDeleteConfirmation() {
        _state.value = _state.value.copy(showDeleteConfirmation = true)
    }

    fun hideDeleteConfirmation() {
        _state.value = _state.value.copy(showDeleteConfirmation = false)
    }

    fun deleteSelectedWords() {
        val selectedIds = _state.value.selectedWordIds.toList()
        viewModelScope.launch {
            deletionHandler.deleteSelectedWords(selectedIds)
        }
    }


    fun shareWords() {
        try {
            exportHandler.shareWords(
                words = _state.value.words,
                selectedWordIds = _state.value.selectedWordIds
            )
        } catch (e: Exception) {
            val errorMsg = e.message ?: ""
            _events.trySend(WordManagerEvent.Error(errorMsg))
        }
    }
}


