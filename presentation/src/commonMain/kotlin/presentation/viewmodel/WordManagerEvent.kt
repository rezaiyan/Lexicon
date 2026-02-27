package presentation.viewmodel

import domain.word.model.LearningStage
import domain.word.model.Word
import presentation.model.WordSortOption
import utils.Language

sealed interface WordManagerEvent {
    data object ResetState : WordManagerEvent
    data class ToggleWordSelection(val wordId: Int) : WordManagerEvent
    data object SelectAll : WordManagerEvent
    data object DeselectAll : WordManagerEvent
    data class UpdateSearchQuery(val query: String) : WordManagerEvent
    data object ClearSearch : WordManagerEvent
    data class SetSortOption(val option: WordSortOption) : WordManagerEvent
    data class SetFilterLanguage(val language: Language?) : WordManagerEvent
    data class SetFilterLearningStage(val stage: LearningStage?) : WordManagerEvent
    data object EnterSelectionMode : WordManagerEvent
    data object ExitSelectionMode : WordManagerEvent
    data class OpenWordDetail(val word: Word) : WordManagerEvent
    data object CloseWordDetail : WordManagerEvent
    data class UpdateWord(val word: Word) : WordManagerEvent
    data object ShowDeleteConfirmation : WordManagerEvent
    data object HideDeleteConfirmation : WordManagerEvent
    data object DeleteSelectedWords : WordManagerEvent
    data object ShowBatchEditLanguages : WordManagerEvent
    data object HideBatchEditLanguages : WordManagerEvent
    data class BatchUpdateLanguages(
        val sourceLanguage: Language,
        val targetLanguage: Language
    ) : WordManagerEvent
    data object ShareWords : WordManagerEvent
}
