package presentation.ui.components.imports

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import components.sheet.WordFormSheetPage
import domain.tag.model.Tag
import domain.word.add.parser.RejectedLine
import expects.BackHandler
import feature.addwords.model.Candidate
import feature.addwords.model.CandidateReview
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.add_n_words
import lexicon.resources.generated.resources.add_words_skipped_lines
import lexicon.resources.generated.resources.ai_wizard_all_selected
import lexicon.resources.generated.resources.ai_wizard_deselect_all
import lexicon.resources.generated.resources.ai_wizard_select_all
import lexicon.resources.generated.resources.ai_wizard_selection_count
import lexicon.resources.generated.resources.edit_word_cd
import lexicon.resources.generated.resources.image_review_edit_word
import lexicon.resources.generated.resources.review_found_subtitle
import lexicon.resources.generated.resources.review_found_title
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.Language

private const val MaxSkippedLinesShown = 20

/**
 * "We found N words" — the one review step shared by file, photo and AI sources: pick, fix and tag
 * the words before they are added. A word being edited replaces the list; system back cancels the edit.
 */
@Composable
internal fun CandidateReviewPage(
    review: CandidateReview,
    learning: Language,
    native: Language,
    tags: List<Tag>,
    selectedTagId: Long?,
    isCommitting: Boolean,
    problem: String?,
    onTagSelected: (Long?) -> Unit,
    onCreateTag: () -> Unit,
    onChangeLanguage: () -> Unit,
    onToggle: (Int) -> Unit,
    onSetAllSelected: (Boolean) -> Unit,
    onStartEdit: (Int) -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: (term: String, translation: String, note: String) -> Unit,
    onCommit: () -> Unit,
) {
    val editing = review.editing
    if (editing != null) {
        EditCandidatePage(editing, problem, onSave = onSaveEdit, onCancel = onCancelEdit)
        return
    }

    SheetPage(
        title = stringResource(Res.string.review_found_title, review.candidates.size),
        subtitle = stringResource(Res.string.review_found_subtitle),
        headerAccessory = { LanguagePairChip(source = learning, target = native, onClick = onChangeLanguage) },
        scrollable = false,
        footer = {
            SheetPrimaryButton(
                text = stringResource(Res.string.add_n_words, review.selectedCount),
                onClick = onCommit,
                enabled = review.selectedCount > 0,
                isLoading = isCommitting,
            )
        },
    ) {
        TagSelectorRow(
            tags = tags,
            selectedTagId = selectedTagId,
            onTagSelected = onTagSelected,
            onCreateTag = onCreateTag,
        )

        ErrorMessage(problem)

        Column(modifier = Modifier.weight(1f, fill = false)) {
            SelectionHeader(review, onSetAllSelected)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay))

            LazyColumn(contentPadding = PaddingValues(bottom = Theme.spacing.xs)) {
                items(review.candidates, key = { it.id }) { candidate ->
                    CandidateRow(
                        candidate = candidate,
                        showDivider = candidate.id != review.candidates.last().id,
                        onToggle = { onToggle(candidate.id) },
                        onEdit = { onStartEdit(candidate.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
                if (review.rejected.isNotEmpty()) {
                    item(key = "skipped") { SkippedLines(review.rejected) }
                }
            }
        }
    }
}

@Composable
private fun SelectionHeader(review: CandidateReview, onSetAllSelected: (Boolean) -> Unit) {
    val total = review.candidates.size
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (review.allSelected) {
                stringResource(Res.string.ai_wizard_all_selected, total)
            } else {
                stringResource(Res.string.ai_wizard_selection_count, review.selectedCount, total)
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onSetAllSelected(!review.allSelected) }) {
            Text(
                stringResource(
                    if (review.allSelected) Res.string.ai_wizard_deselect_all else Res.string.ai_wizard_select_all,
                ),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CandidateRow(
    candidate: Candidate,
    showDivider: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = candidate.selected
    val draft = candidate.draft
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
                .heightIn(min = Theme.dimensions.touchTarget + Theme.spacing.md)
                .padding(vertical = Theme.spacing.xs + Theme.spacing.xxxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm + Theme.spacing.xxxs),
        ) {
            CheckBox(selected)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs),
            ) {
                Text(
                    draft.term,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                SecondaryLine(draft.translation, MaterialTheme.typography.bodyMedium)
                if (draft.note.isNotBlank()) SecondaryLine(draft.note, MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(Res.string.edit_word_cd, draft.term),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay))
        }
    }
}

@Composable
private fun SecondaryLine(text: String, style: TextStyle) {
    Text(
        text,
        style = style,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun CheckBox(selected: Boolean) {
    val shape = RoundedCornerShape(Theme.shapes.small - Theme.spacing.xxxs / 2)
    val outline = MaterialTheme.colorScheme.outline
    val boxColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        label = "check",
    )
    Box(
        modifier = Modifier
            .size(Theme.dimensions.iconSize)
            .clip(shape)
            .background(boxColor)
            .then(if (selected) Modifier else Modifier.border(Theme.spacing.xxxs, outline, shape)),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
            )
        }
    }
}

/** "3 lines skipped" — tap to see which lines of the file could not be read. */
@Composable
private fun SkippedLines(rejected: List<RejectedLine>) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = Theme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
    ) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(stringResource(Res.string.add_words_skipped_lines, rejected.size), fontWeight = FontWeight.Bold)
        }
        if (expanded) {
            rejected.take(MaxSkippedLinesShown).forEach { line ->
                SecondaryLine("${line.lineNumber}: ${line.raw}", MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EditCandidatePage(
    candidate: Candidate,
    problem: String?,
    onSave: (term: String, translation: String, note: String) -> Unit,
    onCancel: () -> Unit,
) {
    var term by remember(candidate.id) { mutableStateOf(candidate.draft.term) }
    var translation by remember(candidate.id) { mutableStateOf(candidate.draft.translation) }
    var note by remember(candidate.id) { mutableStateOf(candidate.draft.note) }

    BackHandler(onBack = onCancel)

    Column {
        WordFormSheetPage(
            title = stringResource(Res.string.image_review_edit_word),
            word = term,
            onWordChange = { term = it },
            translation = translation,
            onTranslationChange = { translation = it },
            description = note,
            onDescriptionChange = { note = it },
            onSave = { onSave(term, translation, note) },
            onCancel = onCancel,
        )
        Box(Modifier.padding(horizontal = Theme.spacing.md)) { ErrorMessage(problem) }
    }
}
