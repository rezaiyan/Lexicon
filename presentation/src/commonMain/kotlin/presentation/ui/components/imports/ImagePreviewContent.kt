package presentation.ui.components.imports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import components.animation.AiScanOverlay
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.image_too_large_warning
import lexicon.resources.generated.resources.confirm_and_extract
import lexicon.resources.generated.resources.failed_to_load_image
import lexicon.resources.generated.resources.preview_selected_image
import lexicon.resources.generated.resources.try_another_image
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.LexiconFormatters
import utils.toImageBitmap

@Composable
internal fun ImagePreviewCard(
    imageBytes: ByteArray,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    isEnabled: Boolean,
    imageQuality: Float,
    onQualityChange: (Float) -> Unit,
) {
    val imageBitmap = remember(imageBytes) { imageBytes.toImageBitmap() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Theme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
    ) {
        if (imageBitmap != null) {
            val aspectRatio = remember(imageBitmap) {
                (imageBitmap.width.toFloat() / imageBitmap.height.toFloat())
                    .coerceIn(0.5f, 2.5f)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Theme.shapes.medium)),
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = stringResource(Res.string.preview_selected_image),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio),
                    contentScale = ContentScale.Fit,
                )

                Column {
                    AnimatedVisibility(
                        visible = isLoading,
                        enter = fadeIn(tween(400)),
                        exit = fadeOut(tween(300)),
                    ) {
                        AiScanOverlay(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(aspectRatio),
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(Theme.shapes.medium),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    ImageLoadError()
                }
            }
        }

        val isTooBig = imageBytes.size > 5 * 1024 * 1024
        var sliderValue by remember(imageQuality) { mutableStateOf(imageQuality) }

        AnimatedVisibility(
            visible = imageBitmap != null && !isLoading,
            enter = fadeIn(tween(300, 150)) + expandVertically(tween(300, 150)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Theme.spacing.xxs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Quality",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        LexiconFormatters.fileSizeApprox(imageBytes.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isTooBig) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onQualityChange(sliderValue) },
                    valueRange = 0.2f..1.0f,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEnabled,
                )
                if (isTooBig) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Theme.spacing.xxs),
                        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(Theme.dimensions.iconSize / 2),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            stringResource(Res.string.image_too_large_warning),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Theme.dimensions.buttonHeight),
                    enabled = isEnabled && imageBitmap != null && !isTooBig,
                    shape = RoundedCornerShape(Theme.shapes.medium),
                ) {
                    Text(stringResource(Res.string.confirm_and_extract))
                }
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isEnabled,
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun ImageLoadError() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        modifier = Modifier.padding(Theme.spacing.md),
    ) {
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            modifier = Modifier.size(Theme.dimensions.iconSize),
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            stringResource(Res.string.failed_to_load_image),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(Res.string.try_another_image),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
