package components.snackbar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.close
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/** Visual intent of a snackbar; drives its accent color and leading icon. */
enum class SnackbarTone { Neutral, Success, Error }

/** [SnackbarVisuals] carrying a [SnackbarTone]. Plain `showSnackbar` calls render as [SnackbarTone.Neutral]. */
@Immutable
data class LexiconSnackbarVisuals(
    override val message: String,
    val tone: SnackbarTone = SnackbarTone.Neutral,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration =
        if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Indefinite,
) : SnackbarVisuals

suspend fun SnackbarHostState.showSuccess(message: String): SnackbarResult =
    showSnackbar(LexiconSnackbarVisuals(message, SnackbarTone.Success))

suspend fun SnackbarHostState.showError(message: String): SnackbarResult =
    showSnackbar(LexiconSnackbarVisuals(message, SnackbarTone.Error, withDismissAction = true))

private val SnackbarVisuals.tone: SnackbarTone
    get() = (this as? LexiconSnackbarVisuals)?.tone ?: SnackbarTone.Neutral

/** App-wide snackbar host rendering every message as a [LexiconSnackbar]. */
@Composable
fun LexiconSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        LexiconSnackbar(data)
    }
}

/**
 * Floating card snackbar: a tinted icon badge for the [SnackbarTone],
 * the message, and an optional action and dismiss button.
 */
@Composable
fun LexiconSnackbar(
    data: SnackbarData,
    modifier: Modifier = Modifier,
) {
    val visuals = data.visuals
    val accent = visuals.tone.accentColor()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.screenGutter, vertical = Theme.spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = Theme.dimensions.contentMaxWidth)
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
            shape = RoundedCornerShape(Theme.shapes.large),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = Theme.elevation.overlay,
            border = BorderStroke(Theme.dimensions.hairlineThickness, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = Theme.dimensions.touchTarget)
                    .padding(
                        start = Theme.spacing.sm,
                        end = if (visuals.withDismissAction) Theme.spacing.xxs else Theme.spacing.md,
                        top = Theme.spacing.xs,
                        bottom = Theme.spacing.xs,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
            ) {
                ToneBadge(icon = visuals.tone.icon(), accent = accent)

                Text(
                    text = visuals.message,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                visuals.actionLabel?.let { label ->
                    TextButton(onClick = data::performAction) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                if (visuals.withDismissAction) {
                    IconButton(onClick = data::dismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(Res.string.close),
                            modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToneBadge(icon: ImageVector, accent: Color) {
    Box(
        modifier = Modifier
            .size(Theme.dimensions.iconSizeXLarge)
            .background(accent.copy(alpha = BADGE_ALPHA), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            // The message carries the meaning
            contentDescription = null,
            modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
            tint = accent,
        )
    }
}

@Composable
private fun SnackbarTone.accentColor(): Color = when (this) {
    SnackbarTone.Neutral -> MaterialTheme.colorScheme.primary
    SnackbarTone.Success -> Theme.colors.success
    SnackbarTone.Error -> MaterialTheme.colorScheme.error
}

private fun SnackbarTone.icon(): ImageVector = when (this) {
    SnackbarTone.Neutral -> Icons.Rounded.Info
    SnackbarTone.Success -> Icons.Rounded.CheckCircle
    SnackbarTone.Error -> Icons.Rounded.ErrorOutline
}

private const val BADGE_ALPHA = 0.14f
