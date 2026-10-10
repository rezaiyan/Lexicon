package presentation.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import components.Pill
import domain.word.model.LearningStage
import domain.word.model.Word
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.select
import org.jetbrains.compose.resources.stringResource
import theme.Theme

private val CheckSize = 22.dp

/**
 * One row of the grouped word list. Rows stack edge to edge; [isFirst] / [isLast]
 * round the outer corners so consecutive rows read as a single card.
 *
 * Touch long press belongs to the list's drag-select gesture, so the row itself only
 * handles taps. [onLongPress] is exposed as the accessibility long-click action.
 */
@Composable
internal fun WordCard(
    word: Word,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onTap: () -> Unit,
    isFirst: Boolean,
    isLast: Boolean,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stage = LearningStage.fromLevel(word.level)
    val color = levelColor(stage)
    val selectLabel = stringResource(Res.string.select)

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.focus)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(150),
    )

    val corner = Theme.shapes.large
    val shape = RoundedCornerShape(
        topStart = if (isFirst) corner else 0.dp,
        topEnd = if (isFirst) corner else 0.dp,
        bottomStart = if (isLast) corner else 0.dp,
        bottomEnd = if (isLast) corner else 0.dp,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .clickable(onClick = onTap)
            .semantics {
                if (isSelectionMode) selected = isSelected
                onLongClick(label = selectLabel) {
                    onLongPress()
                    true
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Theme.dimensions.inputFieldHeight + Theme.spacing.xs)
                .padding(horizontal = Theme.spacing.cardPadding, vertical = Theme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
            ) {
                SelectionCheck(
                    checked = isSelected,
                    modifier = Modifier.padding(end = Theme.spacing.inlineGap),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.textGap)
            ) {
                Text(
                    text = word.originalWord,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = word.translation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LevelPill(
                stage = stage,
                color = color,
                modifier = Modifier.padding(start = Theme.spacing.inlineGap),
            )

            AnimatedVisibility(
                visible = !isSelectionMode,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = Theme.spacing.inlineGap)
                        .size(Theme.dimensions.iconSizeMedium)
                )
            }
        }
        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(start = Theme.spacing.cardPadding),
                thickness = Theme.dimensions.hairlineThickness,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

/** Round check: hollow ring when off, filled primary disc with a tick that pops in when on. */
@Composable
private fun SelectionCheck(checked: Boolean, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val fill by animateColorAsState(if (checked) primary else Color.Transparent, tween(150))
    val ring by animateColorAsState(if (checked) primary else MaterialTheme.colorScheme.outline, tween(150))
    val tickScale by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    )

    Box(
        modifier = modifier
            .size(CheckSize)
            .clip(CircleShape)
            .background(fill)
            .border(Theme.dimensions.borderWidthThick, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .size(Theme.dimensions.iconSizeSmall)
                .scale(tickScale),
        )
    }
}

@Composable
private fun LevelPill(
    stage: LearningStage,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Pill(
            text = stageName(stage),
            color = color,
            backgroundAlpha = 0.15f,
            fontWeight = FontWeight.Medium
        )
    }
}
