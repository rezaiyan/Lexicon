package presentation.ui.components.imports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import events.OnEvents
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.add_a_word
import lexicon.resources.generated.resources.add_word
import lexicon.resources.generated.resources.add_word_description
import lexicon.resources.generated.resources.ai_powered_extraction
import lexicon.resources.generated.resources.camera
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.capture_vocab_from_image
import lexicon.resources.generated.resources.choose_file
import lexicon.resources.generated.resources.choose_from_library
import lexicon.resources.generated.resources.confirm_and_extract
import lexicon.resources.generated.resources.confirm_languages
import lexicon.resources.generated.resources.description_optional
import lexicon.resources.generated.resources.extract_example_sentences
import lexicon.resources.generated.resources.extract_individual_words
import lexicon.resources.generated.resources.extraction_options
import lexicon.resources.generated.resources.failed_to_load_image
import lexicon.resources.generated.resources.format_example_1
import lexicon.resources.generated.resources.format_example_2
import lexicon.resources.generated.resources.format_example_3
import lexicon.resources.generated.resources.gallery
import lexicon.resources.generated.resources.import_failed_generic
import lexicon.resources.generated.resources.import_from_file
import lexicon.resources.generated.resources.import_text
import lexicon.resources.generated.resources.import_words
import lexicon.resources.generated.resources.individual_words_hint
import lexicon.resources.generated.resources.original_language
import lexicon.resources.generated.resources.original_word
import lexicon.resources.generated.resources.preview_selected_image
import lexicon.resources.generated.resources.processing_file
import lexicon.resources.generated.resources.processing_image_with_ai
import lexicon.resources.generated.resources.review_before_processing
import lexicon.resources.generated.resources.select_at_least_one_option
import lexicon.resources.generated.resources.select_txt_file_description
import lexicon.resources.generated.resources.sentences_hint
import lexicon.resources.generated.resources.success_imported_words
import lexicon.resources.generated.resources.supported_format
import lexicon.resources.generated.resources.take_new_photo
import lexicon.resources.generated.resources.translation_label
import lexicon.resources.generated.resources.translation_language
import lexicon.resources.generated.resources.try_another_image
import lexicon.resources.generated.resources.txt_format
import lexicon.resources.generated.resources.words_added_count
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import presentation.model.ImageImportState
import components.dialog.LexiconDialogContent
import overlay.bottomsheet.BottomSheetPages
import presentation.ui.components.LanguageSelectionContent
import theme.Theme
import utils.Language
import utils.rememberCameraLauncher
import utils.rememberImagePickerLauncher
import utils.rememberTextFilePickerLauncher
import utils.toImageBitmap

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
    val successImportedWordsFormat = stringResource(Res.string.success_imported_words)
    val latestErrorMessage = rememberUpdatedState(errorMessage)
    val latestSuccessFormat = rememberUpdatedState(successImportedWordsFormat)

    OnEvents(viewModel.effects) { event ->
        when (event) {
            is ImportEvent.WordAddedSuccessfully -> {
                // Do NOT dismiss — sheet stays open for adding more words
            }

            is ImportEvent.FileImportSuccessful -> {
                val pattern = "%1" + '$' + "d"
                val message = latestSuccessFormat.value.replace(pattern, event.count.toString())
                onShowSnackBar(message)
                onDismiss()
            }

            is ImportEvent.ImageImportSuccessful -> {
                val pattern = "%1" + '$' + "d"
                val message = latestSuccessFormat.value.replace(pattern, event.count.toString())
                onShowSnackBar(message)
                onDismiss()
            }

            is ImportEvent.Error -> {
                val message = if (event.message.isNotEmpty()) {
                    "[Error] ${event.message}"
                } else {
                    latestErrorMessage.value
                }
                onShowSnackBar(message)
                onDismiss()
            }
        }
    }

    LaunchedEffect(state.showLanguageConfirmation) {
        if (state.showLanguageConfirmation) {
            currentPage = ImportPage.LanguageConfirmation
        }
    }

    BottomSheetPages(currentPage, label = "ImportPages") { page ->
        when (page) {
            ImportPage.Main -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm)
                        .imePadding()
                ) {
                    Text(
                        stringResource(Res.string.import_words),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = Theme.spacing.sm)
                    )

                    ImportTabSelector(
                        tabs = state.tabs,
                        selectedTab = state.selectedTab,
                        onTabSelected = viewModel::selectTab,
                    )

                    TabContainer(
                        modifier = Modifier
                            .padding(top = Theme.spacing.md)
                            .weight(1f, fill = false),
                        selectedTab = state.selectedTab,
                        textInputState = state.textInputState,
                        fileImportState = state.fileImportState,
                        imageImportState = state.imageImportState,
                        sourceLanguage = state.sourceLanguage,
                        targetLanguage = state.targetLanguage,
                        onWordChange = viewModel::updateWord,
                        onTranslationChange = viewModel::updateTranslation,
                        onDescriptionChange = viewModel::updateDescription,
                        onAddWord = viewModel::addWord,
                        importFile = viewModel::importFile,
                        onSelectImage = viewModel::selectImage,
                        onClearSelectedImage = viewModel::clearSelectedImage,
                        onUpdateExtractionOptions = viewModel::updateExtractionOptions,
                        onImportImage = viewModel::importImage,
                        onDismiss = onDismiss,
                        onShowSourceLanguage = { currentPage = ImportPage.MainSourceLanguage },
                        onShowTargetLanguage = { currentPage = ImportPage.MainTargetLanguage },
                    )
                }
            }

            ImportPage.LanguageConfirmation -> {
                ImportLanguageConfirmationContent(
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
            }

            ImportPage.MainSourceLanguage -> {
                LanguagePickerPage(
                    currentLanguage = state.sourceLanguage,
                    onLanguageSelected = { language ->
                        viewModel.selectSourceLanguage(language)
                        currentPage = ImportPage.Main
                    },
                    onBack = { currentPage = ImportPage.Main },
                )
            }

            ImportPage.MainTargetLanguage -> {
                LanguagePickerPage(
                    currentLanguage = state.targetLanguage,
                    onLanguageSelected = { language ->
                        viewModel.selectTargetLanguage(language)
                        currentPage = ImportPage.Main
                    },
                    onBack = { currentPage = ImportPage.Main },
                )
            }

            ImportPage.ConfirmSourceLanguage -> {
                LanguagePickerPage(
                    currentLanguage = state.targetLanguage,
                    onLanguageSelected = { language ->
                        viewModel.selectTargetLanguage(language)
                        currentPage = ImportPage.LanguageConfirmation
                    },
                    onBack = { currentPage = ImportPage.LanguageConfirmation },
                )
            }

            ImportPage.ConfirmTargetLanguage -> {
                LanguagePickerPage(
                    currentLanguage = state.sourceLanguage,
                    onLanguageSelected = { language ->
                        viewModel.selectSourceLanguage(language)
                        currentPage = ImportPage.LanguageConfirmation
                    },
                    onBack = { currentPage = ImportPage.LanguageConfirmation },
                )
            }
        }
    }
}

@Composable
private fun ImportTabSelector(
    tabs: List<ImportTabV2>,
    selectedTab: ImportTabV2,
    onTabSelected: (ImportTabV2) -> Unit,
) {
    val selectedTabIndex = tabs.indexOfFirst { tab ->
        when (selectedTab) {
            is ImportTabV2.Text if tab is ImportTabV2.Text -> true
            is ImportTabV2.File if tab is ImportTabV2.File -> true
            is ImportTabV2.Image if tab is ImportTabV2.Image -> true
            else -> false
        }
    }.takeIf { it >= 0 } ?: 0

    PrimaryTabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.medium)),
        tabs = {
            tabs.forEach { tab ->
                val isSelected = when (selectedTab) {
                    is ImportTabV2.Text if tab is ImportTabV2.Text -> true
                    is ImportTabV2.File if tab is ImportTabV2.File -> true
                    is ImportTabV2.Image if tab is ImportTabV2.Image -> true
                    else -> false
                }
                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.height(Theme.dimensions.buttonHeight)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall3),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            modifier = Modifier.size(Theme.dimensions.iconSizeMedium)
                        )
                        Text(
                            stringResource(tab.title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun TabContainer(
    selectedTab: ImportTabV2,
    textInputState: TextInputState,
    fileImportState: ImportFileState,
    imageImportState: ImageImportState,
    sourceLanguage: Language,
    targetLanguage: Language,
    modifier: Modifier,
    onWordChange: (String) -> Unit,
    onTranslationChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAddWord: () -> Unit,
    importFile: (String, String?) -> Unit,
    onSelectImage: (ByteArray) -> Unit,
    onClearSelectedImage: () -> Unit,
    onUpdateExtractionOptions: (List<ExtractionOption>) -> Unit,
    onImportImage: () -> Unit,
    onDismiss: () -> Unit,
    onShowSourceLanguage: () -> Unit,
    onShowTargetLanguage: () -> Unit,
) {
    val isLoading = fileImportState is ImportFileState.Loading
    val isEnabled = fileImportState !is ImportFileState.Loading
    val isImageLoading = imageImportState is ImageImportState.Loading
    val isImageEnabled = imageImportState !is ImageImportState.Loading

    Box(modifier = modifier) {
        val selectedTabKind = when (selectedTab) {
            is ImportTabV2.Text -> 0
            is ImportTabV2.File -> 1
            is ImportTabV2.Image -> 2
        }
        AnimatedContent(
            targetState = selectedTabKind,
            modifier = Modifier.fillMaxWidth()
        ) { _ ->
            when (selectedTab) {
                is ImportTabV2.Text -> TextTab(
                    textInputState = textInputState,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    onWordChange = onWordChange,
                    onTranslationChange = onTranslationChange,
                    onDescriptionChange = onDescriptionChange,
                    onAddWord = onAddWord,
                    onShowSourceLanguage = onShowSourceLanguage,
                    onShowTargetLanguage = onShowTargetLanguage,
                )

                is ImportTabV2.File -> FileTab(
                    importFile = importFile,
                    onDismiss = onDismiss,
                    isEnabled = isEnabled,
                    isLoading = isLoading,
                )

                is ImportTabV2.Image -> {
                    val imageTab = selectedTab
                    val extractWords = imageTab.extractionOption.contains(ExtractionOption.Word)
                    val extractSentences =
                        imageTab.extractionOption.contains(ExtractionOption.Sentence)
                    val imagePickerLauncher = rememberImagePickerLauncher { bytes ->
                        if (bytes != null) onSelectImage(bytes)
                    }
                    val cameraLauncher = rememberCameraLauncher { bytes ->
                        if (bytes != null) onSelectImage(bytes)
                    }
                    FromImageTab(
                        extractWords = extractWords,
                        extractSentences = extractSentences,
                        onExtractWordsChange = { checked ->
                            val base = imageTab.extractionOption.toMutableList()
                            if (checked && !base.contains(ExtractionOption.Word)) base.add(
                                ExtractionOption.Word
                            )
                            if (!checked) base.remove(ExtractionOption.Word)
                            onUpdateExtractionOptions(base)
                        },
                        onExtractSentencesChange = { checked ->
                            val base = imageTab.extractionOption.toMutableList()
                            if (checked && !base.contains(ExtractionOption.Sentence)) base.add(
                                ExtractionOption.Sentence
                            )
                            if (!checked) base.remove(ExtractionOption.Sentence)
                            onUpdateExtractionOptions(base)
                        },
                        onCameraClick = cameraLauncher,
                        onGalleryClick = imagePickerLauncher,
                        onDismiss = onDismiss,
                        isEnabled = isImageEnabled && !isLoading,
                        selectedImageBytes = imageTab.selectedImage,
                        onConfirmImage = onImportImage,
                        onCancelImage = onClearSelectedImage
                    )
                }
            }
        }

        if (isImageLoading) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) { detectTapGestures { } },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(Theme.shapes.large)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Theme.spacing.cardPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall2)
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
    }
}


@Composable
private fun TextTab(
    textInputState: TextInputState,
    sourceLanguage: Language,
    targetLanguage: Language,
    onWordChange: (String) -> Unit,
    onTranslationChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAddWord: () -> Unit,
    onShowSourceLanguage: () -> Unit,
    onShowTargetLanguage: () -> Unit,
) {
    val isAddEnabled by derivedStateOf { textInputState.isAddEnabled }
    val wordFocusRequester = remember { FocusRequester() }
    val translationFocusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }
    var previousWordsAdded by remember { mutableStateOf(textInputState.wordsAddedCount) }

    // Auto-focus word field after successful add
    LaunchedEffect(textInputState.wordsAddedCount) {
        if (textInputState.wordsAddedCount > previousWordsAdded) {
            wordFocusRequester.requestFocus()
        }
        previousWordsAdded = textInputState.wordsAddedCount
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            InfoCard(
                title = stringResource(Res.string.add_a_word),
                description = stringResource(Res.string.add_word_description),
                icon = Icons.Filled.Edit
            )

            Spacer(modifier = Modifier.height(Theme.spacing.cardSpacing))

            // Inline language selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacing)
            ) {
                CompactLanguageSelector(
                    label = stringResource(Res.string.original_language),
                    language = sourceLanguage,
                    onClick = onShowSourceLanguage,
                    modifier = Modifier.weight(1f)
                )
                CompactLanguageSelector(
                    label = stringResource(Res.string.translation_language),
                    language = targetLanguage,
                    onClick = onShowTargetLanguage,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Theme.spacing.cardSpacing))

            OutlinedTextField(
                value = textInputState.word,
                onValueChange = onWordChange,
                label = { Text(stringResource(Res.string.original_word)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(wordFocusRequester),
                singleLine = true,
                enabled = textInputState.isEnabled,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { translationFocusRequester.requestFocus() }
                )
            )

            Spacer(modifier = Modifier.height(Theme.spacing.extraSmall2))

            OutlinedTextField(
                value = textInputState.translation,
                onValueChange = onTranslationChange,
                label = { Text(stringResource(Res.string.translation_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(translationFocusRequester),
                singleLine = true,
                enabled = textInputState.isEnabled,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { descriptionFocusRequester.requestFocus() }
                )
            )

            Spacer(modifier = Modifier.height(Theme.spacing.extraSmall2))

            OutlinedTextField(
                value = textInputState.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(Res.string.description_optional)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(descriptionFocusRequester),
                singleLine = true,
                enabled = textInputState.isEnabled,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (isAddEnabled) onAddWord() }
                )
            )

            // Session counter with animated checkmark
            AnimatedVisibility(
                visible = textInputState.wordsAddedCount > 0,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Theme.spacing.cardSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    AnimatedVisibility(
                        visible = textInputState.showSuccessIndicator,
                        enter = fadeIn() + scaleIn(initialScale = 0.5f),
                        exit = fadeOut()
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(Theme.dimensions.iconSizeMedium)
                                .padding(end = Theme.spacing.xxs)
                        )
                    }
                    val countText = stringResource(Res.string.words_added_count)
                    val pattern = "%1" + '$' + "d"
                    Text(
                        text = countText.replace(pattern, textInputState.wordsAddedCount.toString()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Inline error message
            AnimatedVisibility(
                visible = textInputState.errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = textInputState.errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = Theme.spacing.extraSmall2)
                )
            }

            Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))
        }

        Button(
            onClick = onAddWord,
            enabled = isAddEnabled,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Theme.spacing.extraSmall3))
            Text(stringResource(Res.string.add_word))
        }
    }
}

@Composable
private fun CompactLanguageSelector(
    label: String,
    language: Language,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(Theme.shapes.medium)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.cardSpacing, vertical = Theme.spacing.extraSmall2)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = language.nativeName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FileTab(
    isEnabled: Boolean,
    isLoading: Boolean,
    importFile: (String, String?) -> Unit,
    onDismiss: () -> Unit,
) {

    val filePickerLauncher = rememberTextFilePickerLauncher { fileContent, fileName ->
        if (fileContent != null) {
            importFile(fileContent, fileName)
        } else if (fileName != null) {
            importFile("", fileName)
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InfoCard(
                title = stringResource(Res.string.import_from_file),
                description = stringResource(Res.string.select_txt_file_description),
                icon = Icons.Filled.AttachFile
            )

            Spacer(modifier = Modifier.height(Theme.spacing.cardSpacingLarge))

            Button(
                onClick = filePickerLauncher,
                enabled = isEnabled && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(Theme.shapes.large),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                if (isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall2)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Theme.dimensions.touchTarget),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            stringResource(Res.string.processing_file),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall2)
                    ) {
                        Icon(
                            Icons.Filled.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(Theme.dimensions.touchTarget)
                        )
                        Text(
                            stringResource(Res.string.choose_file),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(Res.string.txt_format),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Theme.spacing.cardSpacing))

            SupportedFormatsCard()

            Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))
        }

        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            enabled = isEnabled
        ) {
            Text(stringResource(Res.string.cancel))
        }
    }
}

@Composable
private fun SupportedFormatsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(Theme.shapes.medium)
    ) {
        Column(
            modifier = Modifier.padding(Theme.spacing.cardPadding)
        ) {
            Text(
                stringResource(Res.string.supported_format),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Theme.spacing.extraSmall3))
            Text(
                "• " + stringResource(Res.string.format_example_1),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "• " + stringResource(Res.string.format_example_2),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "• " + stringResource(Res.string.format_example_3),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun FromImageTab(
    extractWords: Boolean,
    extractSentences: Boolean,
    onExtractWordsChange: (Boolean) -> Unit,
    onExtractSentencesChange: (Boolean) -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDismiss: () -> Unit,
    isEnabled: Boolean,
    selectedImageBytes: ByteArray?,
    onConfirmImage: () -> Unit,
    onCancelImage: () -> Unit
) {
    AnimatedContent(
        targetState = selectedImageBytes != null,
        transitionSpec = {
            if (targetState) {
                (slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(animationSpec = tween(400))).togetherWith(
                    slideOutVertically(
                        targetOffsetY = { -it / 3 },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                )
            } else {
                (slideInVertically(
                    initialOffsetY = { -it / 3 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(animationSpec = tween(400))).togetherWith(
                    slideOutVertically(
                        targetOffsetY = { it / 3 },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                )
            }
        },
        label = "ImagePreviewTransition"
    ) { showPreview ->
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (showPreview && selectedImageBytes != null) {
                    ImagePreviewCard(
                        imageBytes = selectedImageBytes,
                        onConfirm = onConfirmImage,
                        onCancel = onCancelImage,
                        isEnabled = isEnabled
                    )
                } else {
                    AiExtractionInfoCard()

                    Spacer(modifier = Modifier.height(Theme.spacing.cardSpacingLarge))

                    ExtractionOptionsCard(
                        extractWords = extractWords,
                        extractSentences = extractSentences,
                        onExtractWordsChange = onExtractWordsChange,
                        onExtractSentencesChange = onExtractSentencesChange,
                        isEnabled = isEnabled
                    )

                    Spacer(modifier = Modifier.height(Theme.spacing.cardSpacingLarge))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)
                    ) {
                        Button(
                            onClick = onCameraClick,
                            enabled = (extractWords || extractSentences) && isEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .height(140.dp),
                            shape = RoundedCornerShape(Theme.shapes.large),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall2)
                            ) {
                                Icon(
                                    Icons.Filled.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(Theme.dimensions.touchTarget)
                                )
                                Text(
                                    stringResource(Res.string.camera),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    stringResource(Res.string.take_new_photo),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }

                        Button(
                            onClick = onGalleryClick,
                            enabled = (extractWords || extractSentences) && isEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .height(140.dp),
                            shape = RoundedCornerShape(Theme.shapes.large),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall2)
                            ) {
                                Icon(
                                    Icons.Filled.Photo,
                                    contentDescription = null,
                                    modifier = Modifier.size(Theme.dimensions.touchTarget)
                                )
                                Text(
                                    stringResource(Res.string.gallery),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    stringResource(Res.string.choose_from_library),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))
                }
            }

            if (!showPreview || selectedImageBytes == null) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding(),
                    enabled = isEnabled
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun AiExtractionInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(Theme.shapes.medium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacing)
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(Theme.dimensions.iconSizeXLarge),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.ai_powered_extraction),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(Theme.spacing.extraSmall3))
                Text(
                    stringResource(Res.string.capture_vocab_from_image),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

@Composable
private fun ExtractionOptionsCard(
    extractWords: Boolean,
    extractSentences: Boolean,
    onExtractWordsChange: (Boolean) -> Unit,
    onExtractSentencesChange: (Boolean) -> Unit,
    isEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(Theme.shapes.medium),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(Theme.spacing.cardPadding)
        ) {
            Text(
                stringResource(Res.string.extraction_options),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Theme.spacing.extraSmall2))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Theme.shapes.small))
                    .padding(Theme.spacing.extraSmall2),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = extractWords,
                    onCheckedChange = onExtractWordsChange,
                    enabled = isEnabled
                )
                Column(modifier = Modifier.weight(1f).padding(start = Theme.spacing.extraSmall2)) {
                    Text(
                        stringResource(Res.string.extract_individual_words),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        stringResource(Res.string.individual_words_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Theme.spacing.extraSmall3))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Theme.shapes.small))
                    .padding(Theme.spacing.extraSmall2),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = extractSentences,
                    onCheckedChange = onExtractSentencesChange,
                    enabled = isEnabled
                )
                Column(modifier = Modifier.weight(1f).padding(start = Theme.spacing.extraSmall2)) {
                    Text(
                        stringResource(Res.string.extract_example_sentences),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        stringResource(Res.string.sentences_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (!extractWords && !extractSentences) {
        Spacer(modifier = Modifier.height(Theme.spacing.extraSmall2))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(Theme.dimensions.iconSizeSmall)
            )
            Text(
                stringResource(Res.string.select_at_least_one_option),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ImagePreviewCard(
    imageBytes: ByteArray,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    isEnabled: Boolean
) {
    val imageBitmap = androidx.compose.runtime.remember(imageBytes) {
        imageBytes.toImageBitmap()
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(300)) +
                slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
        exit = fadeOut(animationSpec = tween(200)) +
                slideOutVertically(targetOffsetY = { it / 2 })
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .padding(Theme.spacing.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacingLarge)
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 100)) +
                        scaleIn(initialScale = 0.8f, animationSpec = tween(400, delayMillis = 100))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall2)
                ) {
                    Icon(
                        Icons.Filled.Preview,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Theme.dimensions.iconSizeLarge)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(Res.string.preview_selected_image),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(Res.string.review_before_processing),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(500, delayMillis = 200)) +
                        scaleIn(
                            initialScale = 0.9f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = Theme.elevation.extraHigh),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageBitmap != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(Theme.spacing.sm),
                                shape = RoundedCornerShape(Theme.shapes.large),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.3f
                                    )
                                )
                            ) {
                                Image(
                                    bitmap = imageBitmap,
                                    contentDescription = stringResource(Res.string.preview_selected_image),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(Theme.spacing.xs)
                                        .clip(RoundedCornerShape(Theme.shapes.medium)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacing),
                                modifier = Modifier.padding(Theme.spacing.cardPadding)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    stringResource(Res.string.failed_to_load_image),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    stringResource(Res.string.try_another_image),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = imageBitmap != null,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 300)) +
                        expandVertically(animationSpec = tween(400, delayMillis = 300)),
                exit = fadeOut(animationSpec = tween(200)) +
                        shrinkVertically(animationSpec = tween(200))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacing)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        enabled = isEnabled
                    ) {
                        Text(stringResource(Res.string.cancel))
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        enabled = isEnabled && imageBitmap != null
                    ) {
                        Text(stringResource(Res.string.confirm_and_extract))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    description: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(Theme.shapes.medium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.cardPadding),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacing)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Theme.dimensions.iconSize)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(Theme.spacing.xxs))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ImportLanguageConfirmationContent(
    sourceLanguage: Language,
    targetLanguage: Language,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onShowSourceLanguage: () -> Unit,
    onShowTargetLanguage: () -> Unit,
) {
    LexiconDialogContent(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = Theme.spacing.lg)
            .padding(bottom = Theme.spacing.lg),
        icon = Icons.Default.Language,
        title = stringResource(Res.string.confirm_languages),
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)
            ) {
                LanguageRow(
                    label = stringResource(Res.string.original_language),
                    language = sourceLanguage,
                    onClick = onShowSourceLanguage,
                )
                LanguageRow(
                    label = stringResource(Res.string.translation_language),
                    language = targetLanguage,
                    onClick = onShowTargetLanguage,
                )
            }
        },
        primaryButtonText = stringResource(Res.string.import_text),
        primaryButtonOnClick = onConfirm,
        secondaryButtonText = stringResource(Res.string.cancel),
        secondaryButtonOnClick = onDismiss,
    )
}

@Composable
private fun LanguageRow(
    label: String,
    language: Language,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.small))
            .clickable(onClick = onClick)
            .padding(vertical = Theme.spacing.extraSmall, horizontal = Theme.spacing.small)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Theme.spacing.xxxs))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)
        ) {
            Text(
                text = "${language.nativeName} (${language.displayName})",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LanguagePickerPage(
    currentLanguage: Language,
    onLanguageSelected: (Language) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.md)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
            )
        }
        LanguageSelectionContent(
            currentLanguage = currentLanguage,
            onLanguageSelected = onLanguageSelected,
        )
    }
}
