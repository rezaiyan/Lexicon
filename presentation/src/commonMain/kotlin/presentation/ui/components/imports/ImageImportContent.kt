package presentation.ui.components.imports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.ai_powered_extraction
import lexicon.resources.generated.resources.camera
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.capture_vocab_from_image
import lexicon.resources.generated.resources.choose_from_library
import lexicon.resources.generated.resources.confirm_and_extract
import lexicon.resources.generated.resources.extract_example_sentences
import lexicon.resources.generated.resources.extract_individual_words
import lexicon.resources.generated.resources.extraction_options
import lexicon.resources.generated.resources.failed_to_load_image
import lexicon.resources.generated.resources.gallery
import lexicon.resources.generated.resources.individual_words_hint
import lexicon.resources.generated.resources.preview_selected_image
import lexicon.resources.generated.resources.review_before_processing
import lexicon.resources.generated.resources.select_at_least_one_option
import lexicon.resources.generated.resources.sentences_hint
import lexicon.resources.generated.resources.take_new_photo
import lexicon.resources.generated.resources.try_another_image
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.toImageBitmap

@Composable
internal fun ImageImportContent(
    imageTab: ImportTabV2.Image,
    isEnabled: Boolean,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onUpdateExtractionOptions: (List<ExtractionOption>) -> Unit,
    onImportImage: () -> Unit,
    onClearSelectedImage: () -> Unit,
    onDismiss: () -> Unit,
) {
    val extractWords = imageTab.extractionOption.contains(ExtractionOption.Word)
    val extractSentences = imageTab.extractionOption.contains(ExtractionOption.Sentence)
    val hasImage = imageTab.selectedImage != null

    AnimatedContent(
        targetState = hasImage,
        transitionSpec = {
            val enterOffset = if (targetState) { i: Int -> i / 3 } else { i: Int -> -i / 3 }
            val exitOffset = if (targetState) { i: Int -> -i / 3 } else { i: Int -> i / 3 }
            (slideInVertically(
                initialOffsetY = enterOffset,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
            ) + fadeIn(tween(400))).togetherWith(
                slideOutVertically(targetOffsetY = exitOffset, animationSpec = tween(300))
                        + fadeOut(tween(300))
            )
        },
        label = "ImagePreviewTransition"
    ) { showPreview ->
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (showPreview && imageTab.selectedImage != null) {
                    ImagePreviewCard(
                        imageBytes = imageTab.selectedImage,
                        onConfirm = onImportImage,
                        onCancel = onClearSelectedImage,
                        isEnabled = isEnabled
                    )
                } else {
                    ImageSelectionContent(
                        extractWords = extractWords,
                        extractSentences = extractSentences,
                        onExtractWordsChange = { checked ->
                            val options = imageTab.extractionOption.toMutableList()
                            if (checked) options.addIfAbsent(ExtractionOption.Word)
                            else options.remove(ExtractionOption.Word)
                            onUpdateExtractionOptions(options)
                        },
                        onExtractSentencesChange = { checked ->
                            val options = imageTab.extractionOption.toMutableList()
                            if (checked) options.addIfAbsent(ExtractionOption.Sentence)
                            else options.remove(ExtractionOption.Sentence)
                            onUpdateExtractionOptions(options)
                        },
                        onCameraClick = onCameraClick,
                        onGalleryClick = onGalleryClick,
                        isEnabled = isEnabled,
                    )
                }
            }

            if (!showPreview) {
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
private fun ImageSelectionContent(
    extractWords: Boolean,
    extractSentences: Boolean,
    onExtractWordsChange: (Boolean) -> Unit,
    onExtractSentencesChange: (Boolean) -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    isEnabled: Boolean,
) {
    AiExtractionInfoCard()

    Spacer(modifier = Modifier.height(Theme.spacing.lg))

    ExtractionOptionsCard(
        extractWords = extractWords,
        extractSentences = extractSentences,
        onExtractWordsChange = onExtractWordsChange,
        onExtractSentencesChange = onExtractSentencesChange,
        isEnabled = isEnabled
    )

    Spacer(modifier = Modifier.height(Theme.spacing.lg))

    CaptureButtons(
        onCameraClick = onCameraClick,
        onGalleryClick = onGalleryClick,
        isEnabled = (extractWords || extractSentences) && isEnabled,
    )

    Spacer(modifier = Modifier.height(Theme.spacing.lg))
}

// region Info & Options Cards

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
                .padding(Theme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)
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
                Spacer(modifier = Modifier.height(Theme.spacing.xxxs))
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
    isEnabled: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(Theme.shapes.medium),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(Theme.spacing.md)) {
            Text(
                stringResource(Res.string.extraction_options),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Theme.spacing.xs))

            ExtractionCheckbox(
                checked = extractWords,
                onCheckedChange = onExtractWordsChange,
                enabled = isEnabled,
                title = stringResource(Res.string.extract_individual_words),
                subtitle = stringResource(Res.string.individual_words_hint),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Theme.spacing.xxxs))

            ExtractionCheckbox(
                checked = extractSentences,
                onCheckedChange = onExtractSentencesChange,
                enabled = isEnabled,
                title = stringResource(Res.string.extract_example_sentences),
                subtitle = stringResource(Res.string.sentences_hint),
            )
        }
    }

    if (!extractWords && !extractSentences) {
        Spacer(modifier = Modifier.height(Theme.spacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs),
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
private fun ExtractionCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.small))
            .padding(Theme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Theme.spacing.xs)
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// endregion

// region Capture Buttons

@Composable
private fun CaptureButtons(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    isEnabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
    ) {
        CaptureButton(
            onClick = onCameraClick,
            enabled = isEnabled,
            icon = Icons.Filled.CameraAlt,
            title = stringResource(Res.string.camera),
            subtitle = stringResource(Res.string.take_new_photo),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )
        CaptureButton(
            onClick = onGalleryClick,
            enabled = isEnabled,
            icon = Icons.Filled.Photo,
            title = stringResource(Res.string.gallery),
            subtitle = stringResource(Res.string.choose_from_library),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CaptureButton(
    onClick: () -> Unit,
    enabled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(Theme.shapes.large),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(Theme.dimensions.touchTarget))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

// endregion

// region Image Preview

@Composable
private fun ImagePreviewCard(
    imageBytes: ByteArray,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    isEnabled: Boolean,
) {
    val imageBitmap = remember(imageBytes) { imageBytes.toImageBitmap() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.extraLarge))
            .padding(Theme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg)
    ) {
        PreviewHeader()

        PreviewImage(imageBitmap = imageBitmap, imageBytes = imageBytes)

        AnimatedVisibility(
            visible = imageBitmap != null,
            enter = fadeIn(tween(400, 300)) + expandVertically(tween(400, 300)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
        ) {
            PreviewActions(
                onCancel = onCancel,
                onConfirm = onConfirm,
                isEnabled = isEnabled && imageBitmap != null,
            )
        }
    }
}

@Composable
private fun PreviewHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
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

@Composable
private fun PreviewImage(
    imageBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    imageBytes: ByteArray,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        shape = RoundedCornerShape(Theme.shapes.extraLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = Theme.elevation.extraHigh),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = stringResource(Res.string.preview_selected_image),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Theme.spacing.sm)
                        .clip(RoundedCornerShape(Theme.shapes.medium)),
                    contentScale = ContentScale.Fit
                )
            } else {
                ImageLoadError()
            }
        }
    }
}

@Composable
private fun ImageLoadError() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        modifier = Modifier.padding(Theme.spacing.md)
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

@Composable
private fun PreviewActions(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    isEnabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)
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
            enabled = isEnabled
        ) {
            Text(stringResource(Res.string.confirm_and_extract))
        }
    }
}

// endregion

private fun <T> MutableList<T>.addIfAbsent(element: T) {
    if (!contains(element)) add(element)
}
