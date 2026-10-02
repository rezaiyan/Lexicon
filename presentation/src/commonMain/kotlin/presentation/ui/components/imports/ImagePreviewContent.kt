package presentation.ui.components.imports

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import components.animation.AiScanOverlay
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import components.sheet.SheetSectionLabel
import components.sheet.SheetTonalButton
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.extract_words
import lexicon.resources.generated.resources.failed_to_load_image
import lexicon.resources.generated.resources.image_too_large_warning
import lexicon.resources.generated.resources.photo_preview_title
import lexicon.resources.generated.resources.photo_quality
import lexicon.resources.generated.resources.preview_selected_image
import lexicon.resources.generated.resources.retake
import lexicon.resources.generated.resources.try_another_image
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.LexiconFormatters
import utils.toImageBitmap

private const val MaxImageBytes = 5 * 1024 * 1024
private val ErrorPlaceholderHeight = 180.dp

// Portrait shots are letterboxed at 3:4 so the quality slider stays above the fold
private const val MinAspectRatio = 0.75f
private const val MaxAspectRatio = 2.5f

/** "Looks good?" — the picked photo, quality control and extract / retake actions. */
@Composable
internal fun PhotoPreviewPage(
    imageBytes: ByteArray,
    isLoading: Boolean,
    isEnabled: Boolean,
    imageQuality: Float,
    onQualityChange: (Float) -> Unit,
    onConfirm: () -> Unit,
    onRetake: () -> Unit,
) {
    val imageBitmap = remember(imageBytes) { imageBytes.toImageBitmap() }
    val isTooBig = imageBytes.size > MaxImageBytes

    SheetPage(
        title = stringResource(Res.string.photo_preview_title),
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
                SheetTonalButton(
                    text = stringResource(Res.string.retake),
                    onClick = onRetake,
                    enabled = isEnabled,
                    modifier = Modifier.width(IntrinsicSize.Max),
                )
                SheetPrimaryButton(
                    text = stringResource(Res.string.extract_words),
                    onClick = onConfirm,
                    enabled = isEnabled && imageBitmap != null && !isTooBig,
                    isLoading = isLoading,
                    icon = Icons.Default.AutoAwesome,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        if (imageBitmap != null) {
            val aspectRatio = remember(imageBitmap) {
                (imageBitmap.width.toFloat() / imageBitmap.height.toFloat()).coerceIn(MinAspectRatio, MaxAspectRatio)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Theme.shapes.extraLarge - Theme.spacing.xxs)),
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = stringResource(Res.string.preview_selected_image),
                    modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio),
                    contentScale = ContentScale.Fit,
                )
                // Explicit call: inside Box the page's ColumnScope overload would be picked otherwise
                androidx.compose.animation.AnimatedVisibility(
                    visible = isLoading,
                    enter = fadeIn(tween(Theme.motion.durationLong)),
                    exit = fadeOut(tween(Theme.motion.durationMedium)),
                    modifier = Modifier.matchParentSize(),
                ) {
                    AiScanOverlay(modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio))
                }
            }
        } else {
            ImageLoadError()
        }

        if (imageBitmap != null && !isLoading) {
            QualityControl(
                imageBytes = imageBytes,
                isTooBig = isTooBig,
                isEnabled = isEnabled,
                imageQuality = imageQuality,
                onQualityChange = onQualityChange,
            )
        }

        if (isTooBig) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .clip(RoundedCornerShape(Theme.shapes.medium))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs + Theme.spacing.xxxs),
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                )
                Text(
                    stringResource(Res.string.image_too_large_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun QualityControl(
    imageBytes: ByteArray,
    isTooBig: Boolean,
    isEnabled: Boolean,
    imageQuality: Float,
    onQualityChange: (Float) -> Unit,
) {
    var sliderValue by remember(imageQuality) { mutableFloatStateOf(imageQuality) }
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SheetSectionLabel(stringResource(Res.string.photo_quality), Modifier.weight(1f))
            Text(
                LexiconFormatters.fileSizeApprox(imageBytes.size),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isTooBig) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onQualityChange(sliderValue) },
            valueRange = 0.2f..1.0f,
            enabled = isEnabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ImageLoadError() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(ErrorPlaceholderHeight)
            .clip(RoundedCornerShape(Theme.shapes.extraLarge - Theme.spacing.xxs))
            .background(MaterialTheme.colorScheme.surface)
            .padding(Theme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs, Alignment.CenterVertically),
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(Theme.dimensions.iconSize),
        )
        Text(
            stringResource(Res.string.failed_to_load_image),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(Res.string.try_another_image),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
