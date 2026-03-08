package presentation.ui.components.imports

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import events.OnEvents
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.import_failed_generic
import lexicon.resources.generated.resources.success_imported_words
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import overlay.bottomsheet.BottomSheetPages
import theme.Theme

private enum class ImportPage {
    Main,
    LanguageConfirmation,
    MainSourceLanguage,
    MainTargetLanguage,
    ConfirmSourceLanguage,
    ConfirmTargetLanguage,
}

@Composable
fun ImportBottomSheet(onDismiss: () -> Unit, onShowSnackBar: (String) -> Unit) {
    val viewModel = koinInject<ImportViewModel>()
    val state by viewModel.state()
    var currentPage by remember { mutableStateOf(ImportPage.Main) }
    val errorMessage = stringResource(Res.string.import_failed_generic)
    val successFormat = stringResource(Res.string.success_imported_words)
    val latestErrorMessage = rememberUpdatedState(errorMessage)
    val latestSuccessFormat = rememberUpdatedState(successFormat)

    OnEvents(viewModel.effects) { event ->
        when (event) {
            is ImportEvent.WordAddedSuccessfully -> Unit
            is ImportEvent.FileImportSuccessful -> {
                onShowSnackBar(formatCount(latestSuccessFormat.value, event.count))
                onDismiss()
            }
            is ImportEvent.ImageImportSuccessful -> {
                onShowSnackBar(formatCount(latestSuccessFormat.value, event.count))
                onDismiss()
            }
            is ImportEvent.Error -> {
                val message = if (event.message.isNotEmpty()) "[Error] ${event.message}"
                else latestErrorMessage.value
                onShowSnackBar(message)
                onDismiss()
            }
        }
    }

    LaunchedEffect(state.showLanguageConfirmation) {
        if (state.showLanguageConfirmation) currentPage = ImportPage.LanguageConfirmation
    }

    BottomSheetPages(currentPage, label = "ImportPages") { page ->
        when (page) {
            ImportPage.Main -> ImportMainPage(
                state = state,
                viewModel = viewModel,
                onDismiss = onDismiss,
                onShowSourceLanguage = { currentPage = ImportPage.MainSourceLanguage },
                onShowTargetLanguage = { currentPage = ImportPage.MainTargetLanguage },
            )
            ImportPage.LanguageConfirmation -> ImportLanguageConfirmationContent(
                sourceLanguage = state.targetLanguage,
                targetLanguage = state.sourceLanguage,
                onConfirm = {
                    viewModel.confirmImport()
                    currentPage = ImportPage.Main
                },
                onDismiss = {
                    viewModel.dismissLanguageConfirmation()
                    currentPage = ImportPage.Main
                },
                onShowSourceLanguage = { currentPage = ImportPage.ConfirmSourceLanguage },
                onShowTargetLanguage = { currentPage = ImportPage.ConfirmTargetLanguage },
            )
            ImportPage.MainSourceLanguage -> LanguagePickerPage(
                currentLanguage = state.sourceLanguage,
                onLanguageSelected = { viewModel.selectSourceLanguage(it); currentPage = ImportPage.Main },
                onBack = { currentPage = ImportPage.Main },
            )
            ImportPage.MainTargetLanguage -> LanguagePickerPage(
                currentLanguage = state.targetLanguage,
                onLanguageSelected = { viewModel.selectTargetLanguage(it); currentPage = ImportPage.Main },
                onBack = { currentPage = ImportPage.Main },
            )
            ImportPage.ConfirmSourceLanguage -> LanguagePickerPage(
                currentLanguage = state.targetLanguage,
                onLanguageSelected = {
                    viewModel.selectTargetLanguage(it)
                    currentPage = ImportPage.LanguageConfirmation
                },
                onBack = { currentPage = ImportPage.LanguageConfirmation },
            )
            ImportPage.ConfirmTargetLanguage -> LanguagePickerPage(
                currentLanguage = state.sourceLanguage,
                onLanguageSelected = {
                    viewModel.selectSourceLanguage(it)
                    currentPage = ImportPage.LanguageConfirmation
                },
                onBack = { currentPage = ImportPage.LanguageConfirmation },
            )
        }
    }
}

@Composable
private fun ImportMainPage(
    state: ImportUiState,
    viewModel: ImportViewModel,
    onDismiss: () -> Unit,
    onShowSourceLanguage: () -> Unit,
    onShowTargetLanguage: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(Theme.spacing.lg)
            .imePadding()
    ) {

        ImportTabSelector(
            modifier = Modifier.padding(vertical = Theme.spacing.xl),
            tabs = state.tabs,
            selectedTab = state.selectedTab,
            onTabSelected = viewModel::selectTab,
        )

        ImportTabContent(
            modifier = Modifier
                .padding(top = Theme.spacing.lg)
                .weight(1f, fill = false),
            state = state,
            viewModel = viewModel,
            onDismiss = onDismiss,
            onShowSourceLanguage = onShowSourceLanguage,
            onShowTargetLanguage = onShowTargetLanguage,
        )
    }
}

private fun formatCount(pattern: String, count: Int): String {
    val placeholder = "%1" + '$' + "d"
    return pattern.replace(placeholder, count.toString())
}
