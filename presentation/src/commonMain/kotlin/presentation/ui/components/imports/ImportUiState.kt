package presentation.ui.components.imports

import androidx.compose.runtime.Stable
import presentation.model.ImageImportState
import utils.Language

data class ImportUiState(
    private val defaultTab: ImportTabV2 = ImportTabV2.Text(),
    val tabs: List<ImportTabV2> = listOf(defaultTab, ImportTabV2.File()),
    val selectedTab: ImportTabV2 = tabs.first(),
    val textInputState: TextInputState = TextInputState(),
    val fileImportState: ImportFileState = ImportFileState.Idle,
    val imageImportState: ImageImportState = ImageImportState.Idle,
    val sourceLanguage: Language = Language.ENGLISH,
    val targetLanguage: Language = Language.ENGLISH,
    val showLanguageConfirmation: Boolean = false,
    val pendingImportAction: PendingImportAction? = null,
)

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
