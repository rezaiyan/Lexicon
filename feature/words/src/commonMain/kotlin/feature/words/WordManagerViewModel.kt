package feature.words

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import domain.tag.usecase.GetTagsUseCase
import domain.word.model.LearningStage
import domain.word.model.Word
import domain.word.model.WordSortOption
import domain.word.usecase.ClassifyImportErrorUseCase
import domain.word.usecase.FilterAndSortWordsUseCase
import core.common.fold
import core.error.toUserMessage
import domain.tag.usecase.BatchAssignTagsParams
import domain.tag.usecase.BatchAssignTagsUseCase
import domain.word.usecase.BatchUpdateLanguagesUseCase
import domain.word.usecase.DeleteWordsUseCase
import domain.word.usecase.ExportWordsUseCase
import domain.word.usecase.GetAllWordsUseCase
import domain.word.usecase.UpdateWordUseCase
import domain.focus.model.LearningFocus
import domain.focus.usecase.ObserveLearningFocusUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import core.base.BaseViewModel
import feature.words.model.DragSelection
import feature.words.model.WordManagerEffect
import feature.words.model.WordManagerScreenState
import utils.Language

@Suppress("LongParameterList")
class WordManagerViewModel(
    private val getAllWordsUseCase: GetAllWordsUseCase,
    private val getTagsUseCase: GetTagsUseCase,
    deleteWordsUseCase: DeleteWordsUseCase,
    batchUpdateLanguagesUseCase: BatchUpdateLanguagesUseCase,
    private val batchAssignTagsUseCase: BatchAssignTagsUseCase,
    updateWordUseCase: UpdateWordUseCase,
    private val exportWordsUseCase: ExportWordsUseCase,
    private val filterAndSortWordsUseCase: FilterAndSortWordsUseCase,
    private val classifyImportErrorUseCase: ClassifyImportErrorUseCase,
    analyticsTracker: IAnalyticsTracker,
    private val observeLearningFocus: ObserveLearningFocusUseCase,
) : BaseViewModel<WordManagerScreenState, WordManagerEffect>() {

    override fun initialState() = WordManagerScreenState()

    private val deletionHandler = WordDeletionHandler(
        deleteWordsUseCase = deleteWordsUseCase,
        analyticsTracker = analyticsTracker,
        stateAccess = stateAccess,
        events = effectsSendChannel,
        scope = viewModelScope
    )

    private val batchEditHandler = WordBatchEditHandler(
        batchUpdateLanguagesUseCase = batchUpdateLanguagesUseCase,
        analyticsTracker = analyticsTracker,
        stateAccess = stateAccess,
        events = effectsSendChannel,
        scope = viewModelScope
    )

    private val exportHandler = WordExportHandler(
        exportWordsUseCase = exportWordsUseCase,
        analyticsTracker = analyticsTracker,
        events = effectsSendChannel,
        scope = viewModelScope
    )

    private val editingHandler = WordEditingHandler(
        updateWordUseCase = updateWordUseCase,
        analyticsTracker = analyticsTracker,
        events = effectsSendChannel,
        scope = viewModelScope
    )

    init {
        startObservingWords()
        startObservingTags()
        applyFocusAsDefaultFilter()
    }

    /**
     * Opens on the user's focus language. Only a default: `Single` implies 2+ languages,
     * and later filter changes here never touch the study focus.
     */
    private fun applyFocusAsDefaultFilter() {
        viewModelScope.launch {
            val focus = observeLearningFocus().first()
            if (focus is LearningFocus.Single && currentState.filterLanguage == null) {
                setFilterLanguage(focus.language)
            }
        }
    }

    fun resetState() {
        updateState {
            copy(
                selectedWordIds = emptySet(),
                isSelectionMode = false,
                searchQuery = "",
                isDeletingWords = false,
                isBatchUpdatingLanguages = false,
                isBatchAssigningTags = false,
                errorMessage = null
            )
        }
        recomputeFilteredWords()
    }

    private fun startObservingTags() {
        viewModelScope.launch {
            getTagsUseCase()
                .catch { it.printStackTrace() }
                .collect { tags -> updateState { copy(tags = tags) } }
        }
    }

    private fun startObservingWords() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, errorMessage = null) }

            getAllWordsUseCase()
                .catch {
                    val message = it.toUserMessage()
                    updateState {
                        copy(
                            isLoading = false,
                            errorMessage = message,
                            errorClassification = classifyImportErrorUseCase(message),
                        )
                    }
                }
                .collect { words ->
                    updateState {
                        copy(
                            words = words,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    recomputeFilteredWords()
                }
        }
    }

    fun toggleWordSelection(wordId: Int) {
        updateState {
            val newSelection = if (selectedWordIds.contains(wordId)) {
                selectedWordIds - wordId
            } else {
                selectedWordIds + wordId
            }
            copy(
                selectedWordIds = newSelection,
                isSelectionMode = newSelection.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        updateState {
            val allWordIds: Set<Int> = filteredWords.map { it.id }.toSet()
            if (selectedWordIds.containsAll(allWordIds)) {
                copy(
                    selectedWordIds = emptySet(),
                    isSelectionMode = true
                )
            } else {
                copy(
                    selectedWordIds = allWordIds,
                    isSelectionMode = allWordIds.isNotEmpty()
                )
            }
        }
    }

    /** Long press landed on row [index] of the visible list: begins a drag selection there. */
    fun startDragSelection(index: Int) {
        val word = currentState.filteredWords.getOrNull(index) ?: return
        updateState {
            val drag = DragSelection(
                anchorIndex = index,
                baseSelection = selectedWordIds,
                selecting = word.id !in selectedWordIds,
            )
            copy(
                dragSelection = drag,
                selectedWordIds = drag.selectionAt(filteredWords.map { it.id }, index),
                isSelectionMode = true,
            )
        }
    }

    /** Finger is over row [index]; selection becomes the base plus / minus the anchor..index range. */
    fun dragSelectionTo(index: Int) {
        updateState {
            val drag = dragSelection ?: return@updateState this
            copy(selectedWordIds = drag.selectionAt(filteredWords.map { it.id }, index))
        }
    }

    fun endDragSelection() {
        updateState { copy(dragSelection = null, isSelectionMode = selectedWordIds.isNotEmpty()) }
    }

    fun updateSearchQuery(query: String) {
        updateState { copy(searchQuery = query) }
        recomputeFilteredWords()
    }

    fun clearSearch() {
        updateState { copy(searchQuery = "") }
        recomputeFilteredWords()
    }

    fun setSortOption(option: WordSortOption) {
        updateState { copy(sortOption = option) }
        recomputeFilteredWords()
    }

    fun setFilterLanguage(language: Language?) {
        updateState { copy(filterLanguage = language) }
        recomputeFilteredWords()
    }

    fun setFilterLearningStage(stage: LearningStage?) {
        updateState { copy(filterLearningStage = stage) }
        recomputeFilteredWords()
    }

    fun setFilterTagId(tagId: Long?) {
        updateState { copy(filterTagId = tagId) }
        recomputeFilteredWords()
    }

    fun clearFilters() {
        updateState {
            copy(searchQuery = "", filterLanguage = null, filterLearningStage = null, filterTagId = null)
        }
        recomputeFilteredWords()
    }

    fun enterSelectionMode() {
        updateState { copy(isSelectionMode = true) }
    }

    fun exitSelectionMode() {
        updateState { copy(isSelectionMode = false, selectedWordIds = emptySet(), dragSelection = null) }
    }

    fun updateWord(word: Word) {
        editingHandler.updateWord(word)
    }

    fun deleteSelectedWords() {
        deletionHandler.deleteSelectedWords(currentState.selectedWordIds.toList())
    }

    /** Deletes one word from its detail sheet, without touching the list selection. */
    fun deleteWord(wordId: Int) {
        deletionHandler.deleteSelectedWords(listOf(wordId))
    }

    fun batchUpdateLanguages(sourceLanguage: Language, targetLanguage: Language) {
        val selectedIds = currentState.selectedWordIds.toList()
        batchEditHandler.batchUpdateLanguages(selectedIds, sourceLanguage, targetLanguage)
    }

    fun batchAssignTags(tagIds: List<Long>) {
        val selectedIds = currentState.selectedWordIds.toList()
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            updateState { copy(isBatchAssigningTags = true) }
            batchAssignTagsUseCase(BatchAssignTagsParams(selectedIds, tagIds))
                .fold(
                    onSuccess = { count ->
                        updateState {
                            copy(
                                isBatchAssigningTags = false,
                                selectedWordIds = emptySet(),
                                isSelectionMode = false
                            )
                        }
                        emitEffect(WordManagerEffect.WordsTagged(count))
                    },
                    onFailure = { error ->
                        updateState { copy(isBatchAssigningTags = false) }
                        emitEffect(WordManagerEffect.Error(error.toUserMessage()))
                    }
                )
        }
    }

    fun shareWords() {
        exportHandler.shareWords(
            words = currentState.words,
            selectedWordIds = currentState.selectedWordIds
        )
    }

    private fun recomputeFilteredWords() {
        val state = currentState
        val filtered = filterAndSortWordsUseCase(
            FilterAndSortWordsUseCase.Params(
                words = state.words,
                query = state.searchQuery,
                filterLanguage = state.filterLanguage,
                filterStage = state.filterLearningStage,
                filterTagId = state.filterTagId,
                sortOption = state.sortOption,
            )
        )
        updateState { copy(filteredWords = filtered) }
    }
}
