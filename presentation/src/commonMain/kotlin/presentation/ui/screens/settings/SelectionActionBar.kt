package presentation.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.batch_edit_languages
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.delete
import lexicon.resources.generated.resources.select_all
import lexicon.resources.generated.resources.selected_format
import lexicon.resources.generated.resources.share
import org.jetbrains.compose.resources.stringResource
import theme.Theme

@Composable
internal fun SelectionActionBar(
    isVisible: Boolean,
    selectedCount: Int,
    isUserSubscribed: Boolean,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onBatchEditLanguages: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(tween(250)) { it } + fadeIn(tween(250)),
        exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(250)),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = Theme.elevation.overlay,
            shape = RoundedCornerShape(topStart = Theme.shapes.large, topEnd = Theme.shapes.large)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header: close + count
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Theme.spacing.xs, vertical = Theme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(Res.string.cancel),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val countPattern = "%1" + '$' + "d"
                    Text(
                        text = stringResource(Res.string.selected_format)
                            .replace(countPattern, selectedCount.toString()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Actions row with labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Theme.spacing.xs, vertical = Theme.spacing.sm)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SelectionAction(
                        icon = Icons.Default.SelectAll,
                        label = stringResource(Res.string.select_all),
                        color = MaterialTheme.colorScheme.onSurface,
                        onClick = onSelectAll,
                        modifier = Modifier.weight(1f)
                    )
                    SelectionAction(
                        icon = Icons.Default.Language,
                        label = stringResource(Res.string.batch_edit_languages),
                        color = MaterialTheme.colorScheme.onSurface,
                        onClick = onBatchEditLanguages,
                        modifier = Modifier.weight(1f)
                    )
                    SelectionAction(
                        icon = Icons.Default.Delete,
                        label = stringResource(Res.string.delete),
                        color = MaterialTheme.colorScheme.error,
                        onClick = onDelete,
                        modifier = Modifier.weight(1f)
                    )
                    if (isUserSubscribed) {
                        SelectionAction(
                            icon = Icons.Default.FileUpload,
                            label = stringResource(Res.string.share),
                            color = MaterialTheme.colorScheme.onSurface,
                            onClick = onShare,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionAction(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Theme.shapes.medium))
            .combinedClickable(onClick = onClick)
            .padding(vertical = Theme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
