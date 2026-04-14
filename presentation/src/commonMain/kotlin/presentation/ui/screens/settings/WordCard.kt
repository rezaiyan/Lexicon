package presentation.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import components.Pill
import domain.word.model.LearningStage
import domain.word.model.Word
import theme.Theme

@Composable
internal fun WordCard(
    word: Word,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val stage = LearningStage.fromLevel(word.level)
    val color = levelColor(stage)

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.secondaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = tween(200)
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.dimensions.cardCornerRadius))
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress,
            ),
        shape = RoundedCornerShape(Theme.dimensions.cardCornerRadius),
        color = backgroundColor,
        tonalElevation = if (isSelected) Theme.elevation.medium else Theme.elevation.none
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Level color strip
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(color)
            )

            // Content
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = Theme.spacing.extraSmall,
                        end = Theme.spacing.small,
                        top = Theme.spacing.extraSmall,
                        bottom = Theme.spacing.extraSmall
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Word info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall3)
                ) {
                    Text(
                        text = word.originalWord,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = word.translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (word.description.isNotBlank()) {
                        Text(
                            text = word.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Meta row: languages + level pill + date
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LanguagePill(
                            text = word.targetLanguage.code.uppercase(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "\u2192",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        LanguagePill(
                            text = word.sourceLanguage.code.uppercase(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )

                        Text(
                            text = "\u00b7",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )

                        // Level pill
                        LevelPill(stage = stage, color = color)

                        if (word.dateAdded > 0L) {
                            Text(
                                text = "\u00b7 ${formatDateAdded(word.dateAdded)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Checkbox (only in selection mode)
                AnimatedVisibility(
                    visible = isSelectionMode,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200))
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelPill(
    stage: LearningStage,
    color: Color,
) {
    Pill(
        text = stageName(stage),
        color = color,
        backgroundAlpha = 0.15f,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun LanguagePill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Pill(
        text = text,
        color = color,
        modifier = modifier,
        cornerRadius = Theme.shapes.extraSmall
    )
}
