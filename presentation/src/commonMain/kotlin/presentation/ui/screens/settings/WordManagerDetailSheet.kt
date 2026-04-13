package presentation.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import components.dialog.ButtonState
import components.dialog.ButtonType
import components.dialog.LexiconDialogContent
import domain.tag.model.Tag
import domain.word.model.LearningStage
import domain.word.model.Word
import domain.common.util.EpochDateFormatter
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.assign_tags
import lexicon.resources.generated.resources.delete
import lexicon.resources.generated.resources.filter_tag
import lexicon.resources.generated.resources.detail_added
import lexicon.resources.generated.resources.detail_languages
import lexicon.resources.generated.resources.detail_next_review
import lexicon.resources.generated.resources.detail_reviews
import lexicon.resources.generated.resources.edit
import lexicon.resources.generated.resources.learning_progress

@Composable
internal fun WordDetailSheetContent(
    word: Word,
    tags: List<Tag>,
    onEdit: (Word) -> Unit,
    onDelete: (Word) -> Unit,
    onAssignTags: (Word) -> Unit,
) {
    val stage = LearningStage.fromLevel(word.level)
    val color = levelColor(stage)
    val assignedTags = tags.filter { it.id in word.tagIds }

    LexiconDialogContent(
        title = word.originalWord,
        message = word.translation,
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Description
                if (word.description.isNotBlank()) {
                    Text(
                        text = word.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(Theme.spacing.small))
                }

                HorizontalDivider()
                Spacer(modifier = Modifier.height(Theme.spacing.small))

                // Learning Progress
                Text(
                    text = stringResource(Res.string.learning_progress),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Theme.spacing.extraSmall2))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall2)
                ) {
                    LinearProgressIndicator(
                        progress = { word.level / 6f },
                        modifier = Modifier
                            .weight(1f)
                            .height(Theme.spacing.xs)
                            .clip(RoundedCornerShape(Theme.shapes.extraSmall)),
                        color = color,
                        trackColor = color.copy(alpha = 0.15f),
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "Lv.${word.level}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                Spacer(modifier = Modifier.height(Theme.spacing.extraSmall3))

                // Stage name with colored dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall3)
                ) {
                    Box(
                        modifier = Modifier
                            .size(Theme.spacing.xs)
                            .clip(RoundedCornerShape(Theme.shapes.extraSmall))
                            .background(color)
                    )
                    Text(
                        text = stageName(stage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(Theme.spacing.small))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(Theme.spacing.small))

                // Details grid
                DetailRow(
                    icon = Icons.Default.Language,
                    label = stringResource(Res.string.detail_languages),
                    value = "${word.targetLanguage.displayName} \u2192 ${word.sourceLanguage.displayName}"
                )
                Spacer(modifier = Modifier.height(Theme.spacing.extraSmall2))

                DetailRow(
                    icon = Icons.Default.CalendarToday,
                    label = stringResource(Res.string.detail_added),
                    value = EpochDateFormatter.toShortDate(word.dateAdded)
                )
                Spacer(modifier = Modifier.height(Theme.spacing.extraSmall2))

                if (word.nextReviewDate > 0L) {
                    DetailRow(
                        icon = Icons.Default.School,
                        label = stringResource(Res.string.detail_next_review),
                        value = EpochDateFormatter.toShortDate(word.nextReviewDate)
                    )
                    Spacer(modifier = Modifier.height(Theme.spacing.extraSmall2))
                }

                DetailRow(
                    icon = Icons.Default.Refresh,
                    label = stringResource(Res.string.detail_reviews),
                    value = "${word.repetitions}"
                )

                if (assignedTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Theme.spacing.extraSmall2))
                    DetailRow(
                        icon = Icons.Default.Label,
                        label = stringResource(Res.string.filter_tag),
                        value = assignedTags.joinToString(", ") { it.name }
                    )
                }
            }
        },
        negativeButton = ButtonState(
            text = stringResource(Res.string.delete),
            onClick = { onDelete(word) },
            type = ButtonType.Error
        ),
        secondaryButton = ButtonState(
            text = stringResource(Res.string.assign_tags),
            onClick = { onAssignTags(word) }
        ),
        primaryButton = ButtonState(
            text = stringResource(Res.string.edit),
            onClick = { onEdit(word) }
        )
    )
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

