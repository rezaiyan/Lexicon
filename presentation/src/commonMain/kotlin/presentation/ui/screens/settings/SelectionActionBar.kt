package presentation.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.delete
import lexicon.resources.generated.resources.selection_action_language
import lexicon.resources.generated.resources.selection_action_tag
import lexicon.resources.generated.resources.share
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/**
 * Floating toolbar for selection mode. Count, close and select-all live in the top bar,
 * so this holds only the bulk actions. Actions dim and stop responding while nothing is selected.
 */
@Composable
internal fun SelectionActionBar(
    isVisible: Boolean,
    enabled: Boolean,
    onDelete: () -> Unit,
    onBatchEditLanguages: () -> Unit,
    onBatchAssignTags: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy)) { it / 2 } +
            scaleIn(initialScale = 0.92f) + fadeIn(tween(150)),
        exit = slideOutVertically(tween(180)) { it / 2 } + fadeOut(tween(150)),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.md),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = Theme.elevation.overlay,
            shape = RoundedCornerShape(Theme.shapes.extraLarge),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Theme.dimensions.bottomBarHeight)
                    .padding(horizontal = Theme.spacing.xs),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val onSurface = MaterialTheme.colorScheme.onSurface
                SelectionAction(
                    icon = Icons.Outlined.Translate,
                    label = stringResource(Res.string.selection_action_language),
                    color = onSurface,
                    enabled = enabled,
                    onClick = onBatchEditLanguages,
                    modifier = Modifier.weight(1f),
                )
                SelectionAction(
                    icon = Icons.AutoMirrored.Outlined.Label,
                    label = stringResource(Res.string.selection_action_tag),
                    color = onSurface,
                    enabled = enabled,
                    onClick = onBatchAssignTags,
                    modifier = Modifier.weight(1f),
                )
                SelectionAction(
                    icon = Icons.Outlined.IosShare,
                    label = stringResource(Res.string.share),
                    color = onSurface,
                    enabled = enabled,
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                )
                SelectionAction(
                    icon = Icons.Outlined.DeleteOutline,
                    label = stringResource(Res.string.delete),
                    color = MaterialTheme.colorScheme.error,
                    enabled = enabled,
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SelectionAction(
    icon: ImageVector,
    label: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Theme.shapes.large))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else Theme.opacity.disabled)
            .padding(vertical = Theme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(Theme.dimensions.iconSize),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
