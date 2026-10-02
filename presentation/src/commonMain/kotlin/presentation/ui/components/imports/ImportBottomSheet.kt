package presentation.ui.components.imports

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import events.OnEvents
import expects.BackHandler
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.create_tag
import lexicon.resources.generated.resources.import_error_file_format
import lexicon.resources.generated.resources.import_error_image_hint
import lexicon.resources.generated.resources.import_error_network
import lexicon.resources.generated.resources.import_failed_generic
import lexicon.resources.generated.resources.new_tag
import lexicon.resources.generated.resources.original_language
import lexicon.resources.generated.resources.translation_language
import lexicon.resources.generated.resources.translation_language_hint
import lexicon.resources.generated.resources.word_language_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import overlay.bottomsheet.BottomSheetPageConfig
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import presentation.model.ImageImportState
import presentation.ui.components.TagFormContent
import utils.Language
import utils.rememberCameraLauncher
import utils.rememberImagePickerLauncher

private sealed interface ImportPage {
    data object MethodChooser : ImportPage
    data object SourceLanguageChooser : ImportPage
    data object TextContent : ImportPage
    data object FileContent : ImportPage
    data object ImageContent : ImportPage
    data object ImageReview : ImportPage
    data object LanguageConfirmation : ImportPage
    data object SourceLanguagePicker : ImportPage
    data object TargetLanguagePicker : ImportPage
    data object CreateTag : ImportPage
    data class Success(val count: Int, val previewWords: List<String>) : ImportPage
}

/**
 * Manual add-words flow: chooser → (word language, first time only) → type / file / photo → success.
 *
 * @param onAiAssistant shows the "Generate with AI" card on the chooser when non-null.
 * @param onStartReview offered on the success page when non-null.
 */
@Composable
fun ImportBottomSheet(
    onDismiss: () -> Unit,
    onShowSnackBar: (String) -> Unit,
    onClose: (() -> Unit)? = null,
    onAiAssistant: (() -> Unit)? = null,
    onStartReview: (() -> Unit)? = null,
) {
    val viewModel = koinInject<ImportViewModel>()
    val state by viewModel.state()
    val pages = rememberBottomSheetPageNavigator<ImportPage>(ImportPage.MethodChooser)
    val errorMessageGeneric = stringResource(Res.string.import_failed_generic)
    val errorMessageNetwork = stringResource(Res.string.import_error_network)
    val errorMessageImage = stringResource(Res.string.import_error_image_hint)
    val errorMessageFileFormat = stringResource(Res.string.import_error_file_format)
    val latestErrorGeneric = rememberUpdatedState(errorMessageGeneric)
    val latestErrorNetwork = rememberUpdatedState(errorMessageNetwork)
    val latestErrorImage = rememberUpdatedState(errorMessageImage)
    val latestErrorFileFormat = rememberUpdatedState(errorMessageFileFormat)
    // Words shown as chips on the success page; captured before the import clears review state
    var pendingPreviewWords by remember { mutableStateOf(emptyList<String>()) }

    OnEvents(viewModel.effects) { effect ->
        when (effect) {
            is ImportEffect.FileImportSuccessful ->
                pages.navigateTo(ImportPage.Success(effect.count, emptyList()))

            is ImportEffect.ImageImportSuccessful ->
                pages.navigateTo(ImportPage.Success(effect.count, pendingPreviewWords))

            is ImportEffect.Error -> {
                val raw = effect.message
                val isNetwork = raw.contains("timeout", ignoreCase = true) ||
                    raw.contains("connect", ignoreCase = true) ||
                    raw.contains("network", ignoreCase = true) ||
                    raw.contains("internet", ignoreCase = true) ||
                    raw.contains("offline", ignoreCase = true)
                val isImageError = raw.contains("image", ignoreCase = true) ||
                    raw.contains("extract", ignoreCase = true) ||
                    raw.contains("recognition", ignoreCase = true)
                val isFileError = raw.contains("parse", ignoreCase = true) ||
                    raw.contains("format", ignoreCase = true) ||
                    raw.contains("csv", ignoreCase = true)

                val message = when {
                    isNetwork -> latestErrorNetwork.value
                    isImageError -> latestErrorImage.value
                    isFileError -> latestErrorFileFormat.value
                    raw.isNotEmpty() -> raw
                    else -> latestErrorGeneric.value
                }
                onShowSnackBar(message)
                onDismiss()
            }
        }
    }

    LaunchedEffect(state.showLanguageConfirmation) {
        if (state.showLanguageConfirmation) pages.navigateTo(ImportPage.LanguageConfirmation)
    }

    LaunchedEffect(state.imageReviewState) {
        when (state.imageReviewState) {
            is ImageReviewState.Review -> if (pages.currentPage !is ImportPage.ImageReview) {
                pages.navigateTo(ImportPage.ImageReview)
            }
            is ImageReviewState.None -> {
                if (pages.currentPage is ImportPage.ImageReview) pages.navigateBack()
            }
        }
    }

    // Clean up language confirmation state when navigating away
    LaunchedEffect(pages.currentPage) {
        val onConfirmFlow = pages.currentPage is ImportPage.LanguageConfirmation ||
            pages.currentPage is ImportPage.SourceLanguagePicker ||
            pages.currentPage is ImportPage.TargetLanguagePicker
        if (!onConfirmFlow && state.showLanguageConfirmation) {
            viewModel.dismissLanguageConfirmation()
        }
    }

    val openCreateTag: () -> Unit = { pages.navigateTo(ImportPage.CreateTag) }

    val openMethod: (ImportTabV2, ImportPage) -> Unit = { tab, page ->
        viewModel.selectTab(tab)
        // Ask for the word language only while it is unknown (still equal to the translation language)
        if (state.sourceLanguage == state.targetLanguage) {
            pages.navigateTo(ImportPage.SourceLanguageChooser)
        } else {
            pages.navigateTo(page)
        }
    }
    val contentPageFor: (ImportTabV2) -> ImportPage = { tab ->
        when (tab) {
            is ImportTabV2.File -> ImportPage.FileContent
            is ImportTabV2.Image -> ImportPage.ImageContent
            else -> ImportPage.TextContent
        }
    }
    val isReviewing = pages.currentPage is ImportPage.ImageReview

    BottomSheetPages(
        navigator = pages,
        onClose = if (isReviewing) viewModel::requestCancelImageReview else onClose,
        label = "ImportPages",
        pageConfig = { page ->
            BottomSheetPageConfig(
                showBackButton = page !is ImportPage.ImageReview && page !is ImportPage.Success,
            )
        },
    ) { page ->
        when (page) {
            is ImportPage.CreateTag -> TagFormContent(
                title = stringResource(Res.string.new_tag),
                confirmText = stringResource(Res.string.create_tag),
                onConfirm = { name ->
                    viewModel.createTag(name)
                    pages.navigateBack()
                },
                onDismiss = { pages.navigateBack() },
            )

            is ImportPage.MethodChooser -> AddWordsChooserContent(
                hasImageAccess = state.tabs.any { it is ImportTabV2.Image },
                onAiAssistant = onAiAssistant,
                onTypeWord = { openMethod(ImportTabV2.Text(), ImportPage.TextContent) },
                onImportFile = { openMethod(ImportTabV2.File(), ImportPage.FileContent) },
                onScanPhoto = { openMethod(ImportTabV2.Image(), ImportPage.ImageContent) },
            )

            is ImportPage.SourceLanguageChooser -> ImportLanguageListPage(
                title = stringResource(Res.string.word_language_title),
                subtitle = stringResource(Res.string.translation_language_hint, state.targetLanguage.displayName),
                languages = Language.entries.filter { it != state.targetLanguage },
                selected = state.sourceLanguage.takeIf { it != state.targetLanguage },
                onLanguageSelected = { language ->
                    viewModel.selectSourceLanguage(language)
                    pages.navigateTo(contentPageFor(state.selectedTab))
                },
            )

            is ImportPage.TextContent -> TextImportContent(
                textInputState = state.textInputState,
                sourceLanguage = state.sourceLanguage,
                targetLanguage = state.targetLanguage,
                tags = state.tags,
                selectedTagId = state.selectedTagId,
                onTagSelected = viewModel::selectTag,
                onCreateTag = openCreateTag,
                onChangeLanguage = { pages.navigateTo(ImportPage.SourceLanguagePicker) },
                onWordChange = viewModel::updateWord,
                onTranslationChange = viewModel::updateTranslation,
                onDescriptionChange = viewModel::updateDescription,
                onAddWord = viewModel::addWord,
                onDone = {
                    val added = state.textInputState
                    if (added.wordsAddedCount > 0) {
                        pages.navigateTo(
                            ImportPage.Success(added.wordsAddedCount, added.recentWords.map { it.word })
                        )
                    } else {
                        onDismiss()
                    }
                },
            )

            is ImportPage.FileContent -> FileImportContent(
                isLoading = state.fileImportState is ImportFileState.Loading,
                sourceLanguage = state.sourceLanguage,
                targetLanguage = state.targetLanguage,
                tags = state.tags,
                selectedTagId = state.selectedTagId,
                onTagSelected = viewModel::selectTag,
                onCreateTag = openCreateTag,
                onChangeLanguage = { pages.navigateTo(ImportPage.SourceLanguagePicker) },
                importFile = viewModel::importFile,
            )

            is ImportPage.ImageContent -> ImageContentPage(
                state = state,
                viewModel = viewModel,
                onChangeLanguage = { pages.navigateTo(ImportPage.SourceLanguagePicker) },
                onCreateTag = openCreateTag,
            )

            is ImportPage.ImageReview -> {
                val reviewState = state.imageReviewState as? ImageReviewState.Review ?: return@BottomSheetPages
                // Composed inside the page so it outranks the pager's own back handler:
                // leaving the review must go through the discard confirmation
                BackHandler { viewModel.requestCancelImageReview() }
                ImageWordReviewContent(
                    reviewState = reviewState,
                    onRemoveWord = viewModel::removeExtractedWord,
                    onStartEditWord = viewModel::startEditingWord,
                    onCancelEdit = viewModel::cancelEditingWord,
                    onSaveEdit = viewModel::saveEditedWord,
                    onConfirmImport = {
                        pendingPreviewWords = reviewState.words.map { it.word }
                        viewModel.confirmImageImport()
                    },
                    onDismissCancelConfirmation = viewModel::dismissCancelConfirmation,
                    onCancelImport = viewModel::cancelImageReview,
                )
            }

            is ImportPage.LanguageConfirmation -> {
                val content = (state.pendingImportAction as? PendingImportAction.File)?.content.orEmpty()
                val lines = remember(content) { previewPairs(content) }
                ImportLanguageConfirmationContent(
                    sourceLanguage = state.sourceLanguage,
                    targetLanguage = state.targetLanguage,
                    previewLines = lines,
                    totalLines = lines.size,
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
                    onSwapLanguages = {
                        val source = state.sourceLanguage
                        viewModel.selectSourceLanguage(state.targetLanguage)
                        viewModel.selectTargetLanguage(source)
                    },
                )
            }

            is ImportPage.SourceLanguagePicker -> ImportLanguageListPage(
                title = stringResource(Res.string.original_language),
                languages = Language.entries,
                selected = state.sourceLanguage,
                onLanguageSelected = { viewModel.selectSourceLanguage(it); pages.navigateBack() },
            )

            is ImportPage.TargetLanguagePicker -> ImportLanguageListPage(
                title = stringResource(Res.string.translation_language),
                languages = Language.entries,
                selected = state.targetLanguage,
                onLanguageSelected = { viewModel.selectTargetLanguage(it); pages.navigateBack() },
            )

            is ImportPage.Success -> ImportSuccessContent(
                count = page.count,
                previewWords = page.previewWords,
                onStartReview = onStartReview,
                onAddMore = {
                    while (pages.canNavigateBack) pages.navigateBack()
                },
                onDone = onDismiss,
            )
        }
    }
}

@Composable
private fun ImageContentPage(
    state: ImportUiState,
    viewModel: ImportViewModel,
    onChangeLanguage: () -> Unit,
    onCreateTag: () -> Unit,
) {
    val isImageLoading = state.imageImportState is ImageImportState.Loading
    val imageTab = state.tabs.filterIsInstance<ImportTabV2.Image>().firstOrNull() ?: return

    val imagePickerLauncher = rememberImagePickerLauncher { bytes ->
        if (bytes != null) viewModel.selectImage(bytes)
    }
    val cameraLauncher = rememberCameraLauncher { bytes ->
        if (bytes != null) viewModel.selectImage(bytes)
    }

    ImageImportContent(
        imageTab = imageTab,
        isEnabled = !isImageLoading && state.fileImportState !is ImportFileState.Loading,
        isLoading = isImageLoading,
        imageQuality = state.imageQuality,
        sourceLanguage = state.sourceLanguage,
        targetLanguage = state.targetLanguage,
        tags = state.tags,
        selectedTagId = state.selectedTagId,
        onTagSelected = viewModel::selectTag,
        onCreateTag = onCreateTag,
        onChangeLanguage = onChangeLanguage,
        onCameraClick = cameraLauncher,
        onGalleryClick = imagePickerLauncher,
        onImportImage = viewModel::importImage,
        onClearSelectedImage = viewModel::clearSelectedImage,
        onQualityChange = viewModel::adjustImageQuality,
    )
}

private val PreviewSeparators = charArrayOf(',', ';')

/** First two columns of each non-blank line, for the confirm-languages preview. */
private fun previewPairs(content: String): List<Pair<String, String>> =
    content.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { line ->
            val parts = line.split(*PreviewSeparators).map { it.trim() }
            parts.first() to parts.getOrElse(1) { "" }
        }
        .toList()
