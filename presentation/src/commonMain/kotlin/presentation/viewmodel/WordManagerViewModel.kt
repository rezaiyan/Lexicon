@file:OptIn(ExperimentalTime::class)

package presentation.viewmodel

import analytics.IAnalyticsTracker
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.auth.usecase.GetFeatureAccessUseCase
import domain.word.model.LearningStage
import domain.word.model.Word
import domain.word.usecase.BatchUpdateLanguagesUseCase
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
import presentation.model.WordManagerEffect
import presentation.model.WordManagerScreenState
import presentation.model.WordSortOption
import utils.Language
import kotlin.time.ExperimentalTime

class WordManagerViewModel(
    private val getAllWordsUseCase: GetAllWordsUseCase,
    private val deleteWordsUseCase: DeleteWordsUseCase,
    private val batchUpdateLanguagesUseCase: BatchUpdateLanguagesUseCase,
    private val updateWordUseCase: UpdateWordUseCase,
    private val exportWordsUseCase: ExportWordsUseCase,
    private val getFeatureAccessUseCase: GetFeatureAccessUseCase,
    private val analyticsTracker: IAnalyticsTracker
) : ViewModel() {

    private val _state = MutableStateFlow(WordManagerScreenState())
    val state = _state.asStateFlow()

    private val _events = Channel<WordManagerEffect>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val deletionHandler = WordDeletionHandler(
        deleteWordsUseCase = deleteWordsUseCase,
        analyticsTracker = analyticsTracker,
        state = _state,
        events = _events,
        scope = viewModelScope
    )

    private val batchEditHandler = WordBatchEditHandler(
        batchUpdateLanguagesUseCase = batchUpdateLanguagesUseCase,
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
            getFeatureAccessUseCase()
                .catch {
                    it.printStackTrace()
                }
                .collect { featureAccess ->
                    _state.value = _state.value.copy(isUserSubscribed = featureAccess.userAccess.hasPremiumAccess)
                }
        }
    }

    fun resetState() {
        _state.value = _state.value.copy(
            selectedWordIds = emptySet(),
            isSelectionMode = false,
            searchQuery = "",
            detailWord = null,
            showDeleteConfirmation = false,
            showBatchEditLanguages = false,
            isDeletingWords = false,
            isBatchUpdatingLanguages = false,
            errorMessage = null
        )
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
                        words = words,
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
        _state.value = _state.value.copy(
            selectedWordIds = newSelection,
            isSelectionMode = newSelection.isNotEmpty()
        )
    }

    fun selectAll() {
        val allWordIds: Set<Int> = _state.value.filteredWords.map { it.id }.toSet()
        _state.value = _state.value.copy(
            selectedWordIds = allWordIds,
            isSelectionMode = allWordIds.isNotEmpty()
        )
    }

    fun deselectAll() {
        _state.value = _state.value.copy(
            selectedWordIds = emptySet(),
            isSelectionMode = false
        )
    }

    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun clearSearch() {
        _state.value = _state.value.copy(searchQuery = "")
    }

    fun setSortOption(option: WordSortOption) {
        _state.value = _state.value.copy(sortOption = option)
    }

    fun setFilterLanguage(language: Language?) {
        _state.value = _state.value.copy(filterLanguage = language)
    }

    fun setFilterLearningStage(stage: LearningStage?) {
        _state.value = _state.value.copy(filterLearningStage = stage)
    }

    fun enterSelectionMode() {
        _state.value = _state.value.copy(isSelectionMode = true)
    }

    fun exitSelectionMode() {
        _state.value = _state.value.copy(
            isSelectionMode = false,
            selectedWordIds = emptySet()
        )
    }

    fun openWordDetail(word: Word) {
        editingHandler.startEditing(word)
    }

    fun closeWordDetail() {
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

    fun showBatchEditLanguages() {
        _state.value = _state.value.copy(showBatchEditLanguages = true)
    }

    fun hideBatchEditLanguages() {
        _state.value = _state.value.copy(showBatchEditLanguages = false)
    }

    fun batchUpdateLanguages(sourceLanguage: Language, targetLanguage: Language) {
        val selectedIds = _state.value.selectedWordIds.toList()
        batchEditHandler.batchUpdateLanguages(selectedIds, sourceLanguage, targetLanguage)
    }

    fun shareWords() {
        exportHandler.shareWords(
            words = _state.value.words,
            selectedWordIds = _state.value.selectedWordIds
        )
    }

    fun onEvent(event: WordManagerEvent) {
        when (event) {
            is WordManagerEvent.ResetState -> resetState()
            is WordManagerEvent.ToggleWordSelection -> toggleWordSelection(event.wordId)
            is WordManagerEvent.SelectAll -> selectAll()
            is WordManagerEvent.DeselectAll -> deselectAll()
            is WordManagerEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
            is WordManagerEvent.ClearSearch -> clearSearch()
            is WordManagerEvent.SetSortOption -> setSortOption(event.option)
            is WordManagerEvent.SetFilterLanguage -> setFilterLanguage(event.language)
            is WordManagerEvent.SetFilterLearningStage -> setFilterLearningStage(event.stage)
            is WordManagerEvent.EnterSelectionMode -> enterSelectionMode()
            is WordManagerEvent.ExitSelectionMode -> exitSelectionMode()
            is WordManagerEvent.OpenWordDetail -> openWordDetail(event.word)
            is WordManagerEvent.CloseWordDetail -> closeWordDetail()
            is WordManagerEvent.UpdateWord -> updateWord(event.word)
            is WordManagerEvent.ShowDeleteConfirmation -> showDeleteConfirmation()
            is WordManagerEvent.HideDeleteConfirmation -> hideDeleteConfirmation()
            is WordManagerEvent.DeleteSelectedWords -> deleteSelectedWords()
            is WordManagerEvent.ShowBatchEditLanguages -> showBatchEditLanguages()
            is WordManagerEvent.HideBatchEditLanguages -> hideBatchEditLanguages()
            is WordManagerEvent.BatchUpdateLanguages -> batchUpdateLanguages(
                event.sourceLanguage, event.targetLanguage
            )
            is WordManagerEvent.ShareWords -> shareWords()
        }
    }
}
