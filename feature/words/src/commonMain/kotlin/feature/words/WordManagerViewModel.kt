package feature.words

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import domain.auth.usecase.GetFeatureAccessUseCase
import domain.tag.usecase.GetTagsUseCase
import domain.word.model.LearningStage
import domain.word.model.Word
import core.common.fold
import domain.tag.usecase.BatchAssignTagsParams
import domain.tag.usecase.BatchAssignTagsUseCase
import domain.word.usecase.BatchUpdateLanguagesUseCase
import domain.word.usecase.DeleteWordsUseCase
import domain.word.usecase.ExportWordsUseCase
import domain.word.usecase.GetAllWordsUseCase
import domain.word.usecase.UpdateWordUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import core.base.BaseViewModel
import feature.words.model.WordManagerEffect
import feature.words.model.WordManagerScreenState
import feature.words.model.WordSortOption
import utils.Language
class WordManagerViewModel(
    private val getAllWordsUseCase: GetAllWordsUseCase,
    private val getTagsUseCase: GetTagsUseCase,
    deleteWordsUseCase: DeleteWordsUseCase,
    batchUpdateLanguagesUseCase: BatchUpdateLanguagesUseCase,
    private val batchAssignTagsUseCase: BatchAssignTagsUseCase,
    updateWordUseCase: UpdateWordUseCase,
    private val exportWordsUseCase: ExportWordsUseCase,
    private val getFeatureAccessUseCase: GetFeatureAccessUseCase,
    analyticsTracker: IAnalyticsTracker
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
        viewModelScope.launch {
            getFeatureAccessUseCase()
                .catch {
                    it.printStackTrace()
                }
                .collect { featureAccess ->
                    updateState { copy(isUserSubscribed = featureAccess.userAccess.hasPremiumAccess) }
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
                    updateState {
                        copy(
                            isLoading = false,
                            errorMessage = it.message ?: "Failed to load words"
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

    fun updateSearchQuery(query: String) {
        updateState { copy(searchQuery = query) }
    }

    fun clearSearch() {
        updateState { copy(searchQuery = "") }
    }

    fun setSortOption(option: WordSortOption) {
        updateState { copy(sortOption = option) }
    }

    fun setFilterLanguage(language: Language?) {
        updateState { copy(filterLanguage = language) }
    }

    fun setFilterLearningStage(stage: LearningStage?) {
        updateState { copy(filterLearningStage = stage) }
    }

    fun setFilterTagId(tagId: Long?) {
        updateState { copy(filterTagId = tagId) }
    }

    fun enterSelectionMode() {
        updateState { copy(isSelectionMode = true) }
    }

    fun exitSelectionMode() {
        updateState { copy(isSelectionMode = false, selectedWordIds = emptySet()) }
    }

    fun updateWord(word: Word) {
        editingHandler.updateWord(word)
    }

    fun deleteSelectedWords() {
        val selectedIds = currentState.selectedWordIds.toList()
        viewModelScope.launch {
            deletionHandler.deleteSelectedWords(selectedIds)
        }
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
                        emitEffect(WordManagerEffect.Error(error.message ?: "Failed to update tags"))
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
}
