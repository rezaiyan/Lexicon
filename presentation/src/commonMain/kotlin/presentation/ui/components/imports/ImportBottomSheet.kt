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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Upload
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import events.OnEvents
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import presentation.model.ImageImportState
import theme.Theme
import utils.rememberCameraLauncher
import utils.rememberImagePickerLauncher
import utils.rememberTextFilePickerLauncher
import utils.toImageBitmap
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.ai_powered_extraction
import vokab.resources.generated.resources.camera
import vokab.resources.generated.resources.cancel
import vokab.resources.generated.resources.capture_vocab_from_image
import vokab.resources.generated.resources.choose_file
import vokab.resources.generated.resources.choose_from_library
import vokab.resources.generated.resources.confirm_and_extract
import vokab.resources.generated.resources.enter_words
import vokab.resources.generated.resources.enter_words_manually_description
import vokab.resources.generated.resources.extract_example_sentences
import vokab.resources.generated.resources.extract_individual_words
import vokab.resources.generated.resources.extraction_options
import vokab.resources.generated.resources.failed_to_load_image
import vokab.resources.generated.resources.format_example_1
import vokab.resources.generated.resources.format_example_2
import vokab.resources.generated.resources.format_example_3
import vokab.resources.generated.resources.format_hint_comma_separated
import vokab.resources.generated.resources.gallery
import vokab.resources.generated.resources.import_failed_generic
import vokab.resources.generated.resources.import_from_file
import vokab.resources.generated.resources.import_text
import vokab.resources.generated.resources.import_words
import vokab.resources.generated.resources.individual_words_hint
import vokab.resources.generated.resources.preview_selected_image
import vokab.resources.generated.resources.processing_file
import vokab.resources.generated.resources.processing_image_with_ai
import vokab.resources.generated.resources.remaining_extractions
import vokab.resources.generated.resources.review_before_processing
import vokab.resources.generated.resources.select_at_least_one_option
import vokab.resources.generated.resources.select_txt_file_description
import vokab.resources.generated.resources.sentences_hint
import vokab.resources.generated.resources.success_imported_words
import vokab.resources.generated.resources.supported_format
import vokab.resources.generated.resources.take_new_photo
import vokab.resources.generated.resources.try_another_image
import vokab.resources.generated.resources.txt_format
import vokab.resources.generated.resources.type_or_paste_words

@Composable
fun ImportBottomSheet(onDismiss: () -> Unit, onShowSnackBar: (String) -> Unit) {
    val viewModel = koinInject<ImportViewModel>()
    val state = viewModel.state()
    val errorMessage = stringResource(Res.string.import_failed_generic)
    val successImportedWordsFormat = stringResource(Res.string.success_imported_words)
    val latestErrorMessage = rememberUpdatedState(errorMessage)
    val latestSuccessFormat = rememberUpdatedState(successImportedWordsFormat)

    OnEvents(viewModel.events) { event ->
        when (event) {
            is ImportEvent.TextImportSuccessful -> {
                val pattern = "%1" + '$' + "d"
                val message = latestSuccessFormat.value.replace(pattern, event.count.toString())
                onShowSnackBar(message)
                onDismiss()
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
                    "✗ ${event.message}"
                } else {
                    latestErrorMessage.value
                }
                onShowSnackBar(message)
                onDismiss()
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.medium)
                .imePadding()
        ) {
            ImportHeader(
                onDismiss = onDismiss,
                canDismiss = true,//todo check loading
            )

            ImportTabSelector(
                tabs = state.tabs,
                selectedTab = state.selectedTab,
                onTabSelected = viewModel::selectTab,
            )

            TabContainer(
                modifier = Modifier
                    .padding(top = Theme.spacing.medium)
                    .weight(1f, fill = false),
                selectedTab = state.selectedTab,
                textInputState = state.textInputState,
                fileImportState = state.fileImportState,
                imageImportState = state.imageImportState,
                onTextChange = viewModel::updateTextEntry,
                onImport = viewModel::importText,
                importFile = viewModel::importFile,
                onSelectImage = viewModel::selectImage,
                onClearSelectedImage = viewModel::clearSelectedImage,
                onUpdateExtractionOptions = viewModel::updateExtractionOptions,
                onImportImage = viewModel::importImage,
                onDismiss = onDismiss,
            )
        }

    }
}

@Composable
private fun ImportHeader(
    onDismiss: () -> Unit,
    canDismiss: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(Res.string.import_words),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(
            onClick = onDismiss,
            enabled = canDismiss,
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(Res.string.cancel),
                tint = if (canDismiss)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
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
            .clip(RoundedCornerShape(12.dp)),
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
                    modifier = Modifier.height(56.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall3),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            stringResource(tab.title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
    modifier: Modifier,
    onTextChange: (String) -> Unit,
    importFile: (String, String?) -> Unit,
    onSelectImage: (ByteArray) -> Unit,
    onClearSelectedImage: () -> Unit,
    onUpdateExtractionOptions: (List<ExtractionOption>) -> Unit,
    onImportImage: () -> Unit,
    onDismiss: () -> Unit,
    onImport: () -> Unit,
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
                    onTextChange = onTextChange,
                    onDismiss = onDismiss,
                    onImport = onImport,
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
                        remainingAiExtractions = selectedTab.remainingCredit,
                        isSubscribed = selectedTab.isSubscribed,
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
                        .clip(RoundedCornerShape(16.dp)),
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
                            modifier = Modifier.size(48.dp),
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
    onTextChange: (String) -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isImportEnabled by derivedStateOf { textInputState.isImportEnabled }

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
                title = stringResource(Res.string.type_or_paste_words),
                description = stringResource(Res.string.enter_words_manually_description),
                icon = Icons.Filled.Edit
            )

            Spacer(modifier = Modifier.height(Theme.spacing.cardSpacing))

            OutlinedTextField(
                value = textInputState.text,
                onValueChange = onTextChange,
                label = { Text(stringResource(Res.string.enter_words)) },
                placeholder = {
                    Text(
                        "hello,hola\ngoodbye,adiós\nthanks,gracias",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 350.dp),
                minLines = 8,
                enabled = textInputState.isEnabled,
                supportingText = {
                    Text(
                        stringResource(Res.string.format_hint_comma_separated),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            )

            Spacer(modifier = Modifier.height(Theme.spacing.sectionSpacing))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacing)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                enabled = textInputState.isEnabled
            ) {
                Text(stringResource(Res.string.cancel))
            }
            Button(
                onClick = onImport,
                enabled = isImportEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Filled.Upload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Theme.spacing.extraSmall3))
                Text(stringResource(Res.string.import_text))
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
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
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
                shape = RoundedCornerShape(16.dp),
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
                            modifier = Modifier.size(48.dp),
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
                            modifier = Modifier.size(48.dp)
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
        shape = RoundedCornerShape(12.dp)
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
    remainingAiExtractions: Int,
    isSubscribed: Boolean,
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
                    .verticalScroll(rememberScrollState())
            ) {
                if (showPreview && selectedImageBytes != null) {
                    ImagePreviewCard(
                        imageBytes = selectedImageBytes,
                        onConfirm = onConfirmImage,
                        onCancel = onCancelImage,
                        isEnabled = isEnabled
                    )
                } else {
                    AiExtractionInfoCard(
                        remainingAiExtractions = remainingAiExtractions,
                        isSubscribed = isSubscribed
                    )

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
                            shape = RoundedCornerShape(16.dp),
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
                                    modifier = Modifier.size(48.dp)
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
                            shape = RoundedCornerShape(16.dp),
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
                                    modifier = Modifier.size(48.dp)
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
private fun AiExtractionInfoCard(
    remainingAiExtractions: Int,
    isSubscribed: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.cardPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacing)
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
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

            if (remainingAiExtractions > 0 && isSubscribed) {
                Spacer(modifier = Modifier.height(Theme.spacing.extraSmall))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.height(Theme.spacing.extraSmall))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall3)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(Theme.spacing.extraSmall3),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = stringResource(Res.string.remaining_extractions),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        Card(
                            modifier = Modifier.padding(Theme.spacing.extraSmall3),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (remainingAiExtractions <= 2)
                                    MaterialTheme.colorScheme.errorContainer
                                else
                                    MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Text(
                                text = "$remainingAiExtractions",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (remainingAiExtractions <= 2)
                                    MaterialTheme.colorScheme.onErrorContainer
                                else
                                    MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
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
        shape = RoundedCornerShape(12.dp),
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
                    .clip(RoundedCornerShape(8.dp))
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
                    .clip(RoundedCornerShape(8.dp))
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
                modifier = Modifier.size(16.dp)
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
                        modifier = Modifier.size(28.dp)
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
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
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
                                    .padding(12.dp),
                                shape = RoundedCornerShape(16.dp),
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
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(12.dp)),
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
        shape = RoundedCornerShape(12.dp)
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
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}
