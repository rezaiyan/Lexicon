package presentation.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import components.sheet.SheetBadge
import components.sheet.SheetDestructiveIconButton
import components.sheet.SheetFooterRow
import components.sheet.SheetGroup
import components.sheet.SheetInfoRow
import components.sheet.SheetOptionRow
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import components.sheet.SheetSectionLabel
import domain.common.util.EpochDateFormatter
import domain.tag.model.Tag
import domain.word.model.LearningStage
import domain.word.model.Word
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.assign_tags
import lexicon.resources.generated.resources.delete
import lexicon.resources.generated.resources.detail_added
import lexicon.resources.generated.resources.detail_languages
import lexicon.resources.generated.resources.detail_next_review
import lexicon.resources.generated.resources.detail_reviews
import lexicon.resources.generated.resources.edit
import lexicon.resources.generated.resources.filter_tag
import lexicon.resources.generated.resources.learning_progress
import org.jetbrains.compose.resources.stringResource
import theme.Theme

private const val MaxLevel = 6f

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

    SheetPage(
        title = word.originalWord,
        subtitle = word.translation,
        headerAccessory = {
            SheetBadge(
                text = "${stageName(stage)} · Lv.${word.level}",
                containerColor = color.copy(alpha = 0.15f),
                contentColor = color,
            )
        },
        footer = {
            SheetFooterRow(
                secondary = {
                    SheetDestructiveIconButton(
                        icon = Icons.Default.DeleteOutline,
                        contentDescription = stringResource(Res.string.delete),
                        onClick = { onDelete(word) },
                        modifier = it,
                    )
                },
                primary = {
                    SheetPrimaryButton(
                        text = stringResource(Res.string.edit),
                        onClick = { onEdit(word) },
                        modifier = it,
                    )
                },
            )
        },
    ) {
        if (word.description.isNotBlank()) {
            Text(
                text = word.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
            SheetSectionLabel(stringResource(Res.string.learning_progress))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
            ) {
                LinearProgressIndicator(
                    progress = { word.level / MaxLevel },
                    modifier = Modifier
                        .weight(1f)
                        .height(Theme.spacing.xs)
                        .clip(RoundedCornerShape(Theme.shapes.extraSmall)),
                    color = color,
                    trackColor = color.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round,
                )
                Text(
                    text = "${word.level}/${MaxLevel.toInt()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
            }
        }

        SheetGroup {
            SheetInfoRow(
                icon = Icons.Default.Language,
                label = stringResource(Res.string.detail_languages),
                value = "${word.targetLanguage.displayName} → ${word.sourceLanguage.displayName}",
                showDivider = true,
            )
            SheetInfoRow(
                icon = Icons.Default.CalendarToday,
                label = stringResource(Res.string.detail_added),
                value = EpochDateFormatter.toShortDate(word.dateAdded),
                showDivider = true,
            )
            if (word.nextReviewDate > 0L) {
                SheetInfoRow(
                    icon = Icons.Default.School,
                    label = stringResource(Res.string.detail_next_review),
                    value = EpochDateFormatter.toShortDate(word.nextReviewDate),
                    showDivider = true,
                )
            }
            SheetInfoRow(
                icon = Icons.Default.Refresh,
                label = stringResource(Res.string.detail_reviews),
                value = "${word.repetitions}",
                showDivider = false,
            )
        }

        SheetGroup {
            SheetOptionRow(
                icon = Icons.AutoMirrored.Filled.Label,
                title = stringResource(if (assignedTags.isEmpty()) Res.string.assign_tags else Res.string.filter_tag),
                subtitle = assignedTags.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name },
                onClick = { onAssignTags(word) },
                showDivider = false,
            )
        }
    }
}
