package presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.translation_language
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.imports.ImportLanguageListPage
import utils.Language

/** Single-language picker sheet: searchable list, current choice pinned first; tapping a row applies it. */
@Composable
fun LanguageSelectionContent(
    currentLanguage: Language,
    onLanguageSelected: (Language) -> Unit,
    title: String = stringResource(Res.string.translation_language),
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
) {
    val languages = remember(currentLanguage) {
        listOf(currentLanguage) + Language.entries.filter { it != currentLanguage }
    }
    ImportLanguageListPage(
        title = title,
        languages = languages,
        selected = currentLanguage,
        onLanguageSelected = onLanguageSelected,
        onBack = onBack,
        onClose = onClose,
    )
}
