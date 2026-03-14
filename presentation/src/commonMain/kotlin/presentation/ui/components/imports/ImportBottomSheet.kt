package presentation.ui.components.imports

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import events.OnEvents
import feature.onboarding.ui.components.LanguageGrid
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.import_failed_generic
import lexicon.resources.generated.resources.original_language
import lexicon.resources.generated.resources.original_language_question
import lexicon.resources.generated.resources.processing_image_with_ai
import lexicon.resources.generated.resources.success_imported_words
import lexicon.resources.generated.resources.translation_language_hint
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import presentation.model.ImageImportState
import theme.Theme
import utils.Language
import utils.rememberCameraLauncher
import utils.rememberImagePickerLauncher

private sealed interface ImportPage {
    data object MethodChooser : ImportPage
    data object SourceLanguageChooser : ImportPage
    data object TextContent : ImportPage
    data object FileContent : ImportPage
    data object ImageContent : ImportPage
    data object LanguageConfirmation : ImportPage
    data object SourceLanguagePicker : ImportPage
    data object TargetLanguagePicker : ImportPage
}

@Composable
fun ImportBottomSheet(
    onDismiss: () -> Unit,
    onShowSnackBar: (String) -> Unit,
    onClose: (() -> Unit)? = null,
) {
    val viewModel = koinInject<ImportViewModel>()
    val state by viewModel.state()
    val pages = rememberBottomSheetPageNavigator<ImportPage>(ImportPage.MethodChooser)
    val errorMessage = stringResource(Res.string.import_failed_generic)
    val successFormat = stringResource(Res.string.success_imported_words)
    val latestErrorMessage = rememberUpdatedState(errorMessage)
    val latestSuccessFormat = rememberUpdatedState(successFormat)

    OnEvents(viewModel.effects) { effect ->
        when (effect) {
            is ImportEffect.WordAddedSuccessfully -> Unit
            is ImportEffect.FileImportSuccessful -> {
                onShowSnackBar(formatCount(latestSuccessFormat.value, effect.count))
                onDismiss()
            }
            is ImportEffect.ImageImportSuccessful -> {
                onShowSnackBar(formatCount(latestSuccessFormat.value, effect.count))
                onDismiss()
            }
            is ImportEffect.Error -> {
                val message = if (effect.message.isNotEmpty()) "[Error] ${effect.message}"
                else latestErrorMessage.value
                onShowSnackBar(message)
                onDismiss()
            }
        }
    }

    LaunchedEffect(state.showLanguageConfirmation) {
        if (state.showLanguageConfirmation) pages.navigateTo(ImportPage.LanguageConfirmation)
    }

    // Clean up language confirmation state when navigating away
    LaunchedEffect(pages.currentPage) {
        if (pages.currentPage !is ImportPage.LanguageConfirmation && state.showLanguageConfirmation) {
            viewModel.dismissLanguageConfirmation()
        }
    }

    BottomSheetPages(
        navigator = pages,
        onClose = onClose,
        label = "ImportPages",
    ) { page ->
        when (page) {
            is ImportPage.MethodChooser -> ImportMethodChooserContent(
                hasImageAccess = state.tabs.any { it is ImportTabV2.Image },
                onTextSelected = {
                    viewModel.selectTab(ImportTabV2.Text())
                    pages.navigateTo(ImportPage.SourceLanguageChooser)
                },
                onFileSelected = {
                    viewModel.selectTab(ImportTabV2.File())
                    pages.navigateTo(ImportPage.SourceLanguageChooser)
                },
                onImageSelected = {
                    viewModel.selectTab(ImportTabV2.Image())
                    pages.navigateTo(ImportPage.SourceLanguageChooser)
                },
            )

            is ImportPage.SourceLanguageChooser -> SourceLanguageChooserPage(
                targetLanguage = state.targetLanguage,
                onLanguageSelected = { language ->
                    viewModel.selectSourceLanguage(language)
                    when (state.selectedTab) {
                        is ImportTabV2.File -> pages.navigateTo(ImportPage.FileContent)
                        is ImportTabV2.Image -> pages.navigateTo(ImportPage.ImageContent)
                        else -> pages.navigateTo(ImportPage.TextContent)
                    }
                },
            )

            is ImportPage.TextContent -> TextContentPage(
                state = state,
                viewModel = viewModel,
            )

            is ImportPage.FileContent -> FileContentPage(
                state = state,
                viewModel = viewModel,
                onDismiss = onDismiss,
            )

            is ImportPage.ImageContent -> ImageContentPage(
                state = state,
                viewModel = viewModel,
                onDismiss = onDismiss,
            )

            is ImportPage.LanguageConfirmation -> ImportLanguageConfirmationContent(
                sourceLanguage = state.targetLanguage,
                targetLanguage = state.sourceLanguage,
                onConfirm = {
                    viewModel.confirmImport()
                    pages.navigateBack()
                },
                onDismiss = {
                    viewModel.dismissLanguageConfirmation()
                    pages.navigateBack()
                },
                onShowSourceLanguage = { pages.navigateTo(ImportPage.SourceLanguagePicker) },
                onShowTargetLanguage = { pages.navigateTo(ImportPage.TargetLanguagePicker) },
            )

            is ImportPage.SourceLanguagePicker -> LanguagePickerPage(
                currentLanguage = state.sourceLanguage,
                onLanguageSelected = { viewModel.selectSourceLanguage(it); pages.navigateBack() },
                title = stringResource(Res.string.original_language),
            )

            is ImportPage.TargetLanguagePicker -> LanguagePickerPage(
                currentLanguage = state.targetLanguage,
                onLanguageSelected = { viewModel.selectTargetLanguage(it); pages.navigateBack() },
            )
        }
    }
}

@Composable
private fun TextContentPage(
    state: ImportUiState,
    viewModel: ImportViewModel,
) {
    Column(
        modifier = Modifier
            .padding(Theme.spacing.lg)
            .imePadding()
    ) {
        TextImportContent(
            textInputState = state.textInputState,
            onWordChange = viewModel::updateWord,
            onTranslationChange = viewModel::updateTranslation,
            onDescriptionChange = viewModel::updateDescription,
            onAddWord = viewModel::addWord,
        )
    }
}

@Composable
private fun FileContentPage(
    state: ImportUiState,
    viewModel: ImportViewModel,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(Theme.spacing.lg)
            .imePadding()
    ) {
        FileImportContent(
            isEnabled = state.fileImportState !is ImportFileState.Loading,
            isLoading = state.fileImportState is ImportFileState.Loading,
            importFile = viewModel::importFile,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun ImageContentPage(
    state: ImportUiState,
    viewModel: ImportViewModel,
    onDismiss: () -> Unit,
) {
    val isImageLoading = state.imageImportState is ImageImportState.Loading
    val imageTab = state.tabs.filterIsInstance<ImportTabV2.Image>().firstOrNull()
        ?: return

    val imagePickerLauncher = rememberImagePickerLauncher { bytes ->
        if (bytes != null) viewModel.selectImage(bytes)
    }
    val cameraLauncher = rememberCameraLauncher { bytes ->
        if (bytes != null) viewModel.selectImage(bytes)
    }

    Column(
        modifier = Modifier
            .padding(Theme.spacing.lg)
            .imePadding()
    ) {
        Box {
            ImageImportContent(
                imageTab = imageTab,
                isEnabled = !isImageLoading && state.fileImportState !is ImportFileState.Loading,
                onCameraClick = cameraLauncher,
                onGalleryClick = imagePickerLauncher,
                onImportImage = viewModel::importImage,
                onClearSelectedImage = viewModel::clearSelectedImage,
                onDismiss = onDismiss,
            )

            if (isImageLoading) {
                ImageProcessingOverlay()
            }
        }
    }
}

@Composable
private fun SourceLanguageChooserPage(
    targetLanguage: Language,
    onLanguageSelected: (Language) -> Unit,
) {
    val allLanguages = Language.entries.map { it.displayName }

    Column(modifier = Modifier.padding(horizontal = Theme.spacing.xl)) {
        Text(
            text = stringResource(Res.string.original_language_question),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(Theme.spacing.xs))

        Text(
            text = stringResource(Res.string.translation_language_hint, targetLanguage.nativeName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Theme.spacing.lg))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            LanguageGrid(
                languages = allLanguages,
                selectedLanguage = null,
                onLanguageSelected = { displayName ->
                    Language.entries.find { it.displayName == displayName }
                        ?.let(onLanguageSelected)
                },
            )

            Spacer(Modifier.height(Theme.spacing.lg))
        }
    }
}

@Composable
private fun ImageProcessingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(Theme.shapes.large),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    Theme.spacing.xxs,
                    Alignment.CenterVertically
                )
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Theme.dimensions.touchTarget),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    stringResource(Res.string.processing_image_with_ai),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

private fun formatCount(pattern: String, count: Int): String {
    val placeholder = "%1" + '$' + "d"
    return pattern.replace(placeholder, count.toString())
}
