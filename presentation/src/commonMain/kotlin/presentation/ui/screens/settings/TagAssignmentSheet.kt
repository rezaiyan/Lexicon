package presentation.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import components.sheet.SheetCheckboxRow
import components.sheet.SheetGroup
import components.sheet.SheetOptionRow
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import domain.tag.model.Tag
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.assign_tags
import lexicon.resources.generated.resources.new_tag
import lexicon.resources.generated.resources.no_tags
import lexicon.resources.generated.resources.word_count_label
import org.jetbrains.compose.resources.stringResource

/**
 * Multi-select tag list shared by single-word and batch assignment.
 * Pass [onCreateTag] to offer a "New Tag" row, so the list is never a dead end when no tag fits (or none exists).
 */
@Composable
internal fun TagChecklistContent(
    title: String,
    subtitle: String?,
    tags: List<Tag>,
    selectedTagIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onApply: () -> Unit,
    isLoading: Boolean = false,
    isSaving: Boolean = false,
    onClose: (() -> Unit)? = null,
    onCreateTag: (() -> Unit)? = null,
) {
    SheetPage(
        title = title,
        subtitle = subtitle,
        onClose = onClose,
        footer = {
            SheetPrimaryButton(
                text = stringResource(Res.string.assign_tags),
                onClick = onApply,
                enabled = !isLoading && tags.isNotEmpty(),
                isLoading = isSaving,
            )
        },
    ) {
        when {
            isLoading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            tags.isEmpty() -> Text(
                text = stringResource(Res.string.no_tags),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> SheetGroup {
                tags.forEachIndexed { index, tag ->
                    SheetCheckboxRow(
                        title = tag.name,
                        subtitle = stringResource(Res.string.word_count_label, tag.wordCount.toInt()),
                        icon = Icons.AutoMirrored.Filled.Label,
                        checked = tag.id in selectedTagIds,
                        onCheckedChange = { onToggle(tag.id) },
                        showDivider = index < tags.lastIndex,
                    )
                }
            }
        }

        if (onCreateTag != null && !isLoading) {
            SheetGroup {
                SheetOptionRow(
                    icon = Icons.Default.Add,
                    title = stringResource(Res.string.new_tag),
                    onClick = onCreateTag,
                    showDivider = false,
                )
            }
        }
    }
}
