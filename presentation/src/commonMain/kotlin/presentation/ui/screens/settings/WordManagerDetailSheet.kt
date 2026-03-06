package presentation.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import domain.word.model.LearningStage
import domain.word.model.Word
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import overlay.OverlayHost
import overlay.OverlayNavigator
import overlay.bottomsheet.showSizeToFitBottomSheet
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.delete
import lexicon.resources.generated.resources.detail_added
import lexicon.resources.generated.resources.detail_languages
import lexicon.resources.generated.resources.detail_next_review
import lexicon.resources.generated.resources.detail_reviews
import lexicon.resources.generated.resources.edit
import lexicon.resources.generated.resources.learning_progress

internal fun OverlayHost.showWordDetailSheet(
    word: Word,
    onEdit: (Word) -> Unit,
    onDelete: (Word) -> Unit
) {
    showSizeToFitBottomSheet(tag = "word-detail") { navigator ->
        WordDetailSheetContent(
            word = word,
            navigator = navigator,
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}

@Composable
private fun WordDetailSheetContent(
    word: Word,
    navigator: OverlayNavigator,
    onEdit: (Word) -> Unit,
    onDelete: (Word) -> Unit
) {
    val stage = LearningStage.fromLevel(word.level)
    val color = levelColor(stage)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.small)
    ) {
        // Header: word + edit button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.originalWord,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(Theme.spacing.extraSmall3))
                Text(
                    text = word.translation,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = {
                navigator.dismiss()
                onEdit(word)
            }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(Res.string.edit),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Description
        if (word.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(Theme.spacing.extraSmall))
            Text(
                text = word.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(Theme.spacing.small))
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
            value = formatDetailDate(word.dateAdded)
        )
        Spacer(modifier = Modifier.height(Theme.spacing.extraSmall2))

        if (word.nextReviewDate > 0L) {
            DetailRow(
                icon = Icons.Default.School,
                label = stringResource(Res.string.detail_next_review),
                value = formatDetailDate(word.nextReviewDate)
            )
            Spacer(modifier = Modifier.height(Theme.spacing.extraSmall2))
        }

        DetailRow(
            icon = Icons.Default.Refresh,
            label = stringResource(Res.string.detail_reviews),
            value = "${word.repetitions}"
        )

        Spacer(modifier = Modifier.height(Theme.spacing.small))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(Theme.spacing.extraSmall2))

        // Delete button
        TextButton(
            onClick = {
                navigator.dismiss()
                onDelete(word)
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(Theme.spacing.extraSmall3))
            Text(
                text = stringResource(Res.string.delete),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(Theme.spacing.extraSmall2))
    }
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

private fun formatDetailDate(timestamp: Long): String {
    if (timestamp <= 0L) return "—"
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val timeZone = TimeZone.currentSystemDefault()
    val localDateTime = instant.toLocalDateTime(timeZone)
    val month = localDateTime.month.name.take(3).lowercase()
        .replaceFirstChar { it.uppercase() }
    return "${localDateTime.dayOfMonth} $month ${localDateTime.year}"
}
