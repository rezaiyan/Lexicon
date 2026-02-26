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
    data class Text(val text: String) : PendingImportAction()
    data class File(val content: String, val fileName: String?) : PendingImportAction()
}

@Stable
data class TextInputState(
    val text: String = "",
    val isEnabled: Boolean = true,
) {
    val isImportEnabled: Boolean
        get() = text.isNotBlank() && isEnabled
}

enum class ExtractionOption { Word, Sentence }