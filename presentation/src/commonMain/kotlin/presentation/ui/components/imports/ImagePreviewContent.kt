package presentation.ui.components.imports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.confirm_and_extract
import lexicon.resources.generated.resources.failed_to_load_image
import lexicon.resources.generated.resources.preview_selected_image
import lexicon.resources.generated.resources.review_before_processing
import lexicon.resources.generated.resources.try_another_image
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.toImageBitmap

@Composable
internal fun ImagePreviewCard(
    imageBytes: ByteArray,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    isEnabled: Boolean,
) {
    val imageBitmap = remember(imageBytes) { imageBytes.toImageBitmap() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Theme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg)
    ) {
        PreviewHeader()

        PreviewImage(imageBitmap = imageBitmap)

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
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(Res.string.review_before_processing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PreviewImage(
    imageBitmap: androidx.compose.ui.graphics.ImageBitmap?,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        shape = RoundedCornerShape(Theme.shapes.large),
        elevation = CardDefaults.cardElevation(defaultElevation = Theme.elevation.medium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(
            Theme.dimensions.hairlineThickness,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
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
            modifier = Modifier
                .weight(1f)
                .height(Theme.dimensions.buttonHeight),
            enabled = isEnabled,
            shape = RoundedCornerShape(Theme.shapes.medium)
        ) {
            Text(stringResource(Res.string.cancel))
        }
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .weight(1f)
                .height(Theme.dimensions.buttonHeight),
            enabled = isEnabled,
            shape = RoundedCornerShape(Theme.shapes.medium)
        ) {
            Text(stringResource(Res.string.confirm_and_extract))
        }
    }
}
