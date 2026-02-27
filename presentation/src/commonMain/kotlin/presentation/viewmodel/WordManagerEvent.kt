package presentation.viewmodel

import domain.word.model.Word
import utils.Language

sealed interface WordManagerEvent {
    data object ResetState : WordManagerEvent
    data class ToggleWordSelection(val wordId: Int) : WordManagerEvent
    data object SelectAll : WordManagerEvent
    data object DeselectAll : WordManagerEvent
    data class UpdateSearchQuery(val query: String) : WordManagerEvent
    data object ClearSearch : WordManagerEvent
    data class StartEditingWord(val word: Word) : WordManagerEvent
    data object CancelEditing : WordManagerEvent
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
