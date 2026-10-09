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
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import components.animation.AiScanOverlay
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import components.sheet.SheetTonalButton
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.extract_words
import lexicon.resources.generated.resources.failed_to_load_image
import lexicon.resources.generated.resources.photo_preview_title
import lexicon.resources.generated.resources.preview_selected_image
import lexicon.resources.generated.resources.retake
import lexicon.resources.generated.resources.rotate_photo
import lexicon.resources.generated.resources.try_another_image
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.toImageBitmap

private val ErrorPlaceholderHeight = 180.dp

// Portrait shots are letterboxed at 3:4 so the actions stay above the fold
private const val MinAspectRatio = 0.75f
private const val MaxAspectRatio = 2.5f
private const val QUARTER_TURNS = 4
private const val RIGHT_ANGLE_DEGREES = 90f

/** "Looks good?" — the picked photo with extract / retake actions. */
@Composable
internal fun PhotoPreviewPage(
    imageBytes: ByteArray,
    isLoading: Boolean,
    isEnabled: Boolean,
    problem: String?,
    quarterTurns: Int,
    onRotate: () -> Unit,
    onConfirm: () -> Unit,
    onRetake: () -> Unit,
) {
    val decoded = remember(imageBytes) { imageBytes.toImageBitmap() }
    // Shown exactly as it will be uploaded: sideways text is misread, so the user turns it upright here.
    val imageBitmap = remember(decoded, quarterTurns) { decoded?.rotatedClockwise(quarterTurns) }

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
                    enabled = isEnabled && imageBitmap != null,
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
                if (!isLoading) {
                    FilledTonalIconButton(
                        onClick = onRotate,
                        enabled = isEnabled,
                        modifier = Modifier.align(Alignment.TopEnd).padding(Theme.spacing.sm),
                    ) {
                        Icon(
                            Icons.Default.Rotate90DegreesCw,
                            contentDescription = stringResource(Res.string.rotate_photo),
                        )
                    }
                }
            }
        } else {
            ImageLoadError()
        }

        ErrorMessage(problem)
    }
}

/** The bitmap turned [quarterTurns] × 90° clockwise (drawn into a new bitmap so layout gets the new aspect). */
private fun ImageBitmap.rotatedClockwise(quarterTurns: Int): ImageBitmap {
    val turns = quarterTurns.mod(QUARTER_TURNS)
    if (turns == 0) return this
    val sideways = turns % 2 == 1
    val result = ImageBitmap(if (sideways) height else width, if (sideways) width else height)
    Canvas(result).apply {
        translate(result.width / 2f, result.height / 2f)
        rotate(RIGHT_ANGLE_DEGREES * turns)
        translate(-width / 2f, -height / 2f)
        drawImage(this@rotatedClockwise, Offset.Zero, Paint())
    }
    return result
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
