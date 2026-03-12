package presentation.ui.components.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.camera
import lexicon.resources.generated.resources.capture_vocab_from_image
import lexicon.resources.generated.resources.gallery
import lexicon.resources.generated.resources.ai_powered_extraction
import org.jetbrains.compose.resources.stringResource
import theme.Theme

@Composable
internal fun ImageSourceHeader(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(Theme.dimensions.iconSize),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(Res.string.ai_powered_extraction),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            stringResource(Res.string.capture_vocab_from_image),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun ImageSourcePicker(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val cornerRadiusDp = Theme.shapes.large

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(8.dp.toPx(), 5.dp.toPx()),
                        ),
                    ),
                    cornerRadius = CornerRadius(cornerRadiusDp.toPx()),
                )
            }
            .padding(Theme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
    ) {
        Surface(
            modifier = Modifier.size(Theme.dimensions.iconSizeHuge),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(Theme.dimensions.iconSize),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        ) {
            Button(
                onClick = onCameraClick,
                enabled = isEnabled,
                modifier = Modifier
                    .weight(1f)
                    .height(Theme.dimensions.buttonHeightSmall),
                shape = RoundedCornerShape(Theme.shapes.small),
            ) {
                Text(stringResource(Res.string.camera))
            }
            OutlinedButton(
                onClick = onGalleryClick,
                enabled = isEnabled,
                modifier = Modifier
                    .weight(1f)
                    .height(Theme.dimensions.buttonHeightSmall),
                shape = RoundedCornerShape(Theme.shapes.small),
            ) {
                Text(stringResource(Res.string.gallery))
            }
        }
    }
}
