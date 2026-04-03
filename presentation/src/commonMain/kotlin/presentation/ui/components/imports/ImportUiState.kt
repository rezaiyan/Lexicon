package presentation.ui.components.imports

import androidx.compose.runtime.Stable
import domain.tag.model.Tag
import presentation.model.ImageImportState
import utils.Language

data class ImportUiState(
    val tabs: List<ImportTabV2> = listOf(ImportTabV2.Text(), ImportTabV2.File()),
    val selectedTab: ImportTabV2 = tabs.first(),
    val textInputState: TextInputState = TextInputState(),
    val fileImportState: ImportFileState = ImportFileState.Idle,
    val imageImportState: ImageImportState = ImageImportState.Idle,
    val imageReviewState: ImageReviewState = ImageReviewState.None,
    val sourceLanguage: Language = Language.ENGLISH,
    val targetLanguage: Language = Language.ENGLISH,
    val showLanguageConfirmation: Boolean = false,
    val pendingImportAction: PendingImportAction? = null,
    val tags: List<Tag> = emptyList(),
    val selectedTagId: Long? = null,
    val showCreateTagDialog: Boolean = false,
)

data class ExtractedWordItem(
    val id: Int,
    val word: String,
    val translation: String,
    val description: String = "",
)

sealed class ImageReviewState {
    data object None : ImageReviewState()
    data class Review(
        val words: List<ExtractedWordItem>,
        val editingWordId: Int? = null,
        val showCancelConfirmation: Boolean = false,
        val isImporting: Boolean = false,
    ) : ImageReviewState()
}

sealed class PendingImportAction {
    data class File(val content: String, val fileName: String?) : PendingImportAction()
}

@Stable
data class TextInputState(
    val word: String = "",
    val translation: String = "",
    val description: String = "",
    val isEnabled: Boolean = true,
    val wordsAddedCount: Int = 0,
    val showSuccessIndicator: Boolean = false,
    val errorMessage: String? = null,
) {
    val isAddEnabled: Boolean
        get() = word.isNotBlank() && translation.isNotBlank() && isEnabled
}
